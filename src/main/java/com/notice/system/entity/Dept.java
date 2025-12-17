package com.notice.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 部门表
 */
@Data
@TableName("dept")
public class Dept {

    /**
     * 部门主键，使用 UUID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 部门名称
     */
    private String name;

    /**
     * 上级部门 ID（根部门可为空）
     */
    private String parentId;

    /**
     * 部门描述 / 备注
     */
    private String description;

    /**
     * 排序字段（数值越小越靠前）
     */
    private Integer sortOrder;

    /**
     * 部门状态：1=启用，0=停用
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

