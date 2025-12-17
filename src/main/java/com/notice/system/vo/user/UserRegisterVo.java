package com.notice.system.vo.user;

import lombok.Data;

/**
 * 用户注册请求 VO
 */
@Data
public class UserRegisterVo {

    private String username;
    private String password;
    private String deptId;
    private String nickname;
    private String email;
    private String phone;
}



