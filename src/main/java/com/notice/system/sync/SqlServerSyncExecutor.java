package com.notice.system.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 目标库为 SQL Server 的同步执行器
 */
@Slf4j
@Component
public class SqlServerSyncExecutor extends AbstractSyncExecutor {

    public SqlServerSyncExecutor(SyncMetadataRegistry metadataRegistry) {
        super(DatabaseType.SQLSERVER, "[SQLSERVER]", metadataRegistry);
    }
}






