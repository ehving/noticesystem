package com.notice.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("users")
public class User {

    /**
     * 用户主键，使用 UUID，三库保持一致
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 登录用户名（唯一）
     */
    private String username;

    /**
     * 登录密码
     *  - 当前为明文存储，后续可改为加密后存储
     */
    private String password;

    /**
     * 角色 ID（关联 role 表）
     */
    private String roleId;

    /**
     * 部门 ID（关联 dept 表，可为空）
     */
    private String deptId;

    /**
     * 昵称（用于前端展示，可为空）
     */
    private String nickname;

    /**
     * 邮箱（可为空，推荐后续加唯一约束）
     */
    private String email;

    /**
     * 手机号（可为空，推荐后续加唯一约束）
     */
    private String phone;

    /**
     * 头像 URL（可为空）
     */
    private String avatar;

    /**
     * 账户状态：1=正常，0=禁用
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginTime;

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



