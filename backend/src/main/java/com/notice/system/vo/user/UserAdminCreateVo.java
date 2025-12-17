package com.notice.system.vo.user;

import com.notice.system.sync.DatabaseType;
import lombok.Data;

/**
 * 管理端创建用户 VO
 */
@Data
public class UserAdminCreateVo {

    private String username;
    private String password;   // 初始密码（目前明文）

    private String nickname;
    private String email;
    private String phone;
    private String avatar;

    private String roleId;
    private String deptId;

    private Integer status;    // 可选，null 时后端默认 1

    // 源库（如果不传，后端用默认库）
    private DatabaseType sourceDb;
}

