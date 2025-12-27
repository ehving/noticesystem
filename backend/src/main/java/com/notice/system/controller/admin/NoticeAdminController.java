package com.notice.system.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.common.Result;
import com.notice.system.converter.NoticeConverter;
import com.notice.system.entity.Dept;
import com.notice.system.entity.Notice;
import com.notice.system.entity.NoticeTargetDept;
import com.notice.system.entity.User;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.service.AuthService;
import com.notice.system.service.DeptService;
import com.notice.system.service.NoticeService;
import com.notice.system.service.NoticeTargetDeptService;
import com.notice.system.vo.notice.NoticeAdminPageVo;
import com.notice.system.vo.notice.NoticeAdminRowVo;
import com.notice.system.vo.notice.NoticeAdminSaveVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 公告管理接口（管理端）
 * 权限：管理员
 * 选库：
 *  - 查询接口：使用请求参数 db（默认 noticeService.defaultDb()）
 *  - 写接口：使用 vo.sourceDb 或请求参数 db（默认 noticeService.defaultDb()），并触发多库同步
 * 功能：
 *  - 分页查询公告（含 scope 展示）
 *  - 新建/编辑公告（含目标部门）
 *  - 发布/撤回/删除
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class NoticeAdminController {

    private final AuthService authService;
    private final NoticeService noticeService;
    private final DeptService deptService;
    private final NoticeTargetDeptService noticeTargetDeptService;

    /* ======================== 1) 管理端分页查询 ======================== */

    /**
     * 管理端分页查询公告（基础版）
     * 权限：管理员
     * 选库：db 可选
     */
    @PostMapping("/page")
    public Result<Page<Notice>> pageNotices(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody NoticeAdminPageVo vo
    ) {
        DatabaseType useDb = (db == null ? noticeService.defaultDb() : db);
        authService.requireAdmin(useDb);

        Page<Notice> page = noticeService.pageAdminNoticesInDb(useDb, vo);
        return Result.success(page);
    }

    /**
     * 管理端分页查询公告（带 scope 展示）
     * scopeType：
     *  - GLOBAL：全局公告（未绑定任何部门）
     *  - DEPT  ：定向公告（绑定了若干目标部门）
     * 权限：管理员
     * 选库：db 可选
     */
    @PostMapping("/pageWithScope")
    public Result<Page<NoticeAdminRowVo>> pageNoticesWithScope(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody NoticeAdminPageVo vo
    ) {
        DatabaseType useDb = (db == null ? noticeService.defaultDb() : db);
        authService.requireAdmin(useDb);

        Page<NoticeAdminRowVo> page = noticeService.pageAdminNoticesWithScopeInDb(useDb, vo);
        return Result.success(page);
    }

    /**
     * 获取某条公告的目标部门信息（用于回显）
     * 返回：
     *  - scopeType: GLOBAL/DEPT
     *  - deptIds: 目标部门 id 列表
     *  - depts: [{id,name}]（仅用于展示）
     * 权限：管理员
     * 选库：db 可选
     */
    @GetMapping("/{noticeId}/targets")
    public Result<Map<String, Object>> getNoticeTargets(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @PathVariable("noticeId") String noticeId
    ) {
        DatabaseType useDb = (db == null ? noticeService.defaultDb() : db);
        authService.requireAdmin(useDb);

        if (noticeId == null || noticeId.isBlank()) {
            return Result.fail("noticeId 不能为空");
        }

        List<NoticeTargetDept> rels = noticeTargetDeptService.listByNoticeIdFromDb(useDb, noticeId);

        Set<String> deptIds = new LinkedHashSet<>();
        if (rels != null) {
            for (NoticeTargetDept r : rels) {
                if (r == null) continue;
                String did = r.getDeptId();
                if (did != null && !did.isBlank()) deptIds.add(did);
            }
        }

        List<Dept> depts = deptIds.isEmpty()
                ? Collections.emptyList()
                : deptService.listByIdsFromDb(useDb, deptIds);

        List<Map<String, String>> items = new ArrayList<>();
        if (depts != null) {
            for (Dept d : depts) {
                if (d == null) continue;
                Map<String, String> m = new HashMap<>();
                m.put("id", d.getId());
                m.put("name", d.getName());
                items.add(m);
            }
        }

        Map<String, Object> ret = new HashMap<>();
        ret.put("scopeType", deptIds.isEmpty() ? "GLOBAL" : "DEPT");
        ret.put("deptIds", new ArrayList<>(deptIds));
        ret.put("depts", items);
        return Result.success(ret);
    }

    /* ======================== 2) 新建公告 ======================== */

    /**
     * 新建公告（默认以草稿创建），并写入目标部门关联
     * 权限：管理员
     * 源库：vo.sourceDb（可选，默认 noticeService.defaultDb()）
     * 规则：
     *  - title/content 必填
     *  - 若 publishTime 非空：必须至少晚于当前时间 1 分钟（用于定时发布）
     */
    @PostMapping
    public Result<?> createNotice(@RequestBody NoticeAdminSaveVo vo) {
        DatabaseType useDb = (vo.getSourceDb() == null ? noticeService.defaultDb() : vo.getSourceDb());
        authService.requireAdmin(useDb);
        User admin = authService.requireLoginAdmin(useDb);

        if (vo.getTitle() == null || vo.getTitle().isBlank()) return Result.fail("标题不能为空");
        if (vo.getContent() == null || vo.getContent().isBlank()) return Result.fail("内容不能为空");

        if (vo.getPublishTime() != null) {
            LocalDateTime min = LocalDateTime.now().plusSeconds(59);
            if (!vo.getPublishTime().isAfter(min)) {
                return Result.fail("定时发布时间必须至少晚于当前时间1分钟");
            }
        }

        Notice notice = NoticeConverter.toEntityForCreate(vo, admin.getId());
        notice.setStatus("DRAFT");
        notice.setViewCount(0L);

        noticeService.createNoticeInDb(useDb, notice, vo.getTargetDeptIds());
        return Result.success("公告创建成功（草稿）");
    }

    /* ======================== 3) 修改公告 ======================== */

    /**
     * 修改公告（仅允许编辑 草稿/撤回 状态）
     * 权限：管理员
     * 源库：vo.sourceDb（可选，默认 noticeService.defaultDb()）
     * 规则：
     *  - 已发布公告不允许直接编辑（需先撤回）
     *  - 若 publishTime 非空：必须至少晚于当前时间 1 分钟
     *  - 忽略前端传入的 status：保持原状态
     */
    @PutMapping("/{id}")
    public Result<?> updateNotice(
            @PathVariable("id") String id,
            @RequestBody NoticeAdminSaveVo vo
    ) {
        DatabaseType useDb = (vo.getSourceDb() == null ? noticeService.defaultDb() : vo.getSourceDb());
        authService.requireAdmin(useDb);

        Notice notice = noticeService.getById(useDb, id);
        if (notice == null) return Result.fail("公告不存在");

        String st = notice.getStatus();
        if (!"DRAFT".equalsIgnoreCase(st) && !"RECALLED".equalsIgnoreCase(st)) {
            return Result.fail("已发布公告不允许直接编辑，请先撤回后再编辑");
        }

        if (vo.getPublishTime() != null) {
            LocalDateTime min = LocalDateTime.now().plusSeconds(59);
            if (!vo.getPublishTime().isAfter(min)) {
                return Result.fail("定时发布时间必须至少晚于当前时间1分钟");
            }
        }

        String keepStatus = notice.getStatus();
        NoticeConverter.applyAdminUpdate(vo, notice);
        notice.setStatus(keepStatus);

        noticeService.updateNoticeInDb(useDb, notice, vo.getTargetDeptIds());
        return Result.success("公告更新成功");
    }

    /* ======================== 4) 发布公告 ======================== */

    /**
     * 立即发布公告
     * 权限：管理员
     * 选库：db 可选（该库作为源库触发同步）
     */
    @PostMapping("/{id}/publish")
    public Result<?> publish(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = (db == null ? noticeService.defaultDb() : db);
        authService.requireAdmin(useDb);

        boolean ok = noticeService.publishNoticeNowInDb(useDb, id);
        return ok ? Result.success("已发布") : Result.fail("发布失败：公告不存在或状态不允许");
    }

    /* ======================== 5) 撤回公告 ======================== */

    /**
     * 撤回公告（仅允许撤回已发布）
     * 权限：管理员
     * 选库：db 可选（该库作为源库触发同步）
     */
    @PostMapping("/{id}/recall")
    public Result<?> recallNotice(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = (db == null ? noticeService.defaultDb() : db);
        authService.requireAdmin(useDb);

        boolean ok = noticeService.recallNoticeInDb(useDb, id);
        return ok ? Result.success("已撤回") : Result.fail("撤回失败：公告不存在或状态不允许");
    }

    /* ======================== 6) 删除公告 ======================== */

    /**
     * 删除公告（硬删除）
     * 权限：管理员
     * 选库：db 可选（该库作为源库触发同步）
     * 说明：
     *  - 先删 notice_target_dept 关联，再删 notice
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteNotice(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = (db == null ? noticeService.defaultDb() : db);
        authService.requireAdmin(useDb);

        if (id == null || id.isBlank()) {
            return Result.fail("公告 id 不能为空");
        }

        // 先删关联
        noticeTargetDeptService.removeByNoticeIdInDb(useDb, id);

        // 再删公告
        boolean ok = noticeService.removeByIdInDb(useDb, id);
        return ok ? Result.success("已成功删除！") : Result.fail("删除失败：公告不存在或删除失败");
    }
}



