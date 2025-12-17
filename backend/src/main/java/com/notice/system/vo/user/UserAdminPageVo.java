package com.notice.system.vo.user;

import lombok.Data;

/**
 * 管理端用户分页查询请求 VO
 */
@Data
public class UserAdminPageVo {

    private Long pageNo;      // 默认 1
    private Long pageSize;    // 默认 10

    private String keyword;   // 用户名 / 昵称 模糊查
    private String roleId;    // 角色筛选
    private String deptId;    // 部门筛选
    private Integer status;   // 1 启用 / 0 禁用 / null 不筛选
}
