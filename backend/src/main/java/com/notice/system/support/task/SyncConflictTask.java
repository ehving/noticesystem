package com.notice.system.support.task;

import com.notice.system.common.GlobalProperties;
import com.notice.system.service.SyncConflictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncConflictTask {

    private final SyncConflictService syncConflictService;
    private final GlobalProperties globalProperties;

    // 扫 SUCCESS 日志的游标：单机版够用
    private volatile LocalDateTime lastSuccessLogScanAt = LocalDateTime.now().minusDays(30);

    @Scheduled(fixedDelayString = "${notice.sync.conflict.job-fixed-delay-ms:300000}")
    public void runConflictJob() {
        log.info("[SyncConflictTask] 冲突检测Task开始");
        GlobalProperties.Sync.Conflict cfg = globalProperties.getSync().getConflict();
        if (cfg == null || !cfg.isEnabled()) return;

        LocalDateTime now = LocalDateTime.now();

        // 这些也可以放 cfg；课设就先写死也行
        int detectPerDbLimit = 200;
        int detectEntityLimit = 200;

        int recheckLimit = cfg.getRecheckLimit() <= 0 ? 50 : cfg.getRecheckLimit();
        int notifyLimit  = cfg.getNotifyLimit()  <= 0 ? 20 : cfg.getNotifyLimit();

        // ====== A 先扫描 SUCCESS 日志发现新冲突 ======
        LocalDateTime from = lastSuccessLogScanAt;
        lastSuccessLogScanAt = now;

        int found;
        try {
            found = syncConflictService.detectNewConflictsFromSuccessLogs(
                    from, now, detectPerDbLimit, detectEntityLimit
            );
            if (found > 0) {
                log.info("[CONFLICT-JOB] detectNewFromSuccessLogs done, from={}, to={}, found={}",
                        from, now, found);
            }
        } catch (Exception e) {
            log.warn("[CONFLICT-JOB] detectNewFromSuccessLogs failed, from={}, to={}, err={}",
                    from, now, e.getMessage(), e);
        }

        // ====== B 再重检 OPEN 冲突（刷新 items/自动结单/重开） ======
        try {
            int n = syncConflictService.recheckOpenConflicts(now, recheckLimit);
            if (n > 0) log.info("[CONFLICT-JOB] recheckOpen done, count={}", n);
        } catch (Exception e) {
            log.warn("[CONFLICT-JOB] recheckOpen failed, err={}", e.getMessage(), e);
        }

        // ====== C 最后发通知（SQL 冷却保证不会刷屏） ======
        try {
            int sent = syncConflictService.notifyPendingConflicts(now, notifyLimit);
            if (sent > 0) log.info("[CONFLICT-JOB] notifyPending done, sent={}", sent);
        } catch (Exception e) {
            log.warn("[CONFLICT-JOB] notifyPending failed, err={}", e.getMessage(), e);
        }
        log.info("[SyncConflictTask] 冲突检测Task结束");
    }
}


