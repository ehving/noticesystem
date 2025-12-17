package com.notice.system.vo.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户列表行 VO
 */
@Data
public class UserAdminListVo {

    private String id;
    private String username;
    private String nickname;
    private String email;
    private String phone;

    private Integer status;

    private String roleId;
    private String roleName;

    private String deptId;
    private String deptName;

    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
}

