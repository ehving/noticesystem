package com.notice.system.vo.user;

import lombok.Data;

/**
 * 修改密码 VO
 */
@Data
public class UserUpdatePasswordVo {

    private String oldPassword;
    private String newPassword;
}

