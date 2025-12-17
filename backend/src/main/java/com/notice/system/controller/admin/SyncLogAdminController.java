package com.notice.system.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.common.Result;
import com.notice.system.entity.SyncLog;
import com.notice.system.service.AuthService;
import com.notice.system.service.SyncLogService;
import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncAction;
import com.notice.system.sync.SyncEntityType;
import com.notice.system.vo.synclog.SyncLogDailyReportVo;
import com.notice.system.vo.synclog.SyncLogVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 同步日志管理接口（管理端）
 * 路径前缀：
 *  - /api/admin/sync-logs
 * 功能：
 *  - 分页查询同步日志
 *  - 单条日志详情
 *  - 根据日志记录手动重试同步
 *  - 手动清理同步日志
 *  - 提供状态 / 动作 / 实体类型枚举给前端做下拉
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sync-logs")
@RequiredArgsConstructor
public class SyncLogAdminController {

    private final AuthService authService;
    private final SyncLogService syncLogService;

    // ============ 1. 分页查询同步日志 ============

    @PostMapping("/page")
    public Result<Page<SyncLog>> pageLogs(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody(required = false) SyncLogVo vo
    ) {
        authService.requireAdmin();

        SyncLogVo query = (vo == null ? new SyncLogVo() : vo);
        DatabaseType logDb = (db == null ? syncLogService.defaultDb() : db);

        Page<SyncLog> page = syncLogService.pageLogs(logDb, query);
        return Result.success(page);
    }

    // ============ 2. 单条日志详情 ============

    @GetMapping("/{id}")
    public Result<SyncLog> getLogDetail(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        authService.requireAdmin();

        DatabaseType logDb = (db == null ? syncLogService.defaultDb() : db);
        SyncLog logRecord = syncLogService.getById(logDb, id);
        if (logRecord == null) {
            return Result.fail("日志不存在或已被清理");
        }
        return Result.success(logRecord);
    }

    // ============ 3. 根据日志记录手动重试同步 ============

    @PostMapping("/{id}/retry")
    public Result<?> retryByLog(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        authService.requireAdmin();

        DatabaseType logDb = (db == null ? syncLogService.defaultDb() : db);

        // 推荐：将重试逻辑下沉到 SyncLogService
        boolean ok = syncLogService.retrySyncByLogId(logDb, id);
        if (!ok) {
            return Result.fail("重试失败：日志不存在或日志记录缺少必要信息");
        }

        return Result.success("已提交重试任务");
    }

    // ============ 4. 手动清理同步日志 ============

    @PostMapping("/clean")
    public Result<?> clean(
            @RequestParam(value = "db", required = false) String db,
            @RequestParam(value = "retainDays", required = false) Integer retainDays,
            @RequestParam(value = "maxCount", required = false) Long maxCount
    ) {
        authService.requireAdmin();

        long totalDeleted = 0L;

        if (db == null || db.isBlank() || "ALL".equalsIgnoreCase(db)) {
            for (DatabaseType type : DatabaseType.values()) {
                totalDeleted += syncLogService.cleanLogsInDb(type, retainDays, maxCount);
            }
        } else {
            DatabaseType logDb = parseDbOrDefault(db);
            totalDeleted = syncLogService.cleanLogsInDb(logDb, retainDays, maxCount);
        }

        String msg = "本次共清理同步日志 " + totalDeleted + " 条";
        log.info("[SYNC_LOG_ADMIN] 手动清理完成，{}", msg);
        return Result.success(msg);
    }


    // ============ 5. 提供枚举值给前端下拉 ============

    @GetMapping("/statuses")
    public Result<String[]> listStatuses() {
        authService.requireAdmin();
        return Result.success(new String[]{"SUCCESS", "FAILED"});
    }

    @GetMapping("/actions")
    public Result<SyncAction[]> listActions() {
        authService.requireAdmin();
        return Result.success(SyncAction.values());
    }

    @GetMapping("/entity-types")
    public Result<SyncEntityType[]> listEntityTypes() {
        authService.requireAdmin();
        return Result.success(SyncEntityType.values());
    }
    // ============ 6. 同步日志每日统计报表 ============
    /**
     *
     *  - 支持选库（db 参数）
     *  - 复用 SyncLogVo 作为查询条件（时间范围 / 实体类型 / 动作 / 状态 / 源库 / 目标库）
     *  - 不分页：返回一个列表，前端可以直接做图表
     */
    @PostMapping("/daily-report")
    public Result<List<SyncLogDailyReportVo>> dailyReport(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody(required = false) SyncLogVo vo
    ) {
        authService.requireAdmin();

        DatabaseType logDb = (db == null ? syncLogService.defaultDb() : db);
        List<SyncLogDailyReportVo> list = syncLogService.listDailyReport(logDb, vo);

        return Result.success(list);
    }

    // ============ 工具方法 ============

    /**
     * 解析 db 字符串为 DatabaseType，非法时默认 MYSQL
     */
    private DatabaseType parseDbOrDefault(String db) {
        if (db == null || db.isBlank()) {
            return DatabaseType.MYSQL;
        }
        try {
            return DatabaseType.valueOf(db.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("[SYNC_LOG_ADMIN] 非法 db 参数：{}，将使用默认 MYSQL", db);
            return DatabaseType.MYSQL;
        }
    }
}


