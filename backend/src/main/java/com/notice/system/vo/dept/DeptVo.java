package com.notice.system.vo.dept;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门详情 / 列表行 VO
 */
@Data
public class DeptVo {

    private String id;
    private String name;
    private String parentId;
    private String parentName;

    private String description;
    private Integer sortOrder;
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

