package com.notice.system.support.task;

import com.notice.system.common.GlobalProperties;
import com.notice.system.service.SyncLogService;
import com.notice.system.sync.DatabaseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 同步日志清理任务：
 *  - 定期清理三库中过期、过量的 sync_log 记录
 *  - 不通过 SyncService，同步日志本身不再产生新的同步日志
 * 兼容 MySQL / PostgreSQL / SQL Server：
 *  - 删除“LIMIT”语法，改用 MyBatis-Plus 的分页 selectPage 做“按数量删”。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncLogCleanTask {

    private final GlobalProperties globalProperties;
    private final SyncLogService syncLogService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanSchedule() {
        if (!globalProperties.getSync().getFull().isEnabled()) {
            // 如果你有专门的 logClean.enabled，就用那个，这里只是示意
            log.debug("[SYNC_LOG_CLEAN] 已关闭自动清理任务，跳过");
            return;
        }

        int  retainDays = 90;      // 或 globalProperties 中的配置
        long maxCount   = 100000L; // 同上

        log.info("[SYNC_LOG_CLEAN] 定时任务开始，保留天数={}，最大条数={}", retainDays, maxCount);

        long totalDeleted = 0L;
        for (DatabaseType db : DatabaseType.values()) {
            totalDeleted += syncLogService.cleanLogsInDb(db, retainDays, maxCount);
        }

        log.info("[SYNC_LOG_CLEAN] 定时任务结束，本次共删除同步日志 {} 条", totalDeleted);
    }
}


