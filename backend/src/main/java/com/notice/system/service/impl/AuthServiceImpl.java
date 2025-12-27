package com.notice.system.service.impl;

import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.exception.ForbiddenException;
import com.notice.system.exception.UnauthenticatedException;
import com.notice.system.service.AuthService;
import com.notice.system.service.RoleService;
import com.notice.system.service.UserService;
import com.notice.system.entityEnum.DatabaseType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 认证与鉴权相关服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final RoleService roleService;
    private final HttpServletRequest request;

    @Override
    public User requireLoginUser() {
        String username = (String) request.getAttribute("loginUser");
        if (username == null) {
            throw new UnauthenticatedException("未登录或登录已失效");
        }

        User dbUser = userService.findByUsername(username);

        if (dbUser == null) {
            throw new UnauthenticatedException("登录用户不存在，请重新登录");
        }
        return dbUser;
    }

    @Override
    public User requireLoginAdmin(DatabaseType db) {
        String username = (String) request.getAttribute("loginUser");
        if (username == null) {
            throw new UnauthenticatedException("非管理员登录或登录已失效");
        }

        User dbUser = userService.findByUsernameInDb(db,username);

        if (dbUser == null) {
            throw new UnauthenticatedException("管理员账号不存在，请重新登录");
        }
        return dbUser;
    }

    @Override
    public boolean isAdmin(User user, DatabaseType db) {
        if (user == null) return false;
        Role role = roleService.getById(db,user.getRoleId());
        return role != null && "管理员".equalsIgnoreCase(role.getName());
    }

    @Override
    public void requireAdmin(DatabaseType db) {
        User user = requireLoginUser();
        if (!isAdmin(user,db)) {
            throw new ForbiddenException("仅管理员可以执行该操作");
        }
    }
}


