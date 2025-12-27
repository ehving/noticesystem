package com.notice.system.service;

import com.notice.system.entity.User;
import com.notice.system.entityEnum.DatabaseType;

/**
 * 认证与鉴权相关服务接口：
 *  - 获取当前登录用户
 *  - 判断是否管理员
 *  - 管理员权限校验
 */
public interface AuthService {

    /**
     * 获取当前登录用户，未登录则抛出 UnauthenticatedException
     */
    User requireLoginUser();
    /**
     * 获取当前登录管理员，未登录则抛出 UnauthenticatedException
     */
    User requireLoginAdmin(DatabaseType db);

    /**
     * 判断指定用户是否为管理员
     */
    boolean isAdmin(User user, DatabaseType db);

    /**
     * 当前用户必须为管理员，否则抛出 ForbiddenException
     */
    void requireAdmin(DatabaseType db);
}


