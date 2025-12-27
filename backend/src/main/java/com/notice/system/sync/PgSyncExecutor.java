package com.notice.system.sync;

import com.notice.system.entityEnum.DatabaseType;
import org.springframework.stereotype.Component;

/** 目标库为 PostgreSQL 的同步执行器。 */
@Component
public class PgSyncExecutor extends AbstractSyncExecutor {

    public PgSyncExecutor(SyncMetadataRegistry metadataRegistry) {
        super(DatabaseType.PG, "[PG]", metadataRegistry);
    }
}







