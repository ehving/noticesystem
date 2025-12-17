package com.notice.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 多库同步日志表，用来记录每一次从某个源库 -> 其他库的同步结果
 */
@Data
@TableName("sync_log")
public class SyncLog {

    /**
     * 日志主键，使用 UUID，三库保持一致
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 实体类型：NOTICE / CATEGORY / 未来新增的实体等
     * 实际上对应 SyncEntityType.name()
     */
    private String entityType;

    /**
     * 实体主键 id（业务表的主键）
     * 建议与业务表主键类型保持一致，这里直接用 String，便于后续统一使用 UUID
     */
    private String entityId;

    /**
     * 动作：CREATE / UPDATE / DELETE
     * 对应 SyncAction.name()
     */
    private String action;

    /**
     * 源库：MYSQL / PG / SQLSERVER 中的一个
     * 对应 DatabaseType.name()
     */
    private String sourceDb;

    /**
     * 目标库：MYSQL / PG / SQLSERVER 中的一个
     * 对应 DatabaseType.name()
     */
    private String targetDb;

    /**
     * 状态：SUCCESS / FAILED / 未来可以加 PARTIAL 等
     */
    private String status;

    /**
     * 错误信息（失败时记录）
     */
    private String errorMsg;

    /**
     * 重试次数，默认 0
     */
    private Integer retryCount = 0;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}




