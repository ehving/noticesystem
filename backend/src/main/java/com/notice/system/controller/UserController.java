package com.notice.system.controller;

import com.notice.system.common.Result;
import com.notice.system.converter.UserConverter;
import com.notice.system.entity.Dept;
import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.security.JwtUtil;
import com.notice.system.service.AuthService;
import com.notice.system.service.DeptService;
import com.notice.system.service.PasswordService;
import com.notice.system.service.RoleService;
import com.notice.system.service.UserService;
import com.notice.system.vo.user.UserLoginVo;
import com.notice.system.vo.user.UserProfileVo;
import com.notice.system.vo.user.UserRegisterVo;
import com.notice.system.vo.user.UserUpdatePasswordVo;
import com.notice.system.vo.user.UserUpdateProfileVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户认证 / 个人信息接口（用户端）
 * 路径前缀：/api/user
 * 说明：
 *  - register/login 允许匿名访问
 *  - profile/password 需要登录（AuthService.requireLoginUser）
 * 返回：
 *  - login 成功返回 JWT token 字符串
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;
    private final AuthService authService;
    private final PasswordService passwordService;
    private final DeptService deptService;

    /* ==================== 1) 注册 ==================== */

    @PostMapping("/register")
    public Result<?> register(@RequestBody UserRegisterVo vo) {
        if (vo == null) return Result.fail("请求体不能为空");

        String username = trimToNull(vo.getUsername());
        String password = trimToNull(vo.getPassword());

        if (username == null) return Result.fail("用户名不能为空");
        if (password == null) return Result.fail("密码不能为空");
        if (passwordService.isWeak(password)) return Result.fail("密码太弱，至少 6 位，并包含字母和数字");

        if (userService.existsByUsername(username)) {
            return Result.fail("用户名已存在");
        }

        Role userRole = roleService.findByName("普通用户");
        if (userRole == null) {
            return Result.fail("系统未初始化角色（缺少 '普通用户' 角色）");
        }

        String storedPwd = passwordService.prepareForStore(password);
        User user = UserConverter.toEntityForRegister(vo, storedPwd, userRole.getId());

        userService.save(user);
        return Result.success("注册成功");
    }

    /* ==================== 2) 登录 ==================== */

    @PostMapping("/login")
    public Result<?> login(@RequestBody UserLoginVo vo) {
        if (vo == null) return Result.fail("请求体不能为空");

        String username = trimToNull(vo.getUsername());
        String password = trimToNull(vo.getPassword());

        if (username == null || password == null) {
            return Result.fail("用户名和密码不能为空");
        }

        User dbUser = userService.findByUsername(username);
        if (dbUser == null) {
            return Result.fail("用户不存在");
        }

        if (!passwordService.matches(password, dbUser.getPassword())) {
            return Result.fail("密码错误");
        }

        // status=0 禁用（注意 Integer 比较）
        if (Objects.equals(dbUser.getStatus(), 0)) {
            return Result.fail("账户已被禁用，请联系管理员");
        }

        String token = JwtUtil.generateToken(dbUser.getUsername());

        dbUser.setLastLoginTime(LocalDateTime.now());
        userService.updateById(dbUser);

        return Result.success(token);
    }

    /* ==================== 3) 获取当前登录用户信息 ==================== */

    @GetMapping("/profile")
    public Result<UserProfileVo> getProfile() {
        User user = authService.requireLoginUser();

        Role role = (user.getRoleId() == null) ? null : roleService.getById(user.getRoleId());

        Dept dept = null;
        if (user.getDeptId() != null && !user.getDeptId().isBlank()) {
            dept = deptService.getById(user.getDeptId());
        }

        return Result.success(UserConverter.toProfileVo(user, role, dept));
    }

    /* ==================== 4) 修改个人资料 ==================== */

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody UserUpdateProfileVo vo) {
        User user = authService.requireLoginUser();
        // vo 允许为空：Converter 自己决定“无字段则不改”
        UserConverter.applyProfileUpdate(vo, user);

        userService.updateById(user);
        return Result.success("资料更新成功");
    }

    /* ==================== 5) 修改密码 ==================== */

    @PutMapping("/password")
    public Result<?> updatePassword(@RequestBody UserUpdatePasswordVo vo) {
        if (vo == null) return Result.fail("请求体不能为空");

        String oldPwd = trimToNull(vo.getOldPassword());
        String newPwd = trimToNull(vo.getNewPassword());

        if (oldPwd == null || newPwd == null) {
            return Result.fail("旧密码和新密码不能为空");
        }

        User user = authService.requireLoginUser();

        if (!passwordService.matches(oldPwd, user.getPassword())) {
            return Result.fail("旧密码不正确");
        }

        if (passwordService.isWeak(newPwd)) {
            return Result.fail("新密码太弱，至少 6 位，并包含字母和数字");
        }

        user.setPassword(passwordService.prepareForStore(newPwd));
        userService.updateById(user);

        return Result.success("密码修改成功");
    }

    /* ==================== utils ==================== */

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}




