package com.notice.system.vo.dept;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门树 VO
 */
@Data
public class DeptTreeVo {

    private String id;
    private String name;
    private String parentId;
    private Integer sortOrder;
    private Integer status;

    private List<DeptTreeVo> children = new ArrayList<>();
}
