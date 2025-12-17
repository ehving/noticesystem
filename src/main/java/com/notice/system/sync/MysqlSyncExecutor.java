package com.notice.system.sync;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 目标库为 MySQL 的同步执行器
 */
@Slf4j
@Component
public class MysqlSyncExecutor extends AbstractSyncExecutor {

    public MysqlSyncExecutor(SyncMetadataRegistry metadataRegistry) {
        super(DatabaseType.MYSQL, "[MYSQL]", metadataRegistry);
    }
}

