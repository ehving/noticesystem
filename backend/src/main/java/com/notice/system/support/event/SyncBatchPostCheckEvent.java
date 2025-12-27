package com.notice.system.support.event;

import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncAction;
import com.notice.system.entityEnum.SyncEntityType;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
public class SyncBatchPostCheckEvent {

    private final SyncEntityType entityType;
    private final String entityId;
    private final SyncAction action;
    private final DatabaseType sourceDb;

    /**
     * 本次 submitSync 实际执行过的 targetDb 及其 apply 是否成功
     */
    private final Map<DatabaseType, Boolean> targetApplyOk;

    /**
     * 监听器回填：若发现冲突则给出 conflictId
     */
    @Setter
    private String conflictId;

    public SyncBatchPostCheckEvent(SyncEntityType entityType,
                                   String entityId,
                                   SyncAction action,
                                   DatabaseType sourceDb,
                                   Map<DatabaseType, Boolean> targetApplyOk) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.sourceDb = sourceDb;
        this.targetApplyOk = targetApplyOk;
    }
}

