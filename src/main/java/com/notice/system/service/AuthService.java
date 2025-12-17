package com.notice.system.service;

import com.notice.system.entity.User;

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
     * 判断指定用户是否为管理员
     */
    boolean isAdmin(User user);

    /**
     * 当前用户必须为管理员，否则抛出 ForbiddenException
     */
    void requireAdmin();
}


