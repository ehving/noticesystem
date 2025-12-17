package com.notice.system.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.common.Result;
import com.notice.system.converter.UserConverter;
import com.notice.system.entity.Dept;
import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.service.*;
import com.notice.system.sync.DatabaseType;
import com.notice.system.vo.user.UserProfileVo;
import com.notice.system.vo.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理端用户管理接口
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


    /* ========== 1. 用户分页列表 ========== */

    @PostMapping("/page")
    public Result<Page<UserAdminListVo>> pageUsers(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody UserAdminPageVo vo
    ) {
        authService.requireAdmin();

        DatabaseType useDb = (db == null ? userService.defaultDb() : db);

        Page<User> userPage = userService.pageAdminUsersInDb(useDb, vo);

        // 转换为 VO 的分页
        Page<UserAdminListVo> voPage =
                new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserAdminListVo> records = new ArrayList<>();
        for (User user : userPage.getRecords()) {
            Role role = (user.getRoleId() != null ? roleService.getById(user.getRoleId()) : null);
            Dept dept = (user.getDeptId() != null ? deptService.getById(user.getDeptId()) : null);
            records.add(UserConverter.toAdminListVo(user, role, dept));
        }
        voPage.setRecords(records);

        return Result.success(voPage);
    }

    /* ========== 2. 查看单个用户详情 ========== */

    @GetMapping("/{id}")
    public Result<UserProfileVo> getUserDetail(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        authService.requireAdmin();

        DatabaseType useDb = (db == null ? userService.defaultDb() : db);

        User user = userService.getById(useDb, id);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        Role role = (user.getRoleId() != null ? roleService.getById(user.getRoleId()) : null);
        Dept dept = (user.getDeptId() != null ? deptService.getById(user.getDeptId()) : null);

        UserProfileVo vo = UserConverter.toProfileVo(user, role, dept);
        return Result.success(vo);
    }

    /* ========== 3. 管理端创建用户 ========== */

    @PostMapping
    public Result<?> createUser(@RequestBody UserAdminCreateVo vo) {
        authService.requireAdmin();

        if (vo.getUsername() == null || vo.getUsername().isBlank()) {
            return Result.fail("用户名不能为空");
        }
        if (vo.getPassword() == null || vo.getPassword().isBlank()) {
            return Result.fail("密码不能为空");
        }
        if (vo.getRoleId() == null || vo.getRoleId().isBlank()) {
            return Result.fail("角色不能为空");
        }

        DatabaseType sourceDb =
                (vo.getSourceDb() == null ? userService.defaultDb() : vo.getSourceDb());

        // 同库检查用户名是否存在
        if (userService.existsByUsernameInDb(sourceDb, vo.getUsername())) {
            return Result.fail("用户名已存在");
        }

        //
        String rawPwd = vo.getPassword();
        if (passwordService.isWeak(rawPwd)) {
            return Result.fail("密码太弱，至少 6 位，并包含字母和数字");
        }
        String storedPwd = passwordService.prepareForStore(rawPwd);

        User user = UserConverter.toEntityForAdminCreate(vo,storedPwd);

        // 通过 MultiDbSyncService 在指定库插入，并触发同步
        boolean ok = userService.saveInDb(sourceDb, user);

        if (!ok) {
            return Result.fail("创建用户失败");
        }
        return Result.success("创建用户成功");
    }

    /* ========== 4. 管理端更新用户信息（包含角色 / 部门 / 状态） ========== */

    @PutMapping("/{id}")
    public Result<?> updateUser(
            @PathVariable("id") String id,
            @RequestBody UserAdminUpdateVo vo
    ) {
        authService.requireAdmin();

        DatabaseType sourceDb =
                (vo.getSourceDb() == null ? userService.defaultDb() : vo.getSourceDb());

        User user = userService.getById(sourceDb, id);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        // 将 VO 中的变更应用到实体上
        UserConverter.applyAdminUpdate(vo, user);

        // 通过 updateByIdInDb 触发同步
        boolean ok = userService.updateByIdInDb(sourceDb, user);
        if (!ok) {
            return Result.fail("更新用户失败");
        }

        return Result.success("用户更新成功");
    }

    /* ========== 5. 管理端重置用户密码 ========== */

    @PutMapping("/{id}/reset-password")
    public Result<?> resetPassword(
            @PathVariable("id") String id,
            @RequestBody UserAdminResetPasswordVo vo
    ) {
        authService.requireAdmin();

        if (vo.getNewPassword() == null || vo.getNewPassword().isBlank()) {
            return Result.fail("新密码不能为空");
        }

        DatabaseType sourceDb =
                (vo.getSourceDb() == null ? userService.defaultDb() : vo.getSourceDb());

        // TODO: 以后有加密的话，在这里先 encode 再传进去
        String rawPwd = vo.getNewPassword();
        if (passwordService.isWeak(rawPwd)) {
            return Result.fail("新密码太弱，至少 6 位，并包含字母和数字");
        }
        String storedPwd = passwordService.prepareForStore(rawPwd);
        boolean ok = userService.resetPasswordInDb(sourceDb, id, storedPwd);
        if (!ok) {
            return Result.fail("重置密码失败");
        }

        return Result.success("密码已重置");
    }

    /* ========== 6. 删除用户 ========== */

    @DeleteMapping("/{id}")
    public Result<?> deleteUser(
            @PathVariable("id") String id,
            @RequestParam(name = "sourceDb", required = false) DatabaseType sourceDb
    ) {
        authService.requireAdmin();

        DatabaseType db = (sourceDb == null ? userService.defaultDb() : sourceDb);

        boolean ok = userService.removeByIdInDb(db, id);
        if (!ok) {
            return Result.fail("删除用户失败");
        }
        return Result.success("用户删除成功");
    }
}

