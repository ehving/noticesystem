package com.notice.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("role")
public class Role {

    /**
     * 角色主键，统一使用 UUID
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 角色名称，例如：管理员、普通用户
     */
    private String name;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


}

