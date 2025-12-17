package com.notice.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告-部门关联表：
 *  - 一条公告可以发送给多个部门
 */
@Data
@TableName("notice_target_dept")
public class NoticeTargetDept {

    /**
     * 关联主键，使用 UUID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 公告 ID（关联 notice 表）
     */
    private String noticeId;

    /**
     * 部门 ID（关联 dept 表）
     */
    private String deptId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
