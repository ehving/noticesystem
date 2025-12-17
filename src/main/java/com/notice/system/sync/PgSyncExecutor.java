package com.notice.system.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 目标库为 PG 的同步执行器
 */
@Slf4j
@Component
public class PgSyncExecutor extends AbstractSyncExecutor {

    public PgSyncExecutor(SyncMetadataRegistry metadataRegistry) {
        super(DatabaseType.PG, "[PG]", metadataRegistry);
    }
}






