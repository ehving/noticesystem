package com.notice.system.sync;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncAction;
import com.notice.system.entityEnum.SyncEntityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.lang.reflect.Method;
import java.util.Objects;

/** 基于 SyncMetadataRegistry 的通用同步执行器（单条 CREATE/UPDATE/DELETE）。 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractSyncExecutor implements SyncExecutor {

    private final DatabaseType targetDb;
    private final String logPrefix;
    private final SyncMetadataRegistry metadataRegistry;

    @Override
    public DatabaseType targetDb() {
        return targetDb;
    }

    /** 将源库的一条变更同步到目标库。 */
    @Override
    public void applyOne(SyncEntityType entityType,
                         SyncAction action,
                         String entityId,
                         DatabaseType sourceDb) {

        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(action, "action must not be null");
        if (entityId == null || entityId.isBlank()) {
            throw new IllegalArgumentException("entityId must not be blank");
        }
        Objects.requireNonNull(sourceDb, "sourceDb must not be null");

        SyncMetadataRegistry.EntitySyncDefinition<?> def = metadataRegistry.getDefinition(entityType);
        doApply(def, action, entityId, sourceDb);
    }

    private <T> void doApply(SyncMetadataRegistry.EntitySyncDefinition<T> def,
                             SyncAction action,
                             String entityId,
                             DatabaseType sourceDb) {

        String entityName = def.getEntityName();
        BaseMapper<T> sourceMapper = def.getMapper(sourceDb);
        BaseMapper<T> targetMapper = def.getMapper(targetDb);

        if (sourceMapper == null) {
            throw new IllegalStateException("Source mapper not configured: entity=" + entityName + ", db=" + sourceDb);
        }
        if (targetMapper == null) {
            throw new IllegalStateException("Target mapper not configured: entity=" + entityName + ", db=" + targetDb);
        }

        // DELETE：目标库直接删
        if (action == SyncAction.DELETE) {
            int rows = targetMapper.deleteById(entityId);
            log.info("{} [{}] DELETE sourceDb={} -> targetDb={}, id={}, rows={}",
                    logPrefix, entityName, sourceDb, targetDb, entityId, rows);
            return;
        }

        // 读取源库
        T source = sourceMapper.selectById(entityId);
        log.debug("{} [{}] READ sourceDb={}, id={}, entity={}", logPrefix, entityName, sourceDb, entityId, source);

        // 源库不存在：目标库兜底删除（避免幽灵数据）
        if (source == null) {
            int rows = targetMapper.deleteById(entityId);
            log.info("{} [{}] SOURCE_MISSING -> DELETE targetDb={}, id={}, rows={}",
                    logPrefix, entityName, targetDb, entityId, rows);
            return;
        }

        // upsert：目标有则 update，没有则 insert
        T target = targetMapper.selectById(entityId);
        boolean insert = (target == null);
        if (insert) {
            target = def.getEntitySupplier().get();
        }

        BeanUtils.copyProperties(source, target);
        ensureStringIdIfPossible(target, entityId);

        int rows = insert ? targetMapper.insert(target) : targetMapper.updateById(target);
        log.info("{} [{}] {} sourceDb={} -> targetDb={}, id={}, rows={}",
                logPrefix, entityName, insert ? "INSERT" : "UPDATE", sourceDb, targetDb, entityId, rows);
    }

    /** 尝试通过 setId(String) 兜底写入主键（没有该方法则忽略）。 */
    private void ensureStringIdIfPossible(Object entity, String entityId) {
        if (entity == null) return;

        try {
            Method getId = entity.getClass().getMethod("getId");
            Object val = getId.invoke(entity);
            if (val instanceof String s && !s.isBlank()) {
                return;
            }
            if (val != null && !(val instanceof String)) {
                // 非 String 的 id，不做强行覆盖，避免误伤
                return;
            }
        } catch (NoSuchMethodException ignore) {
            return;
        } catch (Exception e) {
            // getId 失败也不致命，继续尝试 setId
            log.debug("{} ensureId(getId) failed: {}", logPrefix, e.getMessage());
        }

        try {
            Method setId = entity.getClass().getMethod("setId", String.class);
            setId.invoke(entity, entityId);
        } catch (NoSuchMethodException ignore) {
            // 没有 setId(String) 就算了
        } catch (Exception e) {
            log.debug("{} ensureId(setId) failed: {}", logPrefix, e.getMessage());
        }
    }
}



