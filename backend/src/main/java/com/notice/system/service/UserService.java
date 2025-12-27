package com.notice.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.User;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.service.base.MultiDbSyncService;
import com.notice.system.vo.user.UserAdminPageVo;

import java.util.List;

/**
 * 用户服务（User）
 *
 * <p>职责：</p>
 * <ul>
 *   <li>多库同步 CRUD（继承 MultiDbSyncService）</li>
 *   <li>用户名查询/存在性校验（支持选库）</li>
 *   <li>管理端分页（指定库）</li>
 *   <li>管理端操作：重置密码/修改状态（走同步框架触发多库同步）</li>
 * </ul>
 */
public interface UserService extends MultiDbSyncService<User> {

    User findByUsername(String username);

    User findByUsernameInDb(DatabaseType db, String username);

    List<User> listByRoleId(String roleId);

    List<User> listByRoleIdInDb(DatabaseType db, String roleId);

    boolean existsByUsername(String username);

    boolean existsByUsernameInDb(DatabaseType db, String username);

    Page<User> pageAdminUsersInDb(DatabaseType db, UserAdminPageVo vo);

    boolean resetPasswordInDb(DatabaseType db, String userId, String newPassword);

    boolean updateStatusInDb(DatabaseType db, String userId, Integer status);
}





