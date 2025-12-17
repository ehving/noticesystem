package com.notice.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.User;
import com.notice.system.service.base.MultiDbSyncService;
import com.notice.system.sync.DatabaseType;
import com.notice.system.vo.user.UserAdminPageVo;

import java.util.List;

/**
 * 用户服务：
 *  - 提供多库同步的 CRUD
 *  - 提供按用户名查询 / 校验接口，并支持按指定数据源执行
 */
public interface UserService extends MultiDbSyncService<User> {

    /**
     * 根据用户名查询用户（默认从 defaultDb 查询）
     */
    User findByUsername(String username);

    /**
     * 根据用户名查询用户（从指定库查询）
     */
    User findByUsernameInDb(DatabaseType db, String username);

    /**
     * 使用默认库，按 roleId 查询用户列表
     */
    List<User> listByRoleId(String roleId);

    /**
     * 在指定库，按 roleId 查询用户列表
     */
    List<User> listByRoleIdInDb(DatabaseType db, String roleId);

    /**
     * 用户名是否已存在（默认从 defaultDb 查询）
     */
    boolean existsByUsername(String username);

    /**
     * 用户名是否已存在（从指定库查询）
     */
    boolean existsByUsernameInDb(DatabaseType db, String username);

    /**
     * 管理端分页查询（指定库）
     */
    Page<User> pageAdminUsersInDb(DatabaseType db, UserAdminPageVo vo);

    /**
     * 管理员重置密码（指定库）
     * 通过我们目前的同步框架实现，会触发多库同步
     */
    boolean resetPasswordInDb(DatabaseType db, String userId, String newPassword);

    /**
     * 管理员修改用户状态（指定库）
     */
    boolean updateStatusInDb(DatabaseType db, String userId, Integer status);
}




