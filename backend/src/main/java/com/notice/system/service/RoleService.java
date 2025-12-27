package com.notice.system.service;

import com.notice.system.entity.Role;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.service.base.MultiDbSyncService;

import java.util.List;

/**
 * 角色服务（Role）
 *
 * <p>职责：</p>
 * <ul>
 *   <li>提供多库同步的 CRUD（继承 MultiDbSyncService）</li>
 *   <li>提供按名称查询、列表查询（支持选库）</li>
 *   <li>删除前检查：该库中是否仍有用户引用该角色（防止悬挂引用）</li>
 * </ul>
 */
public interface RoleService extends MultiDbSyncService<Role> {

    /** 默认库：按名称查找角色。 */
    Role findByName(String name);

    /** 指定库：按名称查找角色。 */
    Role findByNameInDb(DatabaseType db, String name);

    /** 默认库：列出所有角色。 */
    List<Role> listAll();

    /** 指定库：列出所有角色。 */
    List<Role> listAllFromDb(DatabaseType db);
}





