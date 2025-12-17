package com.notice.system.sync;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

/**
 * 通用同步执行器基类：
 *  - 通过 SyncMetadataRegistry 找到 source/target Mapper
 *  - 实现单条记录的 CREATE / UPDATE / DELETE 同步逻辑
 *
 * 不加 @Transactional，避免多数据源下事务锁定数据源。
 */
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

    @Override
    public void applyOne(SyncEntityType entityType,
                         SyncAction action,
                         String entityId,
                         DatabaseType sourceDb) {

        SyncMetadataRegistry.EntitySyncDefinition<?> def =
                metadataRegistry.getDefinition(entityType);
        if (def == null) {
            log.warn("{} [{}] 未找到实体的同步定义，跳过，id={}", logPrefix, entityType, entityId);
            return;
        }

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
            log.warn("{} [{}] 源库 {} 未配置 Mapper，无法同步，id={}",
                    logPrefix, entityName, sourceDb, entityId);
            return;
        }
        if (targetMapper == null) {
            log.warn("{} [{}] 目标库 {} 未配置 Mapper，无法同步，id={}",
                    logPrefix, entityName, targetDb, entityId);
            return;
        }

        if (action == SyncAction.DELETE) {
            int rows = targetMapper.deleteById(entityId);
            log.info("{} [{}] DELETE 完成，sourceDb={}, targetDb={}, id={}, rows={}",
                    logPrefix, entityName, sourceDb, targetDb, entityId, rows);
            return;
        }

        // 读取源库
        T source = sourceMapper.selectById(entityId);
        log.info("{} [{}] 同步前读取源库 {}：id={}, entity={}",
                logPrefix, entityName, sourceDb, entityId, source);

        if (source == null) {
            // 源数据不存在，目标库兜底删除
            int rows = targetMapper.deleteById(entityId);
            log.info("{} [{}] 源数据不存在，执行兜底 DELETE，sourceDb={}, targetDb={}, id={}, rows={}",
                    logPrefix, entityName, sourceDb, targetDb, entityId, rows);
            return;
        }

        T target = targetMapper.selectById(entityId);
        boolean insert = (target == null);
        if (insert) {
            target = def.getEntitySupplier().get();
        }

        BeanUtils.copyProperties(source, target);

        int rows;
        if (insert) {
            rows = targetMapper.insert(target);
        } else {
            rows = targetMapper.updateById(target);
        }

        log.info("{} [{}] {} 完成，sourceDb={} -> targetDb={}，id={}, rows={}",
                logPrefix,
                entityName,
                insert ? "INSERT" : "UPDATE",
                sourceDb,
                targetDb,
                entityId,
                rows);
    }
}


