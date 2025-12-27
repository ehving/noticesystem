package com.notice.system.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.common.Result;
import com.notice.system.converter.UserConverter;
import com.notice.system.entity.Dept;
import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.service.*;
import com.notice.system.vo.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户管理接口（管理端）
 * 权限：管理员
 * 功能：
 *  - 分页查询用户列表（可按条件筛选）
 *  - 查看用户详情
 *  - 创建/更新/删除用户
 *  - 重置用户密码
 * 选库说明：
 *  - 查询接口：通过请求参数 db 选择库（默认 userService.defaultDb()）
 *  - 写入接口：通过 VO 内的 sourceDb 选择源库（默认 userService.defaultDb()），并触发多库同步
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final AuthService authService;
    private final UserService userService;
    private final RoleService roleService;
    private final DeptService deptService;
    private final PasswordService passwordService;

    /* ======================== 1) 用户分页列表 ======================== */

    /**
     * 管理端分页查询用户
     * 权限：管理员
     * 选库：db 可选，默认 userService.defaultDb()
     */
    @PostMapping("/page")
    public Result<Page<UserAdminListVo>> pageUsers(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody UserAdminPageVo vo
    ) {
        DatabaseType useDb = (db == null ? userService.defaultDb() : db);
        authService.requireAdmin(useDb);

        Page<User> userPage = userService.pageAdminUsersInDb(useDb, vo);

        // 转换为 VO 分页（records 逐条拼装 role/dept 展示信息）
        Page<UserAdminListVo> voPage = new Page<>(
                userPage.getCurrent(), userPage.getSize(), userPage.getTotal()
        );

        List<UserAdminListVo> records = new ArrayList<>();
        List<User> raw = userPage.getRecords();
        if (raw != null) {
            for (User user : raw) {
                if (user == null) continue;

                Role role = (user.getRoleId() == null ? null : roleService.getById(useDb, user.getRoleId()));
                Dept dept = (user.getDeptId() == null ? null : deptService.getById(useDb, user.getDeptId()));
                records.add(UserConverter.toAdminListVo(user, role, dept));
            }
        }
        voPage.setRecords(records);

        return Result.success(voPage);
    }

    /* ======================== 2) 查看用户详情 ======================== */

    /**
     * 查看单个用户详情
     * 权限：管理员
     * 选库：db 可选，默认 userService.defaultDb()
     */
    @GetMapping("/{id}")
    public Result<UserProfileVo> getUserDetail(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = (db == null ? userService.defaultDb() : db);
        authService.requireAdmin(useDb);

        if (id == null || id.isBlank()) {
            return Result.fail("用户 id 不能为空");
        }

        User user = userService.getById(useDb, id);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        Role role = (user.getRoleId() == null ? null : roleService.getById(useDb, user.getRoleId()));
        Dept dept = (user.getDeptId() == null ? null : deptService.getById(useDb, user.getDeptId()));

        return Result.success(UserConverter.toProfileVo(user, role, dept));
    }

    /* ======================== 3) 创建用户 ======================== */

    /**
     * 管理端创建用户（会触发多库同步）
     * 权限：管理员
     * 源库：从 vo.sourceDb 读取；为空则使用 userService.defaultDb()
     * 规则：
     *  - username 同库唯一
     *  - password 需通过 PasswordPolicy 校验（当前阶段明文存储）
     *  - roleId 必填（合法性校验在 service 层做）
     */
    @PostMapping
    public Result<?> createUser(@RequestBody UserAdminCreateVo vo) {
        DatabaseType sourceDb = (vo.getSourceDb() == null ? userService.defaultDb() : vo.getSourceDb());
        authService.requireAdmin(sourceDb);

        if (vo.getUsername() == null || vo.getUsername().isBlank()) return Result.fail("用户名不能为空");
        if (vo.getPassword() == null || vo.getPassword().isBlank()) return Result.fail("密码不能为空");
        if (vo.getRoleId() == null || vo.getRoleId().isBlank()) return Result.fail("角色不能为空");

        // 同库唯一性检查
        if (userService.existsByUsernameInDb(sourceDb, vo.getUsername())) {
            return Result.fail("用户名已存在");
        }

        // 密码强度（当前阶段：明文存储）
        String rawPwd = vo.getPassword();
        if (passwordService.isWeak(rawPwd)) {
            return Result.fail("密码太弱，至少 6 位，并包含字母和数字");
        }
        String storedPwd = passwordService.prepareForStore(rawPwd);

        User user = UserConverter.toEntityForAdminCreate(vo, storedPwd);

        boolean ok = userService.saveInDb(sourceDb, user);
        return ok ? Result.success("创建用户成功") : Result.fail("创建用户失败");
    }

    /* ======================== 4) 更新用户 ======================== */

    /**
     * 管理端更新用户（会触发多库同步）
     * 权限：管理员
     * 源库：从 vo.sourceDb 读取；为空则使用 userService.defaultDb()
     * 说明：允许修改角色/部门/状态等字段（字段合法性校验在 service 层处理）。
     */
    @PutMapping("/{id}")
    public Result<?> updateUser(
            @PathVariable("id") String id,
            @RequestBody UserAdminUpdateVo vo
    ) {
        DatabaseType sourceDb = (vo.getSourceDb() == null ? userService.defaultDb() : vo.getSourceDb());
        authService.requireAdmin(sourceDb);

        if (id == null || id.isBlank()) {
            return Result.fail("用户 id 不能为空");
        }

        User user = userService.getById(sourceDb, id);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        UserConverter.applyAdminUpdate(vo, user);

        boolean ok = userService.updateByIdInDb(sourceDb, user);
        return ok ? Result.success("用户更新成功") : Result.fail("更新用户失败");
    }

    /* ======================== 5) 重置密码 ======================== */

    /**
     * 管理端重置用户密码（会触发多库同步）
     * 权限：管理员
     * 源库：从 vo.sourceDb 读取；为空则使用 userService.defaultDb()
     * 当前阶段：明文存储（如后续加密，可在这里 prepareForStore 前进行 encode）。
     */
    @PutMapping("/{id}/reset-password")
    public Result<?> resetPassword(
            @PathVariable("id") String id,
            @RequestBody UserAdminResetPasswordVo vo
    ) {
        DatabaseType sourceDb = (vo.getSourceDb() == null ? userService.defaultDb() : vo.getSourceDb());
        authService.requireAdmin(sourceDb);

        if (id == null || id.isBlank()) return Result.fail("用户 id 不能为空");
        if (vo.getNewPassword() == null || vo.getNewPassword().isBlank()) return Result.fail("新密码不能为空");

        String rawPwd = vo.getNewPassword();
        if (passwordService.isWeak(rawPwd)) {
            return Result.fail("新密码太弱，至少 6 位，并包含字母和数字");
        }

        String storedPwd = passwordService.prepareForStore(rawPwd);

        boolean ok = userService.resetPasswordInDb(sourceDb, id, storedPwd);
        return ok ? Result.success("密码已重置") : Result.fail("重置密码失败");
    }

    /* ======================== 6) 删除用户 ======================== */

    /**
     * 删除用户（会触发多库同步）
     * 权限：管理员
     * 选库：sourceDb 可选，默认 userService.defaultDb()
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteUser(
            @PathVariable("id") String id,
            @RequestParam(name = "sourceDb", required = false) DatabaseType sourceDb
    ) {
        DatabaseType db = (sourceDb == null ? userService.defaultDb() : sourceDb);
        authService.requireAdmin(db);

        if (id == null || id.isBlank()) {
            return Result.fail("用户 id 不能为空");
        }

        boolean ok = userService.removeByIdInDb(db, id);
        return ok ? Result.success("用户删除成功") : Result.fail("删除用户失败");
    }
}


