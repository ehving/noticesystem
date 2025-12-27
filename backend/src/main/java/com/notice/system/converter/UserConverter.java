package com.notice.system.converter;

import com.notice.system.entity.Dept;
import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.vo.user.*;

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

        user.setRoleId(trimOrNull(roleId));
        user.setDeptId(trimOrNull(vo.getDeptId()));

        user.setNickname(trimOrNull(vo.getNickname()));
        user.setEmail(trimOrNull(vo.getEmail()));
        user.setPhone(trimOrNull(vo.getPhone()));

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
     * <p>
     * 约定：
     * - 前端传 "" / "   " 视为“清空字段”，落库为 null
     */
    public static void applyProfileUpdate(UserUpdateProfileVo vo, User user) {
        if (vo == null || user == null) {
            return;
        }
        // 允许清空：把空串归一化为 null
        user.setNickname(trimOrNull(vo.getNickname()));
        user.setEmail(trimOrNull(vo.getEmail()));
        user.setPhone(trimOrNull(vo.getPhone()));
        user.setAvatar(trimOrNull(vo.getAvatar()));
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
        user.setPassword(rawPassword); // 目前未加密，后续可外层统一加密

        user.setNickname(trimOrNull(vo.getNickname()));
        user.setEmail(trimOrNull(vo.getEmail()));
        user.setPhone(trimOrNull(vo.getPhone()));
        user.setAvatar(trimOrNull(vo.getAvatar()));

        user.setRoleId(trimOrNull(vo.getRoleId()));
        user.setDeptId(trimOrNull(vo.getDeptId()));

        user.setStatus(vo.getStatus() == null ? 1 : vo.getStatus());
        return user;
    }

    /**
     * 管理端更新用户（包括角色 / 部门 / 状态）
     * <p>
     * 约定：
     * - 字段为 null：表示“不修改”
     * - 字段为 "" / "   "：表示“清空”，落库为 null
     */
    public static void applyAdminUpdate(UserAdminUpdateVo vo, User user) {
        if (vo == null || user == null) {
            return;
        }

        if (vo.getNickname() != null) {
            user.setNickname(trimOrNull(vo.getNickname()));
        }
        if (vo.getEmail() != null) {
            user.setEmail(trimOrNull(vo.getEmail()));
        }
        if (vo.getPhone() != null) {
            user.setPhone(trimOrNull(vo.getPhone()));
        }
        if (vo.getAvatar() != null) {
            user.setAvatar(trimOrNull(vo.getAvatar()));
        }

        if (vo.getRoleId() != null) {
            user.setRoleId(trimOrNull(vo.getRoleId()));
        }
        if (vo.getDeptId() != null) {
            user.setDeptId(trimOrNull(vo.getDeptId()));
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

    /**
     * 统一规则：null -> null；"   " -> null；"abc " -> "abc"
     */
    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * 仅 trim，不做空串转 null（用于 username 这种必填字段）
     */
    private static String trim(String s) {
        return (s == null ? null : s.trim());
    }
}
