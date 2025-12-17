package com.notice.system.vo.user;

import com.notice.system.sync.DatabaseType;
import lombok.Data;

/**
 * 管理端更新用户信息 VO
 */
@Data
public class UserAdminUpdateVo {

    private String nickname;
    private String email;
    private String phone;
    private String avatar;

    private String roleId;
    private String deptId;

    private Integer status;     // 启用 / 禁用

    private DatabaseType sourceDb;
}

