package com.notice.system.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.common.Result;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.aggBy.SyncConflictAggBy;
import com.notice.system.service.AuthService;
import com.notice.system.service.SyncConflictService;
import com.notice.system.vo.conflict.SyncConflictDetailVo;
import com.notice.system.vo.conflict.SyncConflictQueryVo;
import com.notice.system.vo.conflict.SyncConflictWithItemsVo;
import com.notice.system.vo.conflict.req.SyncConflictNoteVo;
import com.notice.system.vo.conflict.req.SyncConflictResolveVo;
import com.notice.system.vo.report.AggVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步冲突工单（管理端）
 * 路径前缀：/api/admin/sync-conflicts
 * 权限：管理员
 * 选库：?db=MYSQL/PG/SQLSERVER（默认 syncConflictService.defaultDb()）
 * 功能：
 *  - 分页列表 / 详情
 *  - 管理员处理：resolve / ignore / reopen / recheck
 *  - 运维按钮：批量 recheck-open / notify
 *  - 报表聚合
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sync-conflicts")
@RequiredArgsConstructor
public class SyncConflictAdminController {

    private final AuthService authService;
    private final SyncConflictService syncConflictService;

    /* ======================= 1) 分页列表 ======================= */

    @PostMapping("/page")
    public Result<Page<SyncConflictWithItemsVo>> page(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody(required = false) SyncConflictQueryVo query
    ) {
        DatabaseType useDb = (db == null ? syncConflictService.defaultDb() : db);
        authService.requireAdmin(useDb);

        return Result.success(syncConflictService.pageConflicts(useDb, query));
    }

    /* ======================= 2) 详情 ======================= */

    @GetMapping("/{id}")
    public Result<SyncConflictDetailVo> detail(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = (db == null ? syncConflictService.defaultDb() : db);
        authService.requireAdmin(useDb);


        return Result.success(syncConflictService.getDetailWithRows(useDb, id));
    }

    /* ======================= 3) 管理员操作 ======================= */

    /**
     * 解决冲突：选择某库为“正确来源”，覆盖其他库（以 sourceDb 为准进行修复）
     * db：冲突工单所在库（用于鉴权/读取工单）
     * vo.sourceDb：用于修复的数据源库（你选择哪个库为正确）
     */
    @PutMapping("/{id}/resolve")
    public Result<?> resolve(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody SyncConflictResolveVo vo
    ) {
        DatabaseType useDb = (db == null ? syncConflictService.defaultDb() : db);
        authService.requireAdmin(useDb);

        if (vo == null || vo.getSourceDb() == null) {
            return Result.fail("sourceDb 不能为空");
        }

        boolean ok = syncConflictService.resolveConflict(id, vo.getSourceDb(), vo.getNote());
        return ok ? Result.success("已尝试修复") : Result.fail("修复失败（请查看日志/冲突仍可能存在）");
    }

    /**
     * 忽略冲突：标记为 IGNORED
     */
    @PutMapping("/{id}/ignore")
    public Result<?> ignore(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody(required = false) SyncConflictNoteVo vo
    ) {
        DatabaseType useDb = (db == null ? syncConflictService.defaultDb() : db);
        authService.requireAdmin(useDb);

        String note = (vo == null ? null : vo.getNote());
        boolean ok = syncConflictService.ignoreConflict(id, note);
        return ok ? Result.success("已忽略") : Result.fail("忽略失败");
    }

    /**
     * 重开冲突：标记为 OPEN
     */
    @PutMapping("/{id}/reopen")
    public Result<?> reopen(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody(required = false) SyncConflictNoteVo vo
    ) {
        DatabaseType useDb = (db == null ? syncConflictService.defaultDb() : db);
        authService.requireAdmin(useDb);

        String note = (vo == null ? null : vo.getNote());
        boolean ok = syncConflictService.reopenConflict(id, note, null);
        return ok ? Result.success("已重开") : Result.fail("重开失败");
    }

    /**
     * 重新检测：刷新 items 快照 + 自动结单/重开
     */
    @PutMapping("/{id}/recheck")
    public Result<?> recheck(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = (db == null ? syncConflictService.defaultDb() : db);
        authService.requireAdmin(useDb);

        boolean ok = syncConflictService.recheckConflict(id, null);
        return ok ? Result.success("已重新检测") : Result.fail("重新检测失败");
    }

    /* ======================= 4) 手动触发（运维按钮） ======================= */

    /**
     * 手动跑一轮：重检 OPEN 冲突
     */
    @PostMapping("/tasks/recheck-open")
    public Result<Integer> runRecheckOpen(
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = (db == null ? syncConflictService.defaultDb() : db);
        authService.requireAdmin(useDb);

        int n = syncConflictService.recheckOpenConflicts(LocalDateTime.now(), limit == null ? 50 : limit);
        return Result.success(n);
    }

    /**
     * 手动跑一轮：发送待通知冲突
     */
    @PostMapping("/tasks/notify")
    public Result<Integer> runNotify(
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = (db == null ? syncConflictService.defaultDb() : db);
        authService.requireAdmin(useDb);

        int n = syncConflictService.notifyPendingConflicts(LocalDateTime.now(), limit == null ? 20 : limit);
        return Result.success(n);
    }

    /* ======================= 5) 报表聚合 ======================= */

    @PostMapping("/agg")
    public Result<List<AggVo>> aggConflicts(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestParam(name = "by", required = false, defaultValue = "STATUS") SyncConflictAggBy by,
            @RequestParam(name = "begin", required = false) LocalDateTime begin,
            @RequestParam(name = "end", required = false) LocalDateTime end,
            @RequestBody(required = false) SyncConflictQueryVo filter
    ) {
        DatabaseType useDb = (db == null ? syncConflictService.defaultDb() : db);
        authService.requireAdmin(useDb);

        return Result.success(syncConflictService.aggConflicts(useDb, begin, end, by, filter));
    }
}




