package com.notice.system.vo.conflict;

import com.notice.system.entityEnum.ConflictStatus;
import com.notice.system.entityEnum.ConflictType;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SyncConflictWithItemsVo {

    private String id;

    private SyncEntityType entityType;
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

    private List<SyncConflictItemVo> items = new ArrayList<>();
}

