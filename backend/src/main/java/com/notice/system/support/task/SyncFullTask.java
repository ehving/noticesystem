package com.notice.system.support.task;

import com.notice.system.common.GlobalProperties;
import com.notice.system.service.SyncService;
import com.notice.system.sync.DatabaseType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 全量同步定时任务：
 *  - 定期从某个源库对所有实体执行全量同步
 *  - 实现“每日/每周全库校验纠偏”的效果
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncFullTask {

    private final SyncService syncService;
    private final GlobalProperties globalProperties;

    /**
     * 定时全量同步的 Cron 从配置中读取：
     *  - notice.sync.full.cron
     *  - 默认 "0 0 3 * * ?"（每天凌晨 3 点）
     */
    @Scheduled(cron = "${notice.sync.full.cron:0 0 3 * * ?}")
    public void fullSyncAll() {
        GlobalProperties.Sync.Full fullCfg = globalProperties.getSync().getFull();
        if (!fullCfg.isEnabled()) {
            // 配置关闭就不做任何事
            return;
        }

        DatabaseType sourceDb = fullCfg.getSourceDb();
        if (sourceDb == null) {
            sourceDb = DatabaseType.MYSQL;
        }

        log.info("[SYNC-FULL] 定时全量同步开始，源库={}", sourceDb);
        syncService.fullSyncAllFromSource(sourceDb);
        log.info("[SYNC-FULL] 定时全量同步结束，源库={}", sourceDb);
    }
}

