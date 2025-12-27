package com.notice.system.support.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.common.GlobalProperties;
import com.notice.system.entity.SyncLog;
import com.notice.system.service.SyncLogService;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.entityEnum.SyncLogStatus;
import com.notice.system.sync.SyncMetadataRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 同步失败自动重试定时任务：
 *  - 只扫描 FAILED（可重试）状态的 sync_log
 *  - ERROR / CONFLICT 不重试（分别代表：不可恢复错误 / 工单冲突）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncRetryTask {

    private final SyncMetadataRegistry metadataRegistry;
    private final SyncLogService syncLogService;
    private final GlobalProperties globalProperties;

    /**
     * 每 fixedDelayMs 毫秒执行一次重试任务
     * 时间由 application.yml -> notice.sync.retry.fixed-delay-ms 控制
     */
    @Scheduled(fixedDelayString = "${notice.sync.retry.fixed-delay-ms:60000}")
    public void retryFailed() {
        GlobalProperties.Sync.Retry retryCfg = globalProperties.getSync().getRetry();
        if (retryCfg == null || !retryCfg.isEnabled()) {
            return;
        }

        int maxRetry = retryCfg.getMaxRetryCount();
        if (maxRetry <= 0) maxRetry = 3;

        // 从 SyncMetadataRegistry 获取 SYNC_LOG 在各库的 Mapper
        SyncMetadataRegistry.EntitySyncDefinition<SyncLog> def =
                metadataRegistry.getDefinition(SyncEntityType.SYNC_LOG);
        if (def == null) {
            log.warn("[SYNC-RETRY] 未在 SyncMetadataRegistry 中找到 SYNC_LOG 定义");
            return;
        }

        long totalCount = 0L;

        // 你现在日志默认落 MYSQL（并同步到其他库）；为了避免重复重试，任务只扫 MYSQL 即可
        for (DatabaseType db : new DatabaseType[]{DatabaseType.MYSQL}) {
            BaseMapper<SyncLog> mapper = def.getMapper(db);
            if (mapper == null) {
                log.warn("[SYNC-RETRY] 未找到 {} 库的 SyncLog Mapper，跳过", db);
                continue;
            }

            List<SyncLog> failedLogs = mapper.selectList(
                    new LambdaQueryWrapper<SyncLog>()
                            .eq(SyncLog::getStatus, SyncLogStatus.FAILED)
                            .lt(SyncLog::getRetryCount, maxRetry)
                            .orderByAsc(SyncLog::getCreateTime) // 可选：先重试最早的
            );

            if (failedLogs == null || failedLogs.isEmpty()) {
                continue;
            }

            log.info("[SYNC-RETRY] {} 库需要重试的 FAILED 记录数量={}", db, failedLogs.size());
            totalCount += failedLogs.size();

            for (SyncLog logRecord : failedLogs) {
                boolean ok = syncLogService.retrySyncByLogId(db, logRecord.getId());
                if (!ok) {
                    log.debug("[SYNC-RETRY] 单条重试失败或跳过，db={}，logId={}", db, logRecord.getId());
                }
            }
        }

        if (totalCount > 0) {
            log.info("[SYNC-RETRY] 本轮重试任务结束，共尝试重试 {} 条 FAILED 记录", totalCount);
        }
    }
}







