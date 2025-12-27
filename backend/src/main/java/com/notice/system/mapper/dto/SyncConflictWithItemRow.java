package com.notice.system.mapper.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SyncConflictWithItemRow {

    // conflict
    private String conflictId;
    private String entityType;
    private String entityId;
    private String status; // OPEN/RESOLVED/IGNORED
    private String conflictType; // MISSING/MISMATCH
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private LocalDateTime lastCheckedAt;

    private LocalDateTime lastNotifiedAt;
    private Integer notifyCount;

    private String resolutionSourceDb;
    private String resolutionNote;
    private LocalDateTime resolvedAt;

    // item (可能为 null：还没生成 item 或 left join 没匹配)
    private String itemId;
    private String dbType;       // MYSQL/PG/SQLSERVER
    private Integer existsFlag;  // 0/1
    private String rowHash;
    private String rowVersion;
    private LocalDateTime rowUpdateTime;
    private LocalDateTime itemLastCheckedAt;
}



