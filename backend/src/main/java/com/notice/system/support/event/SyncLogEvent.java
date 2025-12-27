package com.notice.system.support.event;

import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncAction;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.entityEnum.SyncLogStatus;

/** 同步完成后发布的日志事件，由日志服务监听并落库。 */
public record SyncLogEvent(
        SyncEntityType entityType,
        String entityId,
        SyncAction action,
        DatabaseType sourceDb,
        DatabaseType targetDb,
        SyncLogStatus status,
        String errorMsg,
        String conflictId
) {}



