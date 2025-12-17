package com.notice.system.vo.user;

import lombok.Data;

/**
 * 更新个人资料 VO
 *  - 只允许修改部分字段（不允许直接改角色/部门等敏感信息）
 */
@Data
public class UserUpdateProfileVo {

    private String nickname;
    private String email;
    private String phone;
    private String avatar;
}


