package com.notice.system.sync;

import com.notice.system.entityEnum.DatabaseType;
import org.springframework.stereotype.Component;

/** 目标库为 MySQL 的同步执行器。 */
@Component
public class MysqlSyncExecutor extends AbstractSyncExecutor {

    public MysqlSyncExecutor(SyncMetadataRegistry metadataRegistry) {
        super(DatabaseType.MYSQL, "[MYSQL]", metadataRegistry);
    }
}


