package com.notice.system.controller;

import com.notice.system.common.Result;
import com.notice.system.converter.UserConverter;
import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.service.AuthService;
import com.notice.system.service.PasswordService;
import com.notice.system.service.RoleService;
import com.notice.system.service.UserService;
import com.notice.system.security.JwtUtil;
import com.notice.system.vo.user.UserLoginVo;
import com.notice.system.vo.user.UserProfileVo;
import com.notice.system.vo.user.UserRegisterVo;
import com.notice.system.vo.user.UserUpdatePasswordVo;
import com.notice.system.vo.user.UserUpdateProfileVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 用户认证 / 个人信息相关接口
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

    // ==================== 1. 注册 ====================

    @PostMapping("/register")
    public Result<?> register(@RequestBody UserRegisterVo vo) {

        if (vo.getUsername() == null || vo.getUsername().isBlank()) {
            return Result.fail("用户名不能为空");
        }
        if (vo.getPassword() == null || vo.getPassword().isBlank()) {
            return Result.fail("密码不能为空");
        }
        if (passwordService.isWeak(vo.getPassword())) {
            return Result.fail("密码太弱，至少 6 位，并包含字母和数字");
        }

        if (userService.existsByUsername(vo.getUsername())) {
            return Result.fail("用户名已存在");
        }

        Role userRole = roleService.findByName("普通用户");
        if (userRole == null) {
            return Result.fail("系统未初始化角色（缺少 '普通用户' 角色）");
        }

        String storedPwd = passwordService.prepareForStore(vo.getPassword());
        User user = UserConverter.toEntityForRegister(vo, storedPwd, userRole.getId());

        userService.save(user);
        return Result.success("注册成功");
    }


    // ==================== 2. 登录 ====================

    @PostMapping("/login")
    public Result<?> login(@RequestBody UserLoginVo vo) {

        if (vo.getUsername() == null || vo.getUsername().isBlank()
                || vo.getPassword() == null || vo.getPassword().isBlank()) {
            return Result.fail("用户名和密码不能为空");
        }

        User dbUser = userService.findByUsername(vo.getUsername());
        if (dbUser == null) {
            return Result.fail("用户不存在");
        }

        if (!passwordService.matches(vo.getPassword(), dbUser.getPassword())) {
            return Result.fail("密码错误");
        }

        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            return Result.fail("账户已被禁用，请联系管理员");
        }

        String token = JwtUtil.generateToken(dbUser.getUsername());

        dbUser.setLastLoginTime(LocalDateTime.now());
        userService.updateById(dbUser);

        return Result.success(token);
    }

    // ==================== 3. 获取当前登录用户信息 ====================

    @GetMapping("/profile")
    public Result<UserProfileVo> getProfile() {
        // 未登录会在 AuthService 中抛出 UnauthenticatedException
        User user = authService.requireLoginUser();

        Role role = null;
        if (user.getRoleId() != null) {
            role = roleService.getById(user.getRoleId());
        }

        // Dept 目前先不查，传 null 即可
        UserProfileVo vo = UserConverter.toProfileVo(user, role, null);

        return Result.success(vo);
    }


    // ==================== 4. 修改个人资料 ====================

    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody UserUpdateProfileVo vo) {
        User user = authService.requireLoginUser();

        // 把 vo 中的变动应用到当前用户
        UserConverter.applyProfileUpdate(vo, user);

        userService.updateById(user);
        return Result.success("资料更新成功");
    }


    // ==================== 5. 修改密码 ====================

    @PutMapping("/password")
    public Result<?> updatePassword(@RequestBody UserUpdatePasswordVo vo) {

        if (vo.getOldPassword() == null || vo.getOldPassword().isBlank()
                || vo.getNewPassword() == null || vo.getNewPassword().isBlank()) {
            return Result.fail("旧密码和新密码不能为空");
        }

        User user = authService.requireLoginUser();

        if (!passwordService.matches(vo.getOldPassword(), user.getPassword())) {
            return Result.fail("旧密码不正确");
        }

        if (passwordService.isWeak(vo.getNewPassword())) {
            return Result.fail("新密码太弱，至少 6 位，并包含字母和数字");
        }

        user.setPassword(passwordService.prepareForStore(vo.getNewPassword()));
        userService.updateById(user);

        return Result.success("密码修改成功");
    }
}



