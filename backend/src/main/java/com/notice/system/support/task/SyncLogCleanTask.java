package com.notice.system.support.task;

import com.notice.system.common.GlobalProperties;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.service.SyncLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 定时清理三库 sync_log，避免日志无限膨胀。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncLogCleanTask {

    private final GlobalProperties globalProperties;
    private final SyncLogService syncLogService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanSchedule() {
        // 没有单独 logClean.enabled 时，就直接执行；后面想加配置再加
        int retainDays = 90;
        long maxCount = 100000L;

        log.info("[SYNC_LOG_CLEAN] start: retainDays={}, maxCount={}", retainDays, maxCount);

        long totalDeleted = 0L;
        for (DatabaseType db : DatabaseType.values()) {
            totalDeleted += syncLogService.cleanLogsInDb(db, retainDays, maxCount);
        }

        log.info("[SYNC_LOG_CLEAN] done: deleted={}", totalDeleted);
    }
}



