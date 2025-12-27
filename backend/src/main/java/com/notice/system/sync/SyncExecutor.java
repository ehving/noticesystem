package com.notice.system.sync;

import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncAction;
import com.notice.system.entityEnum.SyncEntityType;

public interface SyncExecutor {

    /** 返回当前执行器负责写入的目标库。 */
    DatabaseType targetDb();

    /** 从源库读取指定实体并将变更应用到目标库。 */
    void applyOne(SyncEntityType entityType,
                  SyncAction action,
                  String entityId,
                  DatabaseType sourceDb);
}





