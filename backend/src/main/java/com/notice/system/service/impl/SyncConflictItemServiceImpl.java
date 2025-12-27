package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.SyncConflictItem;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.mapper.base.SyncConflictItemBaseMapper;
import com.notice.system.service.SyncConflictItemService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.sync.SyncMetadataRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 冲突项（每库快照）服务：
 * - 只负责 SyncConflictItem 的 CRUD 与快照 upsert
 * - 写入必须走 saveInDb/updateByIdInDb，确保自动同步链路一致
 */
@Slf4j
@Service
public class SyncConflictItemServiceImpl
        extends MultiDbSyncServiceImpl<SyncConflictItem>
        implements SyncConflictItemService {

    public SyncConflictItemServiceImpl(SyncService syncService,
                                       SyncMetadataRegistry metadataRegistry) {
        super(syncService, metadataRegistry, SyncEntityType.SYNC_CONFLICT_ITEM, DatabaseType.MYSQL);
    }

    /**
     * 复杂查询用 mapper（写入仍走 MultiDbSyncServiceImpl 的 saveInDb/updateByIdInDb）
     */
    private SyncConflictItemBaseMapper itemMapper(DatabaseType db) {
        return resolveMapperAs(db, SyncConflictItemBaseMapper.class);
    }

    @Override
    public List<SyncConflictItem> listByConflictId(DatabaseType db, String conflictId) {
        DatabaseType useDb = useDb(db);
        if (conflictId == null || conflictId.isBlank()) return List.of();
        return Optional.ofNullable(itemMapper(useDb).listByConflictId(conflictId)).orElseGet(List::of);
    }

    @Override
    public Map<DatabaseType, SyncConflictItem> mapByConflictId(DatabaseType db, String conflictId) {
        List<SyncConflictItem> list = listByConflictId(db, conflictId);
        Map<DatabaseType, SyncConflictItem> map = new EnumMap<>(DatabaseType.class);
        for (SyncConflictItem it : list) {
            if (it != null && it.getDbType() != null) {
                map.put(it.getDbType(), it);
            }
        }
        return map;
    }

    /**
     * 刷新三库快照 items：
     * - 统一落 workDb（默认 MYSQL），用 saveInDb/updateByIdInDb 触发同步
     * - 查询用 selectOne（依赖唯一键 conflict_id + db_type）
     * - 并发下 insert 可能抛异常：catch 后 re-query 再 update
     */
    @Override
    public void upsertSnapshotItems(String conflictId,
                                    Map<DatabaseType, Integer> exists,
                                    Map<DatabaseType, String> hash,
                                    LocalDateTime now) {
        if (conflictId == null || conflictId.isBlank()) return;

        LocalDateTime t = (now == null ? LocalDateTime.now() : now);
        DatabaseType workDb = defaultDb(); // 跟随基类默认库

        for (DatabaseType dt : DatabaseType.syncDbs()) {
            Integer ex = (exists == null ? null : exists.get(dt));
            String h = (hash == null ? null : hash.get(dt));
            upsertOne(workDb, conflictId, dt, ex, h, t);
        }
    }

    private void upsertOne(DatabaseType workDb,
                           String conflictId,
                           DatabaseType dt,
                           Integer existsFlag,
                           String rowHash,
                           LocalDateTime now) {

        SyncConflictItem old = findOneByConflictAndDb(workDb, conflictId, dt);

        if (old == null) {
            // insert
            SyncConflictItem item = new SyncConflictItem();
            item.setId(UUID.randomUUID().toString().replace("-", ""));
            item.setConflictId(conflictId);
            item.setDbType(dt);
            applySnapshotFields(item, existsFlag, rowHash, now);

            try {
                saveInDb(workDb, item); // CREATE + 自动同步
                return;
            } catch (Exception e) {
                // 并发兜底：可能另一线程刚插入
                log.warn("[CONFLICT-ITEM] insert maybe duplicated, will re-query. conflictId={}, dbType={}, err={}",
                        conflictId, dt, e.getMessage());
            }

            SyncConflictItem again = findOneByConflictAndDb(workDb, conflictId, dt);
            if (again != null) {
                applySnapshotFields(again, existsFlag, rowHash, now);
                try {
                    updateByIdInDb(workDb, again); // UPDATE + 自动同步
                } catch (Exception ex2) {
                    log.warn("[CONFLICT-ITEM] update after duplicate insert failed, conflictId={}, dbType={}, err={}",
                            conflictId, dt, ex2.getMessage(), ex2);
                }
            }
            return;
        }

        // update
        applySnapshotFields(old, existsFlag, rowHash, now);
        try {
            if (updateByIdInDb(workDb, old)) return; // UPDATE + 自动同步
        } catch (Exception e) {
            log.warn("[CONFLICT-ITEM] update failed, will try re-query. conflictId={}, dbType={}, err={}",
                    conflictId, dt, e.getMessage(), e);
        }

        // 兜底：update 影响行数=0 / 或 update 异常后重查
        SyncConflictItem again = findOneByConflictAndDb(workDb, conflictId, dt);
        if (again == null) {
            SyncConflictItem item = new SyncConflictItem();
            item.setId(UUID.randomUUID().toString().replace("-", ""));
            item.setConflictId(conflictId);
            item.setDbType(dt);
            applySnapshotFields(item, existsFlag, rowHash, now);
            try {
                saveInDb(workDb, item);
            } catch (Exception e) {
                log.warn("[CONFLICT-ITEM] fallback insert failed, conflictId={}, dbType={}, err={}",
                        conflictId, dt, e.getMessage(), e);
            }
        } else {
            applySnapshotFields(again, existsFlag, rowHash, now);
            try {
                updateByIdInDb(workDb, again);
            } catch (Exception e) {
                log.warn("[CONFLICT-ITEM] fallback update failed, conflictId={}, dbType={}, err={}",
                        conflictId, dt, e.getMessage(), e);
            }
        }
    }

    private void applySnapshotFields(SyncConflictItem item,
                                     Integer existsFlag,
                                     String rowHash,
                                     LocalDateTime now) {
        item.setExistsFlag(existsFlag);
        item.setRowHash(rowHash);

        // 课设阶段先不做 version/updateTime 的跨库统一
        item.setRowVersion(null);
        item.setRowUpdateTime(null);

        item.setLastCheckedAt(now);
    }

    /**
     * 依赖唯一键 (conflict_id, db_type)，所以这里用 selectOne。
     * 注意：数据库需要加唯一索引才能保证并发下不会出现重复记录。
     */
    private SyncConflictItem findOneByConflictAndDb(DatabaseType db, String conflictId, DatabaseType dt) {
        BaseMapper<SyncConflictItem> mapper = resolveMapper(db);
        return mapper.selectOne(new QueryWrapper<SyncConflictItem>()
                .eq("conflict_id", conflictId)
                .eq("db_type", dt.name()));
    }
}

