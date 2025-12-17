package com.notice.system.service.base;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.sync.DatabaseType;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 支持“选择数据库 + 自动同步”的通用 CRUD Service 接口。
 *
 * 设计原则：
 *  - 查询操作（get / list / page）不触发同步；
 *  - 写操作（save / update / remove）在成功后调用 SyncService 做多库同步；
 *  - 默认库 + 可选库 两套方法都提供。
 *
 * 说明：
 *  - 这里的分页 page / pageInDb 只做“全表分页”（不带查询条件），
 *    具体带条件的分页由各业务 Service 自己定义方法实现。
 */
public interface MultiDbSyncService<T> {

    /**
     * 默认源库（一般是 MYSQL）
     */
    DatabaseType defaultDb();

    /* ======================== 查询，不触发同步 ======================== */

    /**
     * 使用默认库按 id 查询
     */
    T getById(Serializable id);

    /**
     * 使用指定库按 id 查询
     */
    T getById(DatabaseType db, Serializable id);

    /**
     * 查询默认库全部记录
     */
    List<T> listAll();

    /**
     * 查询指定库全部记录
     */
    List<T> listAll(DatabaseType db);

    /**
     * 使用默认库进行“全表分页查询”（不带条件）
     */
    Page<T> page(Page<T> page);

    /**
     * 使用指定库进行“全表分页查询”（不带条件）
     */
    Page<T> pageInDb(DatabaseType db, Page<T> page);

    /* ======================== 写操作，会触发同步 ======================== */

    /**
     * 在默认库插入一条记录，并以默认库为源库进行同步
     */
    boolean save(T entity);

    /**
     * 在指定库插入一条记录，并以该库为源库进行同步
     */
    boolean saveInDb(DatabaseType db, T entity);

    /**
     * 在默认库批量插入，并以默认库为源库进行同步
     */
    boolean saveBatch(Collection<T> entities);

    /**
     * 在指定库批量插入，并以该库为源库进行同步
     */
    boolean saveBatchInDb(DatabaseType db, Collection<T> entities);

    /**
     * 在默认库按 id 更新，并以默认库为源库进行同步
     */
    boolean updateById(T entity);

    /**
     * 在指定库按 id 更新，并以该库为源库进行同步
     */
    boolean updateByIdInDb(DatabaseType db, T entity);

    /**
     * 在默认库按 id 删除，并以默认库为源库进行同步
     */
    boolean removeById(Serializable id);

    /**
     * 在指定库按 id 删除，并以该库为源库进行同步
     */
    boolean removeByIdInDb(DatabaseType db, Serializable id);

    /**
     * 在默认库按 id 集合删除，并以默认库为源库进行同步
     */
    boolean removeByIds(Collection<? extends Serializable> ids);

    /**
     * 在指定库按 id 集合删除，并以该库为源库进行同步
     */
    boolean removeByIdsInDb(DatabaseType db, Collection<? extends Serializable> ids);
}

