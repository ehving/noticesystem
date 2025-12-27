package com.notice.system.service;

import com.notice.system.entity.SyncConflictItem;
import com.notice.system.service.base.MultiDbSyncService;
import com.notice.system.entityEnum.DatabaseType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SyncConflictItemService extends MultiDbSyncService<SyncConflictItem> {

    /**
     * 刷新某个 conflict 的三库快照 items（存在性、hash、检查时间）
     * 写入必须走 saveInDb / updateByIdInDb 以触发同步
     */
    void upsertSnapshotItems(String conflictId,
                             Map<DatabaseType, Integer> exists,
                             Map<DatabaseType, String> hash,
                             LocalDateTime now);

    List<SyncConflictItem> listByConflictId(DatabaseType db, String conflictId);

    Map<DatabaseType, SyncConflictItem> mapByConflictId(DatabaseType db, String conflictId);

}