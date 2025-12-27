package com.notice.system.vo.user;

import com.notice.system.entityEnum.DatabaseType;
import lombok.Data;

/**
 * 管理端重置用户密码 VO
 */
@Data
public class UserAdminResetPasswordVo {

    private String newPassword;
    private DatabaseType sourceDb;
}

