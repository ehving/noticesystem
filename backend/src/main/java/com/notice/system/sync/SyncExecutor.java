package com.notice.system.sync;

public interface SyncExecutor {

    /**
     * 该执行器对应的目标库
     */
    DatabaseType targetDb();

    /**
     * 同步一条数据：
     *  - 从 sourceDb 读取
     *  - 写入到 targetDb（由实现类固定）
     */
    void applyOne(SyncEntityType entityType,
                  SyncAction action,
                  String entityId,
                  DatabaseType sourceDb);
}




