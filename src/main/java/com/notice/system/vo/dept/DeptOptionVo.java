package com.notice.system.vo.dept;

import lombok.Data;

/**
 * 部门下拉选项 VO（前台：选择部门用）
 */
@Data
public class DeptOptionVo {

    private String id;
    private String name;
    private String parentId;
}


