package com.notice.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告表
 */
@Data
@TableName("notice")
public class Notice {

    /**
     * 公告主键，使用 UUID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 公告标题
     */
    private String title;

    /**
     * 公告内容（长文本）
     */
    private String content;

    /**
     * 发布人 ID（关联 user 表）
     */
    private String publisherId;

    /**
     * 公告等级：
     *  - NORMAL：普通
     *  - IMPORTANT：重要
     *  - URGENT：紧急
     */
    private String level;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 失效时间（可为空）
     */
    private LocalDateTime expireTime;

    /**
     * 公告状态：
     *  - DRAFT：草稿
     *  - PUBLISHED：已发布
     *  - RECALLED：已撤回
     */
    private String status;

    /**
     * 浏览次数（可选，用于统计）
     */
    private Long viewCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}




