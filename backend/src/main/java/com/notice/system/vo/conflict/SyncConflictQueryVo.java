package com.notice.system.vo.conflict;

import com.notice.system.entityEnum.ConflictStatus;
import com.notice.system.entityEnum.ConflictType;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SyncConflictQueryVo {

    private Long pageNo = 1L;
    private Long pageSize = 10L;

    private LocalDateTime beginTime;
    private LocalDateTime endTime;

    private SyncEntityType entityType;

    /**
     * OPEN / RESOLVED / IGNORED（String 更兼容前端）
     */
    private ConflictStatus status;

    /**
     * MISSING / MISMATCH（String 更兼容前端）
     */
    private ConflictType conflictType;

    /**
     * 过滤：items 中是否存在某 dbType
     */
    private DatabaseType sourceDb;

    /**
     * 只看 OPEN（可空）
     */
    private Boolean limitToOpenOnly;
}




