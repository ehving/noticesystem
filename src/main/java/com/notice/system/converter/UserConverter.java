package com.notice.system.converter;

import com.notice.system.entity.Dept;
import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.vo.user.*;

import java.time.LocalDateTime;

public class UserConverter {

    private UserConverter() {}
    /* ========== 1. 注册相关 ========== */

    /**
     * 注册时，将 RegisterVo + 原始密码 + 角色ID 转为 User 实体
     */
    public static User toEntityForRegister(UserRegisterVo vo, String rawPassword, String roleId) {
        if (vo == null) {
            return null;
        }
        User user = new User();
        user.setUsername(trim(vo.getUsername()));
        user.setPassword(rawPassword);   // 目前未加密，后续可以在外层先做 encode 再传进来
        user.setRoleId(roleId);
        user.setDeptId(vo.getDeptId());
        user.setNickname(vo.getNickname());
        user.setEmail(vo.getEmail());
        user.setPhone(vo.getPhone());
        user.setStatus(1);               // 默认启用
        return user;
    }

    /* ========== 2. 个人资料相关 ========== */

    /**
     * 构造个人资料视图对象（供 /api/user/profile 使用）
     */
    public static UserProfileVo toProfileVo(User user, Role role, Dept dept) {
        if (user == null) {
            return null;
        }
        UserProfileVo vo = new UserProfileVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());

        vo.setRoleId(user.getRoleId());
        vo.setRoleName(role != null ? role.getName() : null);

        vo.setDeptId(user.getDeptId());
        vo.setDeptName(dept != null ? dept.getName() : null);

        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }

    /**
     * 将 UserUpdateProfileVo 中的变更应用到 User（当前登录用户修改个人资料）
     */
    public static void applyProfileUpdate(UserUpdateProfileVo vo, User user) {
        if (vo == null || user == null) {
            return;
        }
        user.setNickname(vo.getNickname());
        user.setEmail(vo.getEmail());
        user.setPhone(vo.getPhone());
        user.setAvatar(vo.getAvatar());
    }

    /* ========== 3. 管理端创建 / 更新 / 列表 ========== */

    /**
     * 管理员创建用户时，将 VO 转为 User 实体
     */
    public static User toEntityForAdminCreate(UserAdminCreateVo vo, String rawPassword) {
        if (vo == null) {
            return null;
        }
        User user = new User();
        user.setUsername(trim(vo.getUsername()));
        user.setPassword(rawPassword);             // 目前未加密，后续可外层统一加密
        user.setNickname(vo.getNickname());
        user.setEmail(vo.getEmail());
        user.setPhone(vo.getPhone());
        user.setAvatar(vo.getAvatar());

        user.setRoleId(vo.getRoleId());
        user.setDeptId(vo.getDeptId());

        user.setStatus(vo.getStatus() == null ? 1 : vo.getStatus());
        return user;
    }

    /**
     * 管理端更新用户（包括角色 / 部门 / 状态）
     */
    public static void applyAdminUpdate(UserAdminUpdateVo vo, User user) {
        if (vo == null || user == null) {
            return;
        }

        if (vo.getNickname() != null) {
            user.setNickname(vo.getNickname());
        }
        if (vo.getEmail() != null) {
            user.setEmail(vo.getEmail());
        }
        if (vo.getPhone() != null) {
            user.setPhone(vo.getPhone());
        }
        if (vo.getAvatar() != null) {
            user.setAvatar(vo.getAvatar());
        }

        if (vo.getRoleId() != null) {
            user.setRoleId(vo.getRoleId());
        }
        if (vo.getDeptId() != null) {
            user.setDeptId(vo.getDeptId());
        }
        if (vo.getStatus() != null) {
            user.setStatus(vo.getStatus());
        }
    }

    /**
     * 管理端用户列表行 VO
     */
    public static UserAdminListVo toAdminListVo(User user, Role role, Dept dept) {
        if (user == null) {
            return null;
        }
        UserAdminListVo vo = new UserAdminListVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());

        vo.setRoleId(user.getRoleId());
        vo.setRoleName(role != null ? role.getName() : null);

        vo.setDeptId(user.getDeptId());
        vo.setDeptName(dept != null ? dept.getName() : null);

        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /* ================== 工具 ================== */

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String trim(String s) {
        return (s == null ? null : s.trim());
    }
}

