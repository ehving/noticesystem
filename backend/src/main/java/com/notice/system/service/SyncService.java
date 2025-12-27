package com.notice.system.service;

import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncAction;
import com.notice.system.entityEnum.SyncEntityType;

import java.util.Collection;

public interface SyncService {

    /** 源库发生变更后，向其它库提交同步任务。 */
    void submitSync(SyncEntityType entityType,
                    String entityId,
                    SyncAction action,
                    DatabaseType sourceDb);

    /** 批量提交同步任务（同实体/同动作/同源库）。 */
    void submitBatchSync(SyncEntityType entityType,
                         Collection<String> entityIds,
                         SyncAction action,
                         DatabaseType sourceDb);

    /** 仅同步到指定目标库（默认会记同步日志）。 */
    boolean syncToTarget(SyncEntityType entityType,
                         String entityId,
                         SyncAction action,
                         DatabaseType sourceDb,
                         DatabaseType targetDb);

    /** 仅执行同步动作，不生成新的同步日志（用于重试/修复链路）。 */
    boolean syncToTargetWithoutLog(SyncEntityType entityType,
                                   String entityId,
                                   SyncAction action,
                                   DatabaseType sourceDb,
                                   DatabaseType targetDb);

    /** 从指定源库对某个实体类型执行全量同步。 */
    void fullSyncEntityFromSource(SyncEntityType entityType,
                                  DatabaseType sourceDb);

    /** 从指定源库对所有支持同步的实体执行全量同步。 */
    void fullSyncAllFromSource(DatabaseType sourceDb);
}




