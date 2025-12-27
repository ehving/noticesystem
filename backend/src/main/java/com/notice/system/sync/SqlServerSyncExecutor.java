package com.notice.system.sync;

import com.notice.system.entityEnum.DatabaseType;
import org.springframework.stereotype.Component;

/** 目标库为 SQL Server 的同步执行器。 */
@Component
public class SqlServerSyncExecutor extends AbstractSyncExecutor {

    public SqlServerSyncExecutor(SyncMetadataRegistry metadataRegistry) {
        super(DatabaseType.SQLSERVER, "[SQLSERVER]", metadataRegistry);
    }
}







