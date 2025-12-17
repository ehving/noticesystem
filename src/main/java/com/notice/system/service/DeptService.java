package com.notice.system.service;

import com.notice.system.entity.Dept;
import com.notice.system.service.base.MultiDbSyncService;
import com.notice.system.sync.DatabaseType;

import java.util.List;

/**
 * 部门服务：
 *  - 基础 CRUD + 多库同步（来自 MultiDbSyncService）
 *  - 部门下拉、部门树、删除校验等业务方法
 */
public interface DeptService extends MultiDbSyncService<Dept> {

    /**
     * 按名称查找部门（默认从 defaultDb，即 MYSQL 查）
     */
    Dept findByName(String name);

    /**
     * 按名称查找部门（从指定库查）
     */
    Dept findByNameInDb(DatabaseType db, String name);

    /**
     * 列出所有启用状态的部门（默认从 defaultDb 查）
     */
    List<Dept> listEnabled();

    /**
     * 列出所有启用状态的部门（从指定库查）
     */
    List<Dept> listEnabledFromDb(DatabaseType db);

    /**
     * 根据 parentId 列出子部门（默认从 defaultDb 查）
     *  - parentId 为空时，通常表示根部门
     */
    List<Dept> listByParentId(String parentId);

    /**
     * 根据 parentId 列出子部门（从指定库查）
     */
    List<Dept> listByParentIdFromDb(DatabaseType db, String parentId);

    /**
     * 是否存在子部门（默认从 defaultDb 查）
     */
    boolean hasChildren(String deptId);

    /**
     * 是否存在子部门（从指定库查）
     */
    boolean hasChildrenInDb(DatabaseType db, String deptId);
}



