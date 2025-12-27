package com.notice.system.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.common.Result;
import com.notice.system.entity.SyncLog;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncAction;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.entityEnum.SyncLogStatus;
import com.notice.system.entityEnum.aggBy.SyncLogAggBy;
import com.notice.system.service.AuthService;
import com.notice.system.service.SyncLogService;
import com.notice.system.vo.report.AggVo;
import com.notice.system.vo.synclog.SyncLogDailyReportVo;
import com.notice.system.vo.synclog.SyncLogVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步日志管理接口（管理端）
 * 路径前缀：/api/admin/sync-logs
 * 权限：管理员
 * 选库：?db=MYSQL/PG/SQLSERVER（默认 syncLogService.defaultDb()）
 * 功能：
 *  - 分页查询 / 详情
 *  - 按日志重试
 *  - 手动清理
 *  - 枚举下拉
 *  - 聚合报表 / 日报
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sync-logs")
@RequiredArgsConstructor
public class SyncLogAdminController {

    private final AuthService authService;
    private final SyncLogService syncLogService;

    /* ======================= 1) 分页查询 ======================= */

    @PostMapping("/page")
    public Result<Page<SyncLog>> pageLogs(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody(required = false) SyncLogVo vo
    ) {
        DatabaseType useDb = (db == null ? syncLogService.defaultDb() : db);
        authService.requireAdmin(useDb);

        SyncLogVo query = (vo == null ? new SyncLogVo() : vo);
        return Result.success(syncLogService.pageLogs(useDb, query));
    }

    /* ======================= 2) 详情 ======================= */

    @GetMapping("/{id}")
    public Result<SyncLog> getLogDetail(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = (db == null ? syncLogService.defaultDb() : db);
        authService.requireAdmin(useDb);

        SyncLog logRecord = syncLogService.getById(useDb, id);
        return (logRecord == null) ? Result.fail("日志不存在或已被清理") : Result.success(logRecord);
    }

    /* ======================= 3) 按日志重试 ======================= */

    @PostMapping("/{id}/retry")
    public Result<?> retryByLog(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = (db == null ? syncLogService.defaultDb() : db);
        authService.requireAdmin(useDb);

        boolean ok = syncLogService.retrySyncByLogId(useDb, id);
        return ok ? Result.success("已提交重试任务") : Result.fail("重试失败：日志不存在或日志记录缺少必要信息");
    }

    /* ======================= 4) 手动清理 ======================= */

    /**
     * 手动清理同步日志（一次请求清理三库）
     * 说明：
     *  - 鉴权使用 db（或默认库）即可；实际清理会遍历 syncDbs()
     *  - retainDays/maxCount 可用于限制清理范围
     */
    @PostMapping("/clean")
    public Result<?> clean(
            @RequestParam(value = "db", required = false) DatabaseType db,
            @RequestParam(value = "retainDays", required = false) Integer retainDays,
            @RequestParam(value = "maxCount", required = false) Long maxCount
    ) {
        DatabaseType authDb = (db == null ? syncLogService.defaultDb() : db);
        authService.requireAdmin(authDb);

        long totalDeleted = 0L;
        for (DatabaseType eachDb : DatabaseType.syncDbs()) {
            totalDeleted += syncLogService.cleanLogsInDb(eachDb, retainDays, maxCount);
        }

        String msg = "本次共清理同步日志 " + totalDeleted + " 条";
        log.info("[SYNC_LOG_ADMIN] 手动清理完成，{}", msg);
        return Result.success(msg);
    }

    /* ======================= 5) 枚举下拉 ======================= */

    @GetMapping("/statuses")
    public Result<SyncLogStatus[]> listStatuses() {
        authService.requireAdmin(syncLogService.defaultDb());
        return Result.success(SyncLogStatus.values());
    }

    @GetMapping("/actions")
    public Result<SyncAction[]> listActions() {
        authService.requireAdmin(syncLogService.defaultDb());
        return Result.success(SyncAction.values());
    }

    @GetMapping("/entity-types")
    public Result<SyncEntityType[]> listEntityTypes() {
        authService.requireAdmin(syncLogService.defaultDb());
        return Result.success(SyncEntityType.values());
    }

    /* ======================= 6) 聚合报表 ======================= */

    @PostMapping("/agg")
    public Result<List<AggVo>> aggLogs(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestParam(name = "by", required = false, defaultValue = "STATUS") SyncLogAggBy by,
            @RequestParam(name = "begin", required = false) LocalDateTime begin,
            @RequestParam(name = "end", required = false) LocalDateTime end,
            @RequestBody(required = false) SyncLogVo filter
    ) {
        DatabaseType useDb = (db == null ? syncLogService.defaultDb() : db);
        authService.requireAdmin(useDb);
        return Result.success(syncLogService.aggLogs(useDb, begin, end, by, filter));
    }

    /* ======================= 7) 每日统计报表 ======================= */

    /**
     * 同步日志每日统计（不分页，直接返回列表供前端画图）
     *  - 支持选库 db
     *  - 复用 SyncLogVo 作为过滤条件
     */
    @PostMapping("/daily-report")
    public Result<List<SyncLogDailyReportVo>> dailyReport(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody(required = false) SyncLogVo vo
    ) {
        DatabaseType useDb = (db == null ? syncLogService.defaultDb() : db);
        authService.requireAdmin(useDb);

        return Result.success(syncLogService.listDailyReport(useDb, vo));
    }
}



