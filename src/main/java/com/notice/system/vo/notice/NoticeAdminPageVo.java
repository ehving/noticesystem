package com.notice.system.vo.notice;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端公告分页查询条件 VO
 */
@Data
public class NoticeAdminPageVo {

    private long pageNo = 1;
    private long pageSize = 10;

    /**
     * 标题 / 内容关键字
     */
    private String keyword;

    /**
     * 公告状态：DRAFT / PUBLISHED / RECALLED
     */
    private String status;

    /**
     * 公告等级：NORMAL / IMPORTANT / URGENT
     */
    private String level;

    /**
     * 发布人 ID（可选）
     */
    private String publisherId;

    /**
     * 发布时间范围
     */
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
