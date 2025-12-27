package com.notice.system.controller.admin;

import com.notice.system.common.Result;
import com.notice.system.entity.Role;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.service.AuthService;
import com.notice.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口（管理端）
 * 权限：管理员
 * 选库：db 可选，默认 roleService.defaultDb()
 * 当前阶段：仅提供角色列表查询（用于用户创建/编辑时的下拉选项）。
 */
@RestController
@RequestMapping("/api/admin/roles")
@RequiredArgsConstructor
public class RoleAdminController {

    private final RoleService roleService;
    private final AuthService authService;

    /**
     * 获取所有角色（管理端）
     * 权限：管理员
     * 选库：db 可选，默认 roleService.defaultDb()
     */
    @GetMapping
    public Result<List<Role>> listAllRoles(
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = (db == null ? roleService.defaultDb() : db);
        authService.requireAdmin(useDb);

        // listAll(useDb) 已按库查询；为空返回空列表即可
        List<Role> roles = roleService.listAll(useDb);
        return Result.success(roles);
    }
}



