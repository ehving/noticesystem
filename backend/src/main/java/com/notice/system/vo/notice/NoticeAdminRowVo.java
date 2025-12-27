package com.notice.system.vo.notice;

import com.notice.system.entity.Notice;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 管理端公告分页返回行：Notice + 可见范围摘要
 */
@Data
public class NoticeAdminRowVo {

    private Notice notice;

    /**
     * GLOBAL: notice_target_dept 没有记录（全员可见）
     * DEPT  : notice_target_dept 有记录（定向部门）
     */
    private String scopeType; // "GLOBAL" | "DEPT"

    /**
     * 定向部门数量（GLOBAL=0）
     */
    private int targetDeptCount;

    /**
     * 定向部门名称预览（最多 N 个）
     */
    private List<String> targetDeptNamesPreview = Collections.emptyList();

    /**
     * 定向部门 IDs（给前端弹窗“编辑范围”直接用）
     */
    private List<String> targetDeptIds = Collections.emptyList();
}

