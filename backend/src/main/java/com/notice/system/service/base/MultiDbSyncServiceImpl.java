package com.notice.system.service.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncAction;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.service.SyncService;
import com.notice.system.sync.SyncMetadataRegistry;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于 BaseMapper 的多库 CRUD，并在写成功后提交同步任务。
 *
 * <p>设计约定：</p>
 * <ul>
 *   <li>defaultDb：当前实体（entityType）的“默认工作库/源库”</li>
 *   <li>resolveMapper：解析当前实体在指定库的 Mapper，缺失则 fail-fast 抛异常</li>
 *   <li>写操作统一通过 saveInDb/updateByIdInDb/removeByIdInDb 等，成功后提交同步任务</li>
 * </ul>
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
        this.syncService = Objects.requireNonNull(syncService, "syncService must not be null");
        this.metadataRegistry = Objects.requireNonNull(metadataRegistry, "metadataRegistry must not be null");
        this.entityType = Objects.requireNonNull(entityType, "entityType must not be null");
        this.defaultDb = (defaultDb == null ? DatabaseType.MYSQL : defaultDb);
    }

    @Override
    public DatabaseType defaultDb() {
        return defaultDb;
    }

    /* ======================== 查询 ======================== */

    /** 默认库按主键查询。 */
    @Override
    public T getById(Serializable id) {
        return getById(defaultDb, id);
    }

    /** 指定库按主键查询。 */
    @Override
    public T getById(DatabaseType db, Serializable id) {
        if (id == null) return null;
        return resolveMapper(db).selectById(id);
    }

    /** 默认库查询全量列表（无条件）。 */
    @Override
    public List<T> listAll() {
        return listAll(defaultDb);
    }

    /** 指定库查询全量列表（无条件）。 */
    @Override
    public List<T> listAll(DatabaseType db) {
        return Optional.ofNullable(resolveMapper(db).selectList(null)).orElseGet(List::of);
    }

    /** 默认库分页查询（无条件）。 */
    @Override
    public Page<T> page(Page<T> page) {
        return pageInDb(defaultDb, page);
    }

    /** 指定库分页查询（无条件）。 */
    @Override
    public Page<T> pageInDb(DatabaseType db, Page<T> page) {
        Objects.requireNonNull(page, "page must not be null");
        return resolveMapper(db).selectPage(page, null);
    }

    /* ======================== 写操作：默认库 ======================== */

    /** 默认库插入单条记录并触发同步。 */
    @Override
    public boolean save(T entity) {
        return saveInDb(defaultDb, entity);
    }

    /** 默认库批量插入并触发同步。 */
    @Override
    public boolean saveBatch(Collection<T> entities) {
        return saveBatchInDb(defaultDb, entities);
    }

    /** 默认库按主键更新并触发同步。 */
    @Override
    public boolean updateById(T entity) {
        return updateByIdInDb(defaultDb, entity);
    }

    /** 默认库按主键删除并触发同步。 */
    @Override
    public boolean removeById(Serializable id) {
        return removeByIdInDb(defaultDb, id);
    }

    /** 默认库批量删除并触发同步。 */
    @Override
    public boolean removeByIds(Collection<? extends Serializable> ids) {
        return removeByIdsInDb(defaultDb, ids);
    }

    /* ======================== 写操作：指定库 ======================== */

    /** 指定库插入单条记录并触发同步。 */
    @Override
    public boolean saveInDb(DatabaseType db, T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        int rows = resolveMapper(db).insert(entity);
        if (rows <= 0) return false;

        String id = extractId(entity);
        if (id != null) {
            syncService.submitSync(entityType, id, SyncAction.CREATE, useDb(db));
        } else {
            log.warn("[SYNC] insert ok but id is null, entityType={}, entityClass={}",
                    entityType, entity.getClass().getName());
        }
        return true;
    }

    /** 指定库批量插入（全部成功才触发 batch sync）。 */
    @Override
    public boolean saveBatchInDb(DatabaseType db, Collection<T> entities) {
        if (entities == null || entities.isEmpty()) return false;

        BaseMapper<T> mapper = resolveMapper(db);
        boolean allOk = true;
        List<String> ids = new ArrayList<>(entities.size());

        for (T e : entities) {
            if (e == null) continue;
            if (mapper.insert(e) <= 0) allOk = false;

            String id = extractId(e);
            if (id != null) ids.add(id);
        }

        if (allOk && !ids.isEmpty()) {
            syncService.submitBatchSync(entityType, ids, SyncAction.CREATE, useDb(db));
        }
        return allOk;
    }

    /** 指定库按主键更新并触发同步。 */
    @Override
    public boolean updateByIdInDb(DatabaseType db, T entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        int rows = resolveMapper(db).updateById(entity);
        if (rows <= 0) return false;

        String id = extractId(entity);
        if (id != null) {
            syncService.submitSync(entityType, id, SyncAction.UPDATE, useDb(db));
        } else {
            log.warn("[SYNC] update ok but id is null, entityType={}, entityClass={}",
                    entityType, entity.getClass().getName());
        }
        return true;
    }

    /** 指定库按主键删除并触发同步。 */
    @Override
    public boolean removeByIdInDb(DatabaseType db, Serializable id) {
        if (id == null) return false;

        int rows = resolveMapper(db).deleteById(id);
        if (rows <= 0) return false;

        syncService.submitSync(entityType, String.valueOf(id), SyncAction.DELETE, useDb(db));
        return true;
    }

    /** 指定库批量删除（全部成功才触发 batch sync）。 */
    @Override
    public boolean removeByIdsInDb(DatabaseType db, Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return false;

        BaseMapper<T> mapper = resolveMapper(db);
        List<String> idList = ids.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toList());

        boolean allOk = true;
        for (String id : idList) {
            if (mapper.deleteById(id) <= 0) allOk = false;
        }

        if (allOk && !idList.isEmpty()) {
            syncService.submitBatchSync(entityType, idList, SyncAction.DELETE, useDb(db));
        }
        return allOk;
    }

    /* ======================== 内部工具 ======================== */

    /**
     * 解析“当前实体(entityType)”在指定库的 Mapper（未配置直接 fail-fast 抛异常）。
     */
    protected BaseMapper<T> resolveMapper(DatabaseType db) {
        SyncMetadataRegistry.EntitySyncDefinition<T> def = metadataRegistry.getDefinition(entityType);
        if (def == null) {
            throw new IllegalStateException("Definition not found: entityType=" + entityType);
        }
        BaseMapper<T> mapper = def.getMapper(useDb(db));
        if (mapper == null) {
            throw new IllegalStateException("Mapper not found: entityType=" + entityType + ", db=" + useDb(db));
        }
        return mapper;
    }

    /**
     * 当前实体 mapper 的“强转版”，避免各 Service 里反复写 instanceof 模板代码。
     */
    protected <M> M resolveMapperAs(DatabaseType db, Class<M> mapperType) {
        Objects.requireNonNull(mapperType, "mapperType must not be null");
        BaseMapper<T> mapper = resolveMapper(db);
        if (!mapperType.isInstance(mapper)) {
            throw new IllegalStateException("Mapper type mismatch: entityType=" + entityType
                    + ", db=" + useDb(db)
                    + ", expect=" + mapperType.getName()
                    + ", actual=" + mapper.getClass().getName());
        }
        return mapperType.cast(mapper);
    }

    /**
     * 跨实体取 Mapper（用于 Conflict 这类需要读取别的实体的场景）。
     * <p>注意：这是“读取/复杂查询”的基础工具，写入仍建议走对应实体的 MultiDbSyncServiceImpl 以保证同步链路一致。</p>
     */
    protected <E> BaseMapper<E> resolveBaseMapperOf(SyncEntityType type, DatabaseType db) {
        Objects.requireNonNull(type, "type must not be null");

        SyncMetadataRegistry.EntitySyncDefinition<E> def = metadataRegistry.getDefinition(type);
        if (def == null) {
            throw new IllegalStateException("Definition not found: entityType=" + type);
        }
        BaseMapper<E> mapper = def.getMapper(useDb(db));
        if (mapper == null) {
            throw new IllegalStateException("Mapper not found: entityType=" + type + ", db=" + useDb(db));
        }
        return mapper;
    }

    /**
     * 跨实体取“指定类型”的 Mapper（带类型校验）。
     */
    protected <E, M extends BaseMapper<E>> M resolveMapperOf(SyncEntityType type,
                                                             DatabaseType db,
                                                             Class<M> mapperType) {
        Objects.requireNonNull(mapperType, "mapperType must not be null");
        BaseMapper<E> mapper = resolveBaseMapperOf(type, db);
        if (!mapperType.isInstance(mapper)) {
            throw new IllegalStateException("Mapper type mismatch: entityType=" + type
                    + ", db=" + useDb(db)
                    + ", expect=" + mapperType.getName()
                    + ", actual=" + mapper.getClass().getName());
        }
        return mapperType.cast(mapper);
    }

    /** 提取实体主键字符串。 */
    protected String extractId(T entity) {
        if (entity == null) return null;

        SyncMetadataRegistry.EntitySyncDefinition<T> def = metadataRegistry.getDefinition(entityType);
        if (def == null) return null;

        try {
            return def.getIdGetter().apply(entity);
        } catch (Exception e) {
            log.warn("[SYNC] idGetter failed, entityType={}, entityClass={}, err={}",
                    entityType, entity.getClass().getName(), e.getMessage(), e);
            return null;
        }
    }

    /** db 为空时回落 defaultDb。 */
    protected DatabaseType useDb(DatabaseType db) {
        return (db == null ? defaultDb : db);
    }
}
