package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.support.event.SyncLogEvent;
import com.notice.system.service.SyncService;
import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncAction;
import com.notice.system.sync.SyncEntityType;
import com.notice.system.sync.SyncExecutor;
import com.notice.system.sync.SyncMetadataRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncServiceImpl implements SyncService {

    private final SyncMetadataRegistry metadataRegistry;
    private final List<SyncExecutor> executors;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<DatabaseType, SyncExecutor> executorMap = new EnumMap<>(DatabaseType.class);

    @PostConstruct
    public void init() {
        for (SyncExecutor executor : executors) {
            DatabaseType targetDb = executor.targetDb();
            executorMap.put(targetDb, executor);
            log.info("[SYNC] 注册同步执行器：targetDb={}，executor={}",
                    targetDb, executor.getClass().getSimpleName());
        }
    }

    @Override
    public void submitSync(SyncEntityType entityType,
                           String entityId,
                           SyncAction action,
                           DatabaseType sourceDb) {

        DatabaseType realSource = (sourceDb == null ? DatabaseType.MYSQL : sourceDb);

        executorMap.forEach((targetDb, executor) -> {
            if (targetDb == realSource) {
                return;
            }
            // 正常业务：需要记日志
            doSyncOneTarget(entityType, entityId, action, realSource, targetDb, executor, true);
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

    /**
     * 正常业务使用：同步到指定目标库，并记录同步日志
     */
    @Override
    public boolean syncToTarget(SyncEntityType entityType,
                                String entityId,
                                SyncAction action,
                                DatabaseType sourceDb,
                                DatabaseType targetDb) {
        return doSyncToTarget(entityType, entityId, action, sourceDb, targetDb, true);
    }

    /**
     * 重试使用：只做同步动作，不再生成新的 SyncLog 记录
     */
    @Override
    public boolean syncToTargetWithoutLog(SyncEntityType entityType,
                                          String entityId,
                                          SyncAction action,
                                          DatabaseType sourceDb,
                                          DatabaseType targetDb) {
        return doSyncToTarget(entityType, entityId, action, sourceDb, targetDb, false);
    }

    /**
     * 统一封装 syncToTarget 的公共逻辑，通过 writeLog 开关控制是否记日志
     */
    private boolean doSyncToTarget(SyncEntityType entityType,
                                   String entityId,
                                   SyncAction action,
                                   DatabaseType sourceDb,
                                   DatabaseType targetDb,
                                   boolean writeLog) {

        DatabaseType realSource = (sourceDb == null ? DatabaseType.MYSQL : sourceDb);

        if (targetDb == null) {
            log.warn("[SYNC] syncToTarget 时 targetDb 为 null，跳过，entityType={}, id={}",
                    entityType, entityId);
            return false;
        }

        if (targetDb == realSource) {
            log.info("[SYNC] syncToTarget 源库与目标库相同（{}），无需同步，entityType={}, id={}",
                    targetDb, entityType, entityId);
            return true;
        }

        SyncExecutor executor = executorMap.get(targetDb);
        if (executor == null) {
            log.warn("[SYNC] 未找到目标库 {} 的执行器，entityType={}, id={}",
                    targetDb, entityType, entityId);
            return false;
        }

        return doSyncOneTarget(entityType, entityId, action, realSource, targetDb, executor, writeLog);
    }

    @Override
    public void fullSyncEntityFromSource(SyncEntityType entityType,
                                         DatabaseType sourceDb) {

        DatabaseType realSource = (sourceDb == null ? DatabaseType.MYSQL : sourceDb);

        SyncMetadataRegistry.EntitySyncDefinition<?> def =
                metadataRegistry.getDefinition(entityType);
        if (def == null) {
            log.warn("[SYNC] fullSyncEntityFromSource 未找到实体 {} 的同步定义，sourceDb={}",
                    entityType, realSource);
            return;
        }

        BaseMapper<?> sourceMapper = def.getMapper(realSource);
        if (sourceMapper == null) {
            log.warn("[SYNC] fullSyncEntityFromSource 源库 {} 未配置实体 {} 的 Mapper",
                    realSource, entityType);
            return;
        }

        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) sourceMapper.selectList(null);
        if (list == null || list.isEmpty()) {
            log.info("[SYNC] fullSyncEntityFromSource 源库 {} 下实体 {} 无数据可同步",
                    realSource, entityType);
            return;
        }

        @SuppressWarnings("unchecked")
        Function<Object, String> idGetter =
                (Function<Object, String>) def.getIdGetter();

        log.info("[SYNC] fullSyncEntityFromSource 开始全量同步，entityType={}, sourceDb={}, total={}",
                entityType, realSource, list.size());

        for (Object entity : list) {
            String id = idGetter.apply(entity);
            if (id == null) {
                continue;
            }
            // 全量同步是正常业务，同样需要记日志
            submitSync(entityType, id, SyncAction.UPDATE, realSource);
        }

        log.info("[SYNC] fullSyncEntityFromSource 结束，entityType={}, sourceDb={}",
                entityType, realSource);
    }

    @Override
    public void fullSyncAllFromSource(DatabaseType sourceDb) {
        DatabaseType realSource = (sourceDb == null ? DatabaseType.MYSQL : sourceDb);

        log.info("[SYNC] fullSyncAllFromSource 开始，全量同步所有实体，sourceDb={}", realSource);

        for (SyncEntityType type : SyncEntityType.values()) {
            if (!metadataRegistry.supports(type)) {
                continue;
            }
            fullSyncEntityFromSource(type, realSource);
        }

        log.info("[SYNC] fullSyncAllFromSource 完成，sourceDb={}", realSource);
    }

    /**
     * 同步一条记录到指定目标库，并根据 writeLog 决定是否通过事件机制记录日志
     */
    private boolean doSyncOneTarget(SyncEntityType entityType,
                                    String entityId,
                                    SyncAction action,
                                    DatabaseType sourceDb,
                                    DatabaseType targetDb,
                                    SyncExecutor executor,
                                    boolean writeLog) {

        try {
            executor.applyOne(entityType, action, entityId, sourceDb);

            // 避免“日志同步日志”的死循环：SYNC_LOG 本身不同步记录日志
            if (writeLog && entityType != SyncEntityType.SYNC_LOG) {
                publishSuccessLog(entityType, entityId, action, sourceDb, targetDb);
            }
            return true;

        } catch (Exception ex) {
            log.warn("同步失败：entityType={}，id={}，action={}，{} -> {}，错误={}",
                    entityType, entityId, action, sourceDb, targetDb, ex.getMessage(), ex);

            if (writeLog && entityType != SyncEntityType.SYNC_LOG) {
                publishFailureLog(entityType, entityId, action, sourceDb, targetDb, ex.getMessage());
            }
            // 不往外抛异常，避免影响主业务
            return false;
        }
    }

    private void publishSuccessLog(SyncEntityType entityType,
                                   String entityId,
                                   SyncAction action,
                                   DatabaseType sourceDb,
                                   DatabaseType targetDb) {
        SyncLogEvent event = new SyncLogEvent(
                entityType,
                entityId,
                action,
                sourceDb,
                targetDb,
                true,
                null
        );
        eventPublisher.publishEvent(event);
    }

    private void publishFailureLog(SyncEntityType entityType,
                                   String entityId,
                                   SyncAction action,
                                   DatabaseType sourceDb,
                                   DatabaseType targetDb,
                                   String errorMsg) {
        SyncLogEvent event = new SyncLogEvent(
                entityType,
                entityId,
                action,
                sourceDb,
                targetDb,
                false,
                errorMsg
        );
        eventPublisher.publishEvent(event);
    }
}








