package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncAction;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.entityEnum.SyncLogStatus;
import com.notice.system.service.SyncService;
import com.notice.system.support.event.SyncBatchPostCheckEvent;
import com.notice.system.support.event.SyncLogEvent;
import com.notice.system.sync.SyncExecutor;
import com.notice.system.sync.SyncMetadataRegistry;
import com.notice.system.sync.SyncStatusDecider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {

    private final SyncMetadataRegistry metadataRegistry;
    private final List<SyncExecutor> executors;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<DatabaseType, SyncExecutor> executorMap = new EnumMap<>(DatabaseType.class);

    private enum LogMode {
        NONE,        // 不写任何日志（retry 场景）
        FAIL_ONLY,   // 只写失败日志（submitSync 的“先 apply 后统一写 success”）
        ALL          // 成功失败都写（syncToTarget 场景）
    }

    @PostConstruct
    public void init() {
        for (SyncExecutor executor : executors) {
            DatabaseType targetDb = executor.targetDb();
            executorMap.put(targetDb, executor);
            log.info("[SYNC] executor registered: targetDb={}, impl={}", targetDb, executor.getClass().getSimpleName());
        }
    }

    @Override
    public void submitSync(SyncEntityType entityType,
                           String entityId,
                           SyncAction action,
                           DatabaseType sourceDb) {

        DatabaseType realSource = useDb(sourceDb);

        // targetDb -> applyOk（按遍历顺序保留输出稳定性）
        Map<DatabaseType, Boolean> applyOk = new LinkedHashMap<>();

        for (Map.Entry<DatabaseType, SyncExecutor> e : executorMap.entrySet()) {
            DatabaseType targetDb = e.getKey();
            if (targetDb == realSource) {
                continue;
            }
            boolean ok = syncOneTarget(entityType, entityId, action, realSource, targetDb, e.getValue(), LogMode.FAIL_ONLY);
            applyOk.put(targetDb, ok);
        }

        // 不需要做 post-check 的场景：直接对成功目标写 SUCCESS
        if (!allowWriteSyncLog(entityType) || action == SyncAction.DELETE) {
            applyOk.forEach((targetDb, ok) -> {
                if (ok) {
                    publishLog(entityType, entityId, action, realSource, targetDb, SyncLogStatus.SUCCESS, null, null);
                }
            });
            return;
        }

        // 全部失败：失败日志已在 syncOneTarget 中写过
        boolean anyOk = applyOk.values().stream().anyMatch(Boolean::booleanValue);
        if (!anyOk) {
            return;
        }

        // 一次提交结束后统一触发 post-check（生成冲突工单等）
        SyncBatchPostCheckEvent ev = new SyncBatchPostCheckEvent(entityType, entityId, action, realSource, applyOk);
        eventPublisher.publishEvent(ev);

        String conflictId = ev.getConflictId();

        // 对同步成功的目标库写 SUCCESS/CONFLICT
        applyOk.forEach((targetDb, ok) -> {
            if (!ok) return;

            if (conflictId != null) {
                publishLog(entityType, entityId, action, realSource, targetDb,
                        SyncLogStatus.CONFLICT, "post-check detected mismatch, conflict created", conflictId);
            } else {
                publishLog(entityType, entityId, action, realSource, targetDb,
                        SyncLogStatus.SUCCESS, null, null);
            }
        });
    }

    @Override
    public void submitBatchSync(SyncEntityType entityType,
                                Collection<String> entityIds,
                                SyncAction action,
                                DatabaseType sourceDb) {
        if (entityIds == null || entityIds.isEmpty()) {
            return;
        }
        for (String id : entityIds) {
            submitSync(entityType, id, action, sourceDb);
        }
    }

    /** 正常业务：同步到指定目标库并写日志。 */
    @Override
    public boolean syncToTarget(SyncEntityType entityType,
                                String entityId,
                                SyncAction action,
                                DatabaseType sourceDb,
                                DatabaseType targetDb) {
        return doSyncToTarget(entityType, entityId, action, sourceDb, targetDb, LogMode.ALL);
    }

    /** 重试/修复：只执行同步，不生成新日志。 */
    @Override
    public boolean syncToTargetWithoutLog(SyncEntityType entityType,
                                          String entityId,
                                          SyncAction action,
                                          DatabaseType sourceDb,
                                          DatabaseType targetDb) {
        return doSyncToTarget(entityType, entityId, action, sourceDb, targetDb, LogMode.NONE);
    }

    private boolean doSyncToTarget(SyncEntityType entityType,
                                   String entityId,
                                   SyncAction action,
                                   DatabaseType sourceDb,
                                   DatabaseType targetDb,
                                   LogMode logMode) {

        DatabaseType realSource = useDb(sourceDb);
        if (targetDb == null) {
            return false;
        }
        if (targetDb == realSource) {
            return true;
        }

        SyncExecutor executor = executorMap.get(targetDb);
        if (executor == null) {
            log.warn("[SYNC] executor not found: targetDb={}, entityType={}, id={}", targetDb, entityType, entityId);
            return false;
        }

        return syncOneTarget(entityType, entityId, action, realSource, targetDb, executor, logMode);
    }

    @Override
    public void fullSyncEntityFromSource(SyncEntityType entityType, DatabaseType sourceDb) {
        DatabaseType realSource = useDb(sourceDb);

        SyncMetadataRegistry.EntitySyncDefinition<?> def = metadataRegistry.getDefinition(entityType);
        BaseMapper<?> sourceMapper = def.getMapper(realSource);
        if (sourceMapper == null) {
            throw new IllegalStateException("Source mapper not configured: entityType=" + entityType + ", db=" + realSource);
        }

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) sourceMapper.selectList(null);
        if (list == null || list.isEmpty()) {
            log.info("[SYNC] fullSyncEntityFromSource empty: entityType={}, sourceDb={}", entityType, realSource);
            return;
        }

        @SuppressWarnings("unchecked")
        Function<Object, String> idGetter = (Function<Object, String>) def.getIdGetter();

        log.info("[SYNC] fullSyncEntityFromSource start: entityType={}, sourceDb={}, total={}", entityType, realSource, list.size());
        for (Object entity : list) {
            String id = idGetter.apply(entity);
            if (id != null && !id.isBlank()) {
                submitSync(entityType, id, SyncAction.UPDATE, realSource);
            }
        }
        log.info("[SYNC] fullSyncEntityFromSource done: entityType={}, sourceDb={}", entityType, realSource);
    }

    @Override
    public void fullSyncAllFromSource(DatabaseType sourceDb) {
        DatabaseType realSource = useDb(sourceDb);
        log.info("[SYNC] fullSyncAllFromSource start: sourceDb={}", realSource);

        for (SyncEntityType type : SyncEntityType.values()) {
            if (metadataRegistry.supports(type)) {
                fullSyncEntityFromSource(type, realSource);
            }
        }

        log.info("[SYNC] fullSyncAllFromSource done: sourceDb={}", realSource);
    }

    /** 执行单目标库同步，并按日志模式决定是否写 SyncLog。 */
    private boolean syncOneTarget(SyncEntityType entityType,
                                  String entityId,
                                  SyncAction action,
                                  DatabaseType sourceDb,
                                  DatabaseType targetDb,
                                  SyncExecutor executor,
                                  LogMode logMode) {

        try {
            executor.applyOne(entityType, action, entityId, sourceDb);

            if (logMode == LogMode.ALL && allowWriteSyncLog(entityType)) {
                publishLog(entityType, entityId, action, sourceDb, targetDb, SyncLogStatus.SUCCESS, null, null);
            }
            return true;

        } catch (Exception ex) {
            log.warn("[SYNC] failed: entityType={}, id={}, action={}, {} -> {}, err={}",
                    entityType, entityId, action, sourceDb, targetDb, ex.getMessage(), ex);

            if (logMode != LogMode.NONE && allowWriteSyncLog(entityType)) {
                SyncLogStatus st = SyncStatusDecider.decideOnException(ex);
                publishLog(entityType, entityId, action, sourceDb, targetDb, st, ex.getMessage(), null);
            }
            return false;
        }
    }

    /** 过滤掉系统自用表，避免写日志时递归触发同步。 */
    private boolean allowWriteSyncLog(SyncEntityType type) {
        return type != SyncEntityType.SYNC_LOG
                && type != SyncEntityType.SYNC_CONFLICT
                && type != SyncEntityType.SYNC_CONFLICT_ITEM;
    }

    private void publishLog(SyncEntityType entityType,
                            String entityId,
                            SyncAction action,
                            DatabaseType sourceDb,
                            DatabaseType targetDb,
                            SyncLogStatus status,
                            String message,
                            String conflictId) {
        eventPublisher.publishEvent(new SyncLogEvent(
                entityType, entityId, action, sourceDb, targetDb, status, message, conflictId
        ));
    }

    private DatabaseType useDb(DatabaseType db) {
        return (db == null ? DatabaseType.MYSQL : db);
    }
}









