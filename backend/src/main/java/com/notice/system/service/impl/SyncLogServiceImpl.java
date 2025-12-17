package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.SyncLog;
import com.notice.system.support.event.SyncLogEvent;
import com.notice.system.mapper.base.SyncLogBaseMapper;
import com.notice.system.mapper.dto.SyncLogDailyStat;
import com.notice.system.service.MailService;
import com.notice.system.service.SyncLogService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncAction;
import com.notice.system.sync.SyncEntityType;
import com.notice.system.sync.SyncMetadataRegistry;
import com.notice.system.vo.synclog.SyncLogDailyReportVo;
import com.notice.system.vo.synclog.SyncLogVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SyncLogServiceImpl
        extends MultiDbSyncServiceImpl<SyncLog>
        implements SyncLogService {

    private final MailService mailService;

    public SyncLogServiceImpl(SyncService syncService,
                              SyncMetadataRegistry metadataRegistry,MailService mailService) {
        // 默认把 MySQL 作为日志的源库
        super(syncService, metadataRegistry, SyncEntityType.SYNC_LOG, DatabaseType.MYSQL);
        this.mailService = mailService;
    }

    /* ========== 事件监听：真正落库逻辑 ========== */

    @EventListener
    public void handleSyncLogEvent(SyncLogEvent event) {
        if (event.isSuccess()) {
            recordSuccess(
                    event.getEntityType(),
                    event.getEntityId(),
                    event.getAction(),
                    event.getSourceDb(),
                    event.getTargetDb()
            );
        } else {
            recordFailure(
                    event.getEntityType(),
                    event.getEntityId(),
                    event.getAction(),
                    event.getSourceDb(),
                    event.getTargetDb(),
                    event.getErrorMsg()
            );
        }
    }

    /* ========== 记录日志 ========== */

    @Override
    public void recordSuccess(SyncEntityType entityType,
                              String entityId,
                              SyncAction action,
                              DatabaseType sourceDb,
                              DatabaseType targetDb) {
        SyncLog logRecord = buildBaseLog(entityType, entityId, action, sourceDb, targetDb);
        logRecord.setStatus("SUCCESS");
        logRecord.setErrorMsg(null);
        logRecord.setRetryCount(0);
        // 写入默认库（MySQL），MultiDbSyncServiceImpl 会以 MYSQL 为源库触发同步
        this.save(logRecord);
    }

    @Override
    public void recordFailure(SyncEntityType entityType,
                              String entityId,
                              SyncAction action,
                              DatabaseType sourceDb,
                              DatabaseType targetDb,
                              String errorMsg) {
        SyncLog logRecord = buildBaseLog(entityType, entityId, action, sourceDb, targetDb);
        logRecord.setStatus("FAILED");
        logRecord.setErrorMsg(errorMsg);
        logRecord.setRetryCount(0);

        // 1. 保存日志（默认写到 MYSQL，并通过同步机制推给其它库）
        this.save(logRecord);

        // 2. 只有“疑似数据冲突”的错误才发邮件
        if (isConflictError(action, errorMsg)) {
            try {
                mailService.sendSyncConflictAlert(logRecord);
            } catch (Exception e) {
                log.warn("[SYNC-LOG] 发送冲突告警邮件失败，logId={}，错误={}",
                        logRecord.getId(), e.getMessage(), e);
            }
        }
    }

    private SyncLog buildBaseLog(SyncEntityType entityType,
                                 String entityId,
                                 SyncAction action,
                                 DatabaseType sourceDb,
                                 DatabaseType targetDb) {
        SyncLog logRecord = new SyncLog();
        logRecord.setEntityType(entityType.name());
        logRecord.setEntityId(entityId);
        logRecord.setAction(action.name());
        logRecord.setSourceDb(sourceDb.name());
        logRecord.setTargetDb(targetDb.name());
        LocalDateTime now = LocalDateTime.now();
        logRecord.setCreateTime(now);
        logRecord.setUpdateTime(now);
        return logRecord;
    }

    /* ========== 分页 / 查询 / 重试计数 ========== */

    @Override
    public Page<SyncLog> pageLogs(DatabaseType logDb, SyncLogVo vo) {
        DatabaseType db = (logDb == null ? defaultDb() : logDb);
        BaseMapper<SyncLog> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[SYNC-LOG] pageLogs 时未找到 mapper，db={}", db);
            return new Page<>(vo.getPageNo(), vo.getPageSize());
        }

        Page<SyncLog> page = new Page<>(vo.getPageNo(), vo.getPageSize());
        LambdaQueryWrapper<SyncLog> wrapper = new LambdaQueryWrapper<>();

        if (vo.getEntityType() != null) {
            wrapper.eq(SyncLog::getEntityType, vo.getEntityType().name());
        }
        if (vo.getEntityId() != null) {
            wrapper.eq(SyncLog::getEntityId, vo.getEntityId());
        }
        if (vo.getAction() != null) {
            wrapper.eq(SyncLog::getAction, vo.getAction().name());
        }
        if (vo.getSourceDb() != null) {
            wrapper.eq(SyncLog::getSourceDb, vo.getSourceDb().name());
        }
        if (vo.getTargetDb() != null) {
            wrapper.eq(SyncLog::getTargetDb, vo.getTargetDb().name());
        }
        if (vo.getStatus() != null) {
            wrapper.eq(SyncLog::getStatus, vo.getStatus());
        }
        if (vo.getBeginTime() != null) {
            wrapper.ge(SyncLog::getCreateTime, vo.getBeginTime());
        }
        if (vo.getEndTime() != null) {
            wrapper.le(SyncLog::getCreateTime, vo.getEndTime());
        }

        wrapper.orderByDesc(SyncLog::getCreateTime);

        return mapper.selectPage(page, wrapper);
    }

    @Override
    public SyncLog getById(DatabaseType logDb, String id) {
        DatabaseType db = (logDb == null ? defaultDb() : logDb);
        return super.getById(db, id);
    }

    @Override
    public void incrementRetryCount(DatabaseType logDb, String logId) {
        if (logId == null) {
            return;
        }
        DatabaseType db = (logDb == null ? defaultDb() : logDb);

        SyncLog logRecord = this.getById(db, logId);
        if (logRecord == null) {
            return;
        }

        Integer oldCount = logRecord.getRetryCount();
        if (oldCount == null) {
            oldCount = 0;
        }
        logRecord.setRetryCount(oldCount + 1);
        logRecord.setUpdateTime(LocalDateTime.now());

        // 在指定库更新，并以该库为源库触发同步
        this.updateByIdInDb(db, logRecord);
    }

    @Override
    public boolean retrySyncByLogId(DatabaseType logDb, String logId) {
        if (logId == null || logId.isBlank()) {
            return false;
        }

        DatabaseType db = (logDb == null ? defaultDb() : logDb);

        // 1. 先从指定日志库查出这条日志
        SyncLog logRecord = this.getById(db, logId);
        if (logRecord == null) {
            log.warn("[SYNC-LOG] retrySyncByLogId 失败，日志不存在，logDb={}，logId={}", db, logId);
            return false;
        }

        int oldRetry = (logRecord.getRetryCount() == null ? 0 : logRecord.getRetryCount());

        try {
            // 2. 从日志记录中还原出同步的关键信息
            SyncEntityType entityType = SyncEntityType.valueOf(logRecord.getEntityType());
            SyncAction action = SyncAction.valueOf(logRecord.getAction());
            DatabaseType sourceDb = DatabaseType.valueOf(logRecord.getSourceDb());
            DatabaseType targetDb = DatabaseType.valueOf(logRecord.getTargetDb());
            String entityId = logRecord.getEntityId();

            if (entityId == null || entityId.isBlank()) {
                log.warn("[SYNC-LOG] retrySyncByLogId 失败，日志缺少 entityId，logId={}", logId);
                return false;
            }

            // 3. 针对这条日志记录的来源/目标库，重试一次同步
            boolean ok = syncService.syncToTargetWithoutLog(entityType, entityId, action, sourceDb, targetDb);

            // 4. 更新日志的状态 / 重试次数 / 时间 / 错误信息
            logRecord.setRetryCount(oldRetry + 1);
            logRecord.setUpdateTime(LocalDateTime.now());

            if (ok) {
                logRecord.setStatus("SUCCESS");
                logRecord.setErrorMsg(null);
                log.info("[SYNC-LOG] retrySyncByLogId 成功，logDb={}，logId={}，{} {} {} -> {}",
                        db, logId, entityType, action, sourceDb, targetDb);
            } else {
                logRecord.setStatus("FAILED");
                logRecord.setErrorMsg("重试同步仍失败，请查看最近失败日志详情");
                log.warn("[SYNC-LOG] retrySyncByLogId 失败（syncService 返回 false），logDb={}，logId={}，{} {} {} -> {}",
                        db, logId, entityType, action, sourceDb, targetDb);
            }

            // 使用带选库的更新，按该日志库为源库同步
            this.updateByIdInDb(db, logRecord);

            return ok;
        } catch (Exception e) {
            // 枚举解析 / syncToTarget 抛异常也要更新日志
            logRecord.setRetryCount(oldRetry + 1);
            logRecord.setStatus("FAILED");
            logRecord.setErrorMsg(e.getMessage());
            logRecord.setUpdateTime(LocalDateTime.now());
            this.updateByIdInDb(db, logRecord);

            log.warn("[SYNC-LOG] retrySyncByLogId 异常，logDb={}，logId={}，错误={}",
                    db, logId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<SyncLogDailyReportVo> listDailyReport(DatabaseType logDb, SyncLogVo condition) {
        DatabaseType db = (logDb == null ? defaultDb() : logDb);
        SyncLogVo vo = (condition == null ? new SyncLogVo() : condition);

        SyncMetadataRegistry.EntitySyncDefinition<SyncLog> def =
                metadataRegistry.getDefinition(SyncEntityType.SYNC_LOG);
        if (def == null) {
            log.warn("[SYNC-LOG] listDailyReport 时未在 SyncMetadataRegistry 中找到 SYNC_LOG 定义");
            return java.util.Collections.emptyList();
        }

        BaseMapper<SyncLog> baseMapper = def.getMapper(db);
        if (!(baseMapper instanceof SyncLogBaseMapper syncLogMapper)) {
            log.warn("[SYNC-LOG] listDailyReport 时 Mapper 未实现 SyncLogBaseMapper，db={}", db);
            return java.util.Collections.emptyList();
        }

        // ====== 关键：把枚举转换成 String 传给 Mapper ======
        String entityTypeStr = (vo.getEntityType() == null ? null : vo.getEntityType().name());
        String actionStr     = (vo.getAction()     == null ? null : vo.getAction().name());
        String statusStr     = vo.getStatus();  // 你之前就是 String，这里直接用

        String sourceDbStr   = (vo.getSourceDb() == null ? null : vo.getSourceDb().name());
        String targetDbStr   = (vo.getTargetDb() == null ? null : vo.getTargetDb().name());

        List<SyncLogDailyStat> stats = syncLogMapper.listDailyStats(
                vo.getBeginTime(),
                vo.getEndTime(),
                entityTypeStr,
                actionStr,
                statusStr,
                sourceDbStr,
                targetDbStr
        );

        // ====== 转成返回给前端的 VO ======
        List<SyncLogDailyReportVo> result = new java.util.ArrayList<>();
        if (stats != null) {
            for (SyncLogDailyStat stat : stats) {
                if (stat == null) continue;

                SyncLogDailyReportVo r = new SyncLogDailyReportVo();
                r.setStatDate(stat.getStatDate() != null ? stat.getStatDate().toString() : null);
                r.setSourceDb(stat.getSourceDb());
                r.setTargetDb(stat.getTargetDb());
                r.setTotalCount(stat.getTotalCount());
                r.setSuccessCount(stat.getSuccessCount());
                r.setFailedCount(stat.getFailedCount());

                long total  = stat.getTotalCount()  == null ? 0L : stat.getTotalCount();
                long failed = stat.getFailedCount() == null ? 0L : stat.getFailedCount();
                double rate = (total == 0L ? 0.0 : (failed * 1.0 / total));
                r.setFailedRate(rate);

                result.add(r);
            }
        }

        return result;
    }

    @Override
    public long cleanLogsInDb(DatabaseType db, Integer retainDays, Long maxCount) {
        DatabaseType useDb = (db == null ? DatabaseType.MYSQL : db);

        int  days = (retainDays == null || retainDays <= 0) ? 90     : retainDays;
        long max  = (maxCount   == null || maxCount   <= 0) ? 100000L: maxCount;

        SyncMetadataRegistry.EntitySyncDefinition<SyncLog> def =
                metadataRegistry.getDefinition(SyncEntityType.SYNC_LOG);
        if (def == null) {
            log.warn("[SYNC-LOG] cleanLogsInDb 未在 SyncMetadataRegistry 中找到 SYNC_LOG 定义");
            return 0L;
        }

        BaseMapper<SyncLog> baseMapper = def.getMapper(useDb);
        if (!(baseMapper instanceof SyncLogBaseMapper syncLogMapper)) {
            log.warn("[SYNC-LOG] cleanLogsInDb 时 Mapper 未实现 SyncLogBaseMapper，db={}", useDb);
            return 0L;
        }

        Long before = baseMapper.selectCount(null);

        // 调用对应库的存储过程：sp_clean_sync_log
        syncLogMapper.callCleanProc(days, max);

        Long after = baseMapper.selectCount(null);

        long deleted = 0L;
        if (before != null && after != null && before > after) {
            deleted = before - after;
        }

        log.info("[SYNC-LOG] cleanLogsInDb 完成，db={}，删除条数={}", useDb, deleted);
        return deleted;
    }



    /**
     * 判断是否为“数据冲突类错误”：
     *  - 只针对 CREATE / UPDATE
     *  - 只匹配主键 / 唯一约束相关错误关键字
     */
    private boolean isConflictError(SyncAction action, String errorMsg) {
        if (errorMsg == null || errorMsg.isBlank()) {
            return false;
        }

        // DELETE 基本不会产生“冲突”，直接排除
        if (action == SyncAction.DELETE) {
            return false;
        }

        String lower = errorMsg.toLowerCase();

        // MySQL / 通用英文
        if (lower.contains("duplicate entry")
                || lower.contains("duplicate key")
                || lower.contains("primary key")
                || lower.contains("unique constraint")
                || lower.contains("unique index")
                || lower.contains("already exists")) {
            return true;
        }

        // PostgreSQL 常见文案
        if (lower.contains("violates unique constraint")) {
            return true;
        }

        // SQL Server 常见文案（英文）
        if (lower.contains("violation of unique key constraint")
                || lower.contains("violation of primary key constraint")
                || lower.contains("cannot insert duplicate key row")) {
            return true;
        }

        // 中文环境下可能出现的提示
        return lower.contains("违反唯一约束")
                || lower.contains("主键冲突")
                || lower.contains("唯一约束");
    }
}

