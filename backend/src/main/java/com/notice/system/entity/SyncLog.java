package com.notice.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncAction;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.entityEnum.SyncLogStatus;
import lombok.Data;

import java.time.LocalDateTime;

/** 记录一次“源库 -> 目标库”的同步执行结果。 */
@Data
@TableName("sync_log")
public class SyncLog {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private SyncEntityType entityType;
    private String entityId;

    private SyncAction action;

    private DatabaseType sourceDb;
    private DatabaseType targetDb;

    private SyncLogStatus status;

    private String errorMsg;
    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}






