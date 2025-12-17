package com.notice.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告阅读记录表：
 *  - 记录用户何时、从什么终端阅读了哪条公告
 */
@Data
@TableName("notice_read")
public class NoticeRead {

    /**
     * 主键，使用 UUID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 公告 ID（关联 notice 表）
     */
    private String noticeId;

    /**
     * 用户 ID（关联 user 表）
     */
    private String userId;

    /**
     * 阅读时间
     */
    private LocalDateTime readTime;

    /**
     * 设备类型（可选）：
     *  - PC
     *  - MOBILE
     */
    private String deviceType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
