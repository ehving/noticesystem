package com.notice.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.notice.system.entityEnum.DatabaseType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sync_conflict_item")
public class SyncConflictItem {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String conflictId;
    private DatabaseType dbType;

    private Integer existsFlag;
    private String rowHash;
    private String rowVersion;
    private LocalDateTime rowUpdateTime;

    private LocalDateTime lastCheckedAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}


