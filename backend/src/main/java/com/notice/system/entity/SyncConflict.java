package com.notice.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.notice.system.entityEnum.ConflictStatus;
import com.notice.system.entityEnum.ConflictType;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sync_conflict")
public class SyncConflict {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField(value = "entity_type")
    private SyncEntityType entityType;

    @TableField(value = "entity_id")
    private String entityId;

    private ConflictStatus status;
    private ConflictType conflictType;

    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime lastCheckedAt;

    private LocalDateTime lastNotifiedAt;
    private Integer notifyCount;

    private DatabaseType resolutionSourceDb;
    private String resolutionNote;
    private LocalDateTime resolvedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}


