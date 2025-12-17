package com.notice.system.service.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.service.SyncService;
import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncAction;
import com.notice.system.sync.SyncEntityType;
import com.notice.system.sync.SyncMetadataRegistry;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 支持“选择数据库 + 自动同步”的通用 CRUD Service 抽象实现。
 *
 * 不依赖 MyBatis-Plus 的 IService/ServiceImpl，
 * 仅使用 BaseMapper 进行实际 CRUD。
 *
 * 查询：
 *  - getById / listAll / page / pageInDb 都不会触发同步，只从指定库读取数据；
 * 写操作：
 *  - save / update / remove 系列在成功后，会通过 SyncService 提交多库同步任务。
 */
@Slf4j
public abstract class MultiDbSyncServiceImpl<T> implements MultiDbSyncService<T> {

    protected final SyncService syncService;
    protected final SyncMetadataRegistry metadataRegistry;
    protected final SyncEntityType entityType;
    protected final DatabaseType defaultDb;

    protected MultiDbSyncServiceImpl(SyncService syncService,
                                     SyncMetadataRegistry metadataRegistry,
                                     SyncEntityType entityType,
                                     DatabaseType defaultDb) {
        this.syncService = syncService;
        this.metadataRegistry = metadataRegistry;
        this.entityType = entityType;
        this.defaultDb = (defaultDb == null ? DatabaseType.MYSQL : defaultDb);
    }

    @Override
    public DatabaseType defaultDb() {
        return defaultDb;
    }

    /* ======================== 查询 ======================== */

    @Override
    public T getById(Serializable id) {
        return getById(defaultDb, id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getById(DatabaseType db, Serializable id) {
        if (id == null || db == null) {
            return null;
        }
        BaseMapper<T> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[SYNC-SERVICE] getById 未找到 mapper，entityType={}，db={}，id={}",
                    entityType, db, id);
            return null;
        }
        return mapper.selectById(id);
    }

    @Override
    public List<T> listAll() {
        return listAll(defaultDb);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> listAll(DatabaseType db) {
        if (db == null) {
            return List.of();
        }
        BaseMapper<T> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[SYNC-SERVICE] listAll 未找到 mapper，entityType={}，db={}", entityType, db);
            return List.of();
        }
        return mapper.selectList(null);
    }

    @Override
    public Page<T> page(Page<T> page) {
        return pageInDb(defaultDb, page);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<T> pageInDb(DatabaseType db, Page<T> page) {
        if (db == null || page == null) {
            return page;
        }
        BaseMapper<T> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[SYNC-SERVICE] pageInDb 未找到 mapper，entityType={}，db={}", entityType, db);
            return page;
        }
        // 全表分页查询，不带条件；带条件分页由具体业务 Service 自行实现。
        return mapper.selectPage(page, null);
    }

    /* ======================== 写操作：默认库 ======================== */

    @Override
    public boolean save(T entity) {
        return saveInDb(defaultDb, entity);
    }

    @Override
    public boolean saveBatch(Collection<T> entities) {
        return saveBatchInDb(defaultDb, entities);
    }

    @Override
    public boolean updateById(T entity) {
        return updateByIdInDb(defaultDb, entity);
    }

    @Override
    public boolean removeById(Serializable id) {
        return removeByIdInDb(defaultDb, id);
    }

    @Override
    public boolean removeByIds(Collection<? extends Serializable> ids) {
        return removeByIdsInDb(defaultDb, ids);
    }

    /* ======================== 写操作：指定库 ======================== */

    @Override
    @SuppressWarnings("unchecked")
    public boolean saveInDb(DatabaseType db, T entity) {
        if (entity == null || db == null) {
            return false;
        }
        BaseMapper<T> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[SYNC-SERVICE] saveInDb 未找到 mapper，entityType={}，db={}，entityClass={}",
                    entityType, db, entity.getClass().getName());
            return false;
        }
        int rows = mapper.insert(entity);
        boolean ok = rows > 0;
        if (ok) {
            String id = extractId(entity);
            if (id != null) {
                syncService.submitSync(entityType, id, SyncAction.CREATE, db);
            } else {
                log.warn("[SYNC-SERVICE] saveInDb 后无法获取 id，entityType={}，entityClass={}",
                        entityType, entity.getClass().getName());
            }
        }
        return ok;
    }

    @Override
    public boolean saveBatchInDb(DatabaseType db, Collection<T> entities) {
        if (entities == null || entities.isEmpty() || db == null) {
            return false;
        }
        BaseMapper<T> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[SYNC-SERVICE] saveBatchInDb 未找到 mapper，entityType={}，db={}", entityType, db);
            return false;
        }

        boolean allOk = true;
        for (T entity : entities) {
            if (entity == null) {
                continue;
            }
            int rows = mapper.insert(entity);
            if (rows <= 0) {
                allOk = false;
            }
        }

        if (allOk) {
            List<String> ids = entities.stream()
                    .map(this::extractId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!ids.isEmpty()) {
                syncService.submitBatchSync(entityType, ids, SyncAction.CREATE, db);
            }
        }
        return allOk;
    }

    @Override
    public boolean updateByIdInDb(DatabaseType db, T entity) {
        if (entity == null || db == null) {
            return false;
        }
        BaseMapper<T> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[SYNC-SERVICE] updateByIdInDb 未找到 mapper，entityType={}，db={}，entityClass={}",
                    entityType, db, entity.getClass().getName());
            return false;
        }
        int rows = mapper.updateById(entity);
        boolean ok = rows > 0;
        if (ok) {
            String id = extractId(entity);
            if (id != null) {
                syncService.submitSync(entityType, id, SyncAction.UPDATE, db);
            } else {
                log.warn("[SYNC-SERVICE] updateByIdInDb 后无法获取 id，entityType={}，entityClass={}",
                        entityType, entity.getClass().getName());
            }
        }
        return ok;
    }

    @Override
    public boolean removeByIdInDb(DatabaseType db, Serializable id) {
        if (id == null || db == null) {
            return false;
        }
        BaseMapper<T> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[SYNC-SERVICE] removeByIdInDb 未找到 mapper，entityType={}，db={}，id={}",
                    entityType, db, id);
            return false;
        }
        int rows = mapper.deleteById(id);
        boolean ok = rows > 0;
        if (ok) {
            syncService.submitSync(entityType, String.valueOf(id), SyncAction.DELETE, db);
        }
        return ok;
    }

    @Override
    public boolean removeByIdsInDb(DatabaseType db, Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty() || db == null) {
            return false;
        }
        BaseMapper<T> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[SYNC-SERVICE] removeByIdsInDb 未找到 mapper，entityType={}，db={}", entityType, db);
            return false;
        }

        boolean allOk = true;
        for (Serializable id : ids) {
            if (id == null) {
                continue;
            }
            int rows = mapper.deleteById(id);
            if (rows <= 0) {
                allOk = false;
            }
        }

        List<String> idList = ids.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toList());

        if (allOk && !idList.isEmpty()) {
            syncService.submitBatchSync(entityType, idList, SyncAction.DELETE, db);
        }
        return allOk;
    }

    /* ======================== 工具方法：解析 Mapper / Id ======================== */

    @SuppressWarnings("unchecked")
    protected BaseMapper<T> resolveMapper(DatabaseType db) {
        if (db == null) {
            return null;
        }
        SyncMetadataRegistry.EntitySyncDefinition<T> def =
                (SyncMetadataRegistry.EntitySyncDefinition<T>) metadataRegistry.getDefinition(entityType);
        if (def == null) {
            return null;
        }
        return def.getMapper(db);
    }

    @SuppressWarnings("unchecked")
    protected String extractId(T entity) {
        if (entity == null) {
            return null;
        }
        SyncMetadataRegistry.EntitySyncDefinition<T> def =
                (SyncMetadataRegistry.EntitySyncDefinition<T>) metadataRegistry.getDefinition(entityType);
        if (def == null || def.getIdGetter() == null) {
            log.warn("[SYNC-SERVICE] 未在 SyncMetadataRegistry 中配置实体 {} 的 idGetter，entityClass={}",
                    entityType, entity.getClass().getName());
            return null;
        }
        try {
            return def.getIdGetter().apply(entity);
        } catch (Exception e) {
            log.warn("[SYNC-SERVICE] 调用 idGetter 失败，entityType={}，entityClass={}，错误={}",
                    entityType, entity.getClass().getName(), e.getMessage(), e);
            return null;
        }
    }
}


