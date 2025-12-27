package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.SyncLog;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncAction;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.entityEnum.SyncLogStatus;
import com.notice.system.entityEnum.aggBy.SyncLogAggBy;
import com.notice.system.mapper.base.SyncLogBaseMapper;
import com.notice.system.mapper.dto.SyncLogDailyStat;
import com.notice.system.service.SyncLogService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.support.event.SyncLogEvent;
import com.notice.system.sync.SyncMetadataRegistry;
import com.notice.system.vo.report.AggVo;
import com.notice.system.vo.synclog.SyncLogDailyReportVo;
import com.notice.system.vo.synclog.SyncLogVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 同步日志服务：
 * <ul>
 *   <li>监听 SyncLogEvent 落库（日志表自身不再写日志，避免递归）</li>
 *   <li>提供：分页查询、聚合报表、清理、按日志重试、SUCCESS 日志去重扫描</li>
 * </ul>
 */
@Slf4j
@Service
public class SyncLogServiceImpl extends MultiDbSyncServiceImpl<SyncLog> implements SyncLogService {

    private static final int MAX_FAIL = 3;

    public SyncLogServiceImpl(SyncService syncService, SyncMetadataRegistry metadataRegistry) {
        super(syncService, metadataRegistry, SyncEntityType.SYNC_LOG, DatabaseType.MYSQL);
    }

    /* ======================== Event -> 落库 ======================== */

    /** 监听同步事件并记录日志（日志表自身不再写日志，避免递归）。 */
    @EventListener
    public void handleSyncLogEvent(SyncLogEvent event) {
        if (event == null || event.entityType() == null) return;

        if (!shouldLogEntity(event.entityType())) return;

        SyncLog logRecord = new SyncLog();
        logRecord.setEntityType(event.entityType());
        logRecord.setEntityId(event.entityId());
        logRecord.setAction(event.action());
        logRecord.setSourceDb(event.sourceDb());
        logRecord.setTargetDb(event.targetDb());
        logRecord.setStatus(event.status());
        logRecord.setErrorMsg(event.errorMsg());
        logRecord.setRetryCount(0);

        // 落到默认日志库，然后由 MultiDbSyncService 自动同步到其它库
        save(logRecord);
    }

    private boolean shouldLogEntity(SyncEntityType type) {
        return type != SyncEntityType.SYNC_LOG
                && type != SyncEntityType.SYNC_CONFLICT
                && type != SyncEntityType.SYNC_CONFLICT_ITEM;
    }

    /* ======================== 分页查询 ======================== */

    @Override
    public Page<SyncLog> pageLogs(DatabaseType logDb, SyncLogVo vo) {
        DatabaseType db = useDb(logDb);
        SyncLogVo q = (vo == null ? new SyncLogVo() : vo);

        long pageNoRaw = q.getPageNo();
        long pageSizeRaw = q.getPageSize();

        long pageNo = pageNoRaw <= 0 ? 1L : pageNoRaw;
        long pageSize = pageSizeRaw <= 0 ? 10L : pageSizeRaw;

        Page<SyncLog> page = new Page<>(pageNo, pageSize);

        LambdaQueryWrapper<SyncLog> w = new LambdaQueryWrapper<>();

        if (q.getEntityType() != null) w.eq(SyncLog::getEntityType, q.getEntityType());
        if (q.getAction() != null)     w.eq(SyncLog::getAction, q.getAction());
        if (q.getSourceDb() != null)   w.eq(SyncLog::getSourceDb, q.getSourceDb());
        if (q.getTargetDb() != null)   w.eq(SyncLog::getTargetDb, q.getTargetDb());
        if (q.getStatus() != null)     w.eq(SyncLog::getStatus, q.getStatus());

        String entityId = (q.getEntityId() == null ? null : q.getEntityId().trim());
        if (entityId != null && !entityId.isEmpty()) {
            w.eq(SyncLog::getEntityId, entityId);
        }

        if (q.getBeginTime() != null) w.ge(SyncLog::getCreateTime, q.getBeginTime());
        if (q.getEndTime() != null)   w.le(SyncLog::getCreateTime, q.getEndTime());

        w.orderByDesc(SyncLog::getCreateTime);

        return resolveMapper(db).selectPage(page, w);
    }

    @Override
    public SyncLog getById(DatabaseType logDb, String id) {
        return super.getById(useDb(logDb), id);
    }

    /* ======================== 重试计数 ======================== */

    @Override
    public void incrementRetryCount(DatabaseType logDb, String logId) {
        if (logId == null || logId.isBlank()) return;

        DatabaseType db = useDb(logDb);
        SyncLog logRecord = getById(db, logId);
        if (logRecord == null) return;

        int oldCount = (logRecord.getRetryCount() == null ? 0 : logRecord.getRetryCount());
        logRecord.setRetryCount(oldCount + 1);
        logRecord.setUpdateTime(LocalDateTime.now());

        updateByIdInDb(db, logRecord);
    }

    /* ======================== 按日志重试同步 ======================== */

    @Override
    public boolean retrySyncByLogId(DatabaseType logDb, String logId) {
        if (logId == null || logId.isBlank()) return false;

        DatabaseType db = useDb(logDb);

        SyncLog logRecord = getById(db, logId);
        if (logRecord == null) {
            log.warn("[SYNC-LOG] retry failed: not found, db={}, logId={}", db, logId);
            return false;
        }

        int oldRetry = (logRecord.getRetryCount() == null ? 0 : logRecord.getRetryCount());
        if (logRecord.getStatus() == SyncLogStatus.ERROR && oldRetry >= MAX_FAIL) {
            log.warn("[SYNC-LOG] retry refused: status=ERROR and max reached, db={}, logId={}, retryCount={}",
                    db, logId, oldRetry);
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        int newRetry = oldRetry + 1;

        // 1) 字段校验（不可重试则直接 ERROR）
        String invalidMsg = validateRetryable(logRecord);
        if (invalidMsg != null) {
            markUnretryable(db, logRecord, newRetry, now, invalidMsg);
            return false;
        }

        // 2) 发起重试
        SyncEntityType entityType = logRecord.getEntityType();
        SyncAction action = logRecord.getAction();
        DatabaseType sourceDb = logRecord.getSourceDb();
        DatabaseType targetDb = logRecord.getTargetDb();
        String entityId = logRecord.getEntityId();

        try {
            boolean ok = syncService.syncToTargetWithoutLog(entityType, entityId, action, sourceDb, targetDb);

            logRecord.setRetryCount(newRetry);
            logRecord.setUpdateTime(now);

            if (ok) {
                logRecord.setStatus(SyncLogStatus.SUCCESS);
                logRecord.setErrorMsg(null);
                log.info("[SYNC-LOG] retry ok: db={}, logId={}, {} {} {} -> {}",
                        db, logId, entityType, action, sourceDb, targetDb);
            } else {
                markRetryFailed(logRecord, newRetry);
                log.warn("[SYNC-LOG] retry failed: db={}, logId={}, retryCount={}, {} {} {} -> {}",
                        db, logId, newRetry, entityType, action, sourceDb, targetDb);
            }

            updateByIdInDb(db, logRecord);
            return ok;

        } catch (Exception e) {
            logRecord.setRetryCount(newRetry);
            logRecord.setUpdateTime(now);
            logRecord.setStatus(newRetry >= MAX_FAIL ? SyncLogStatus.ERROR : SyncLogStatus.FAILED);

            String msg = (e.getMessage() == null || e.getMessage().isBlank())
                    ? e.getClass().getSimpleName()
                    : e.getMessage();
            logRecord.setErrorMsg(msg);

            updateByIdInDb(db, logRecord);
            log.warn("[SYNC-LOG] retry exception: db={}, logId={}, retryCount={}, err={}", db, logId, newRetry, msg, e);
            return false;
        }
    }

    /** 返回 null 表示可重试；非 null 表示不可重试原因。 */
    private String validateRetryable(SyncLog logRecord) {
        if (logRecord.getEntityType() == null
                || logRecord.getAction() == null
                || logRecord.getSourceDb() == null
                || logRecord.getTargetDb() == null) {
            return "重试失败：日志字段缺失（不可重试）";
        }
        if (logRecord.getEntityId() == null || logRecord.getEntityId().isBlank()) {
            return "重试失败：日志缺少 entityId（不可重试）";
        }
        return null;
    }

    private void markRetryFailed(SyncLog logRecord, int newRetry) {
        if (newRetry >= MAX_FAIL) {
            logRecord.setStatus(SyncLogStatus.ERROR);
            logRecord.setErrorMsg("重试同步失败已达 3 次，已标记为 ERROR，请人工介入处理");
        } else {
            logRecord.setStatus(SyncLogStatus.FAILED);
            logRecord.setErrorMsg("重试同步仍失败，请查看失败日志详情");
        }
    }

    private void markUnretryable(DatabaseType db, SyncLog logRecord, int newRetry, LocalDateTime now, String msg) {
        logRecord.setRetryCount(newRetry);
        logRecord.setStatus(SyncLogStatus.ERROR);
        logRecord.setErrorMsg(msg);
        logRecord.setUpdateTime(now);
        updateByIdInDb(db, logRecord);
    }

    /* ======================== 日报（SQL） ======================== */

    @Override
    public List<SyncLogDailyReportVo> listDailyReport(DatabaseType logDb, SyncLogVo condition) {
        DatabaseType db = useDb(logDb);
        SyncLogVo vo = (condition == null ? new SyncLogVo() : condition);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = (vo.getEndTime() == null ? now : vo.getEndTime());
        LocalDateTime begin = (vo.getBeginTime() == null
                ? now.minusDays(6).toLocalDate().atStartOfDay()
                : vo.getBeginTime());
        if (begin.isAfter(end)) {
            LocalDateTime tmp = begin; begin = end; end = tmp;
        }

        SyncLogBaseMapper mapper = resolveMapperAs(db, SyncLogBaseMapper.class);

        List<SyncLogDailyStat> stats = mapper.listDailyStats(
                begin, end,
                vo.getEntityType() == null ? null : vo.getEntityType().name(),
                vo.getAction() == null ? null : vo.getAction().name(),
                vo.getStatus() == null ? null : vo.getStatus().name(),
                vo.getSourceDb() == null ? null : vo.getSourceDb().name(),
                vo.getTargetDb() == null ? null : vo.getTargetDb().name()
        );
        if (stats == null || stats.isEmpty()) return Collections.emptyList();

        List<SyncLogDailyReportVo> result = new ArrayList<>(stats.size());
        for (SyncLogDailyStat stat : stats) {
            if (stat == null) continue;
            SyncLogDailyReportVo r = new SyncLogDailyReportVo();
            r.setStatDate(stat.getStatDate() == null ? null : stat.getStatDate().toString());
            r.setSourceDb(stat.getSourceDb());
            r.setTargetDb(stat.getTargetDb());
            r.setTotalCount(stat.getTotalCount());
            r.setSuccessCount(stat.getSuccessCount());
            r.setFailedCount(stat.getFailedCount());
            r.setConflictCount(stat.getConflictCount());
            r.setErrorCount(stat.getErrorCount());
            result.add(r);
        }
        return result;
    }

    /* ======================== 清理（存储过程） ======================== */

    @Override
    public long cleanLogsInDb(DatabaseType db, Integer retainDays, Long maxCount) {
        DatabaseType useDb = (db == null ? DatabaseType.MYSQL : db);

        int days = (retainDays == null || retainDays <= 0) ? 90 : retainDays;
        long max = (maxCount == null || maxCount <= 0) ? 100000L : maxCount;

        SyncLogBaseMapper mapper = resolveMapperAs(useDb, SyncLogBaseMapper.class);

        long before = Optional.ofNullable(resolveMapper(useDb).selectCount(null)).orElse(0L);
        mapper.callCleanProc(days, max);
        long after = Optional.ofNullable(resolveMapper(useDb).selectCount(null)).orElse(0L);

        long deleted = Math.max(0L, before - after);
        log.info("[SYNC-LOG] clean done: db={}, deleted={}", useDb, deleted);
        return deleted;
    }

    /* ======================== SUCCESS 日志扫描（Conflict 用） ======================== */

    @Override
    public List<SyncLog> listRecentSuccessLogsDedup(LocalDateTime fromTime,
                                                    LocalDateTime toTime,
                                                    int perDbLimit,
                                                    int entityLimit) {
        int perLim = perDbLimit <= 0 ? 200 : perDbLimit;
        int entLim = entityLimit <= 0 ? 200 : entityLimit;

        List<SyncLog> all = new ArrayList<>(perLim * 3);

        for (DatabaseType eachDb : DatabaseType.all()) {
            try {
                SyncLogBaseMapper mapper = resolveMapperAs(eachDb, SyncLogBaseMapper.class);
                List<SyncLog> part = mapper.listRecentSuccessLogs(fromTime, toTime, perLim);
                if (part != null && !part.isEmpty()) all.addAll(part);
            } catch (Exception e) {
                // 单库异常不影响整体扫描
                log.warn("[SYNC-LOG] recentSuccess query failed, db={}, err={}", eachDb, e.getMessage(), e);
            }
        }

        if (all.isEmpty()) return Collections.emptyList();

        // 按 (entityType, entityId) 去重：保留最新一条
        Map<String, SyncLog> latest = new LinkedHashMap<>();
        for (SyncLog l : all) {
            if (l == null || l.getEntityType() == null || l.getEntityId() == null) continue;

            String key = l.getEntityType() + "|" + l.getEntityId();
            SyncLog prev = latest.get(key);

            if (prev == null) {
                latest.put(key, l);
                continue;
            }

            LocalDateTime t1 = nvlTime(prev.getUpdateTime(), prev.getCreateTime());
            LocalDateTime t2 = nvlTime(l.getUpdateTime(), l.getCreateTime());
            if (t2 != null && (t1 == null || t2.isAfter(t1))) {
                latest.put(key, l);
            }
        }

        return latest.values().stream()
                .sorted((a, b) -> {
                    LocalDateTime ta = nvlTime(a.getUpdateTime(), a.getCreateTime());
                    LocalDateTime tb = nvlTime(b.getUpdateTime(), b.getCreateTime());
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return tb.compareTo(ta);
                })
                .limit(entLim)
                .toList();
    }

    /* ======================== 聚合报表（SQL） ======================== */

    @Override
    public List<AggVo> aggLogs(DatabaseType db,
                               LocalDateTime begin,
                               LocalDateTime end,
                               SyncLogAggBy by,
                               SyncLogVo filter) {
        DatabaseType useDb = useDb(db);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime b = (begin == null ? now.minusDays(6).toLocalDate().atStartOfDay() : begin);
        LocalDateTime e = (end == null ? now : end);
        if (b.isAfter(e)) { LocalDateTime tmp = b; b = e; e = tmp; }
        if (by == null) throw new IllegalArgumentException("agg by 不能为空");

        SyncLogBaseMapper mapper = resolveMapperAs(useDb, SyncLogBaseMapper.class);

        SyncLogVo f = (filter == null ? new SyncLogVo() : filter);
        String groupCol = mapSyncLogGroupCol(useDb, by);

        return mapper.agg(
                b, e, groupCol,
                f.getEntityType() == null ? null : f.getEntityType().name(),
                f.getAction() == null ? null : f.getAction().name(),
                f.getStatus() == null ? null : f.getStatus().name(),
                f.getSourceDb() == null ? null : f.getSourceDb().name(),
                f.getTargetDb() == null ? null : f.getTargetDb().name()
        );
    }

    private String mapSyncLogGroupCol(DatabaseType db, SyncLogAggBy by) {
        boolean isSqlServer = db == DatabaseType.SQLSERVER;
        return switch (by) {
            case STATUS      -> isSqlServer ? "l.[status]" : "l.status";
            case ENTITY_TYPE -> "l.entity_type";
            case ACTION      -> "l.action";
            case SOURCE_DB   -> "l.source_db";
            case TARGET_DB   -> "l.target_db";
        };
    }

    private static LocalDateTime nvlTime(LocalDateTime a, LocalDateTime b) {
        return a != null ? a : b;
    }
}



