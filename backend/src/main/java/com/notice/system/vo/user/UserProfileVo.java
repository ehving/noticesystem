package com.notice.system.vo.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileVo {

    private String id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;

    private String roleId;
    private String roleName;

    private String deptId;
    private String deptName;

    private LocalDateTime lastLoginTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}




