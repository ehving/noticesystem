package com.notice.system.controller.admin;

import com.notice.system.common.Result;
import com.notice.system.entity.Role;
import com.notice.system.service.AuthService;
import com.notice.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理接口（管理端）：
 *  - 目前只提供角色列表查询
 *  - 预留后续增加角色维护功能
 */
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RoleService roleService;
    private final AuthService authService;

    /**
     * 获取所有角色（仅管理员可访问）
     */
    @GetMapping
    public Result<List<Role>> listAllRoles() {
        // 确保当前用户是管理员
        authService.requireAdmin();

        List<Role> roles = roleService.listAll();
        return Result.success(roles);
    }
}


