package com.notice.system.service;

import com.notice.system.entity.Role;
import com.notice.system.service.base.MultiDbSyncService;
import com.notice.system.sync.DatabaseType;

import java.util.List;

/**
 * 角色服务：
 *  - 多库同步的 CRUD
 *  - 按名称查询 / 列表
 *  - 删除前检查是否仍有用户引用该角色
 */
public interface RoleService extends MultiDbSyncService<Role> {

    /**
     * 根据名称查找角色（默认库）
     */
    Role findByName(String name);

    /**
     * 根据名称查找角色（指定库）
     */
    Role findByNameInDb(DatabaseType db, String name);

    /**
     * 列出所有角色（默认库）
     */
    List<Role> listAll();

    /**
     * 列出所有角色（指定库）
     */
    List<Role> listAllFromDb(DatabaseType db);
}




