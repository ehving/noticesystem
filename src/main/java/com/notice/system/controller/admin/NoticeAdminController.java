package com.notice.system.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.common.Result;
import com.notice.system.converter.NoticeConverter;
import com.notice.system.entity.Notice;
import com.notice.system.entity.User;
import com.notice.system.service.AuthService;
import com.notice.system.service.NoticeService;
import com.notice.system.sync.DatabaseType;
import com.notice.system.vo.notice.NoticeAdminPageVo;
import com.notice.system.vo.notice.NoticeAdminSaveVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 公告管理接口（管理员）
 *
 * 路径前缀：
 *  - /api/admin/notices
 *
 * 功能：
 *  - 管理端公告分页查询
 *  - 新建公告（含目标部门）
 *  - 修改公告（含目标部门）
 *  - 撤回公告
 *
 * 说明：
 *  - 所有接口仅对管理员开放（authService.requireAdmin()）
 *  - 新建 / 修改 / 撤回支持选择源库 sourceDb，默认 MYSQL
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class NoticeAdminController {

    private final AuthService authService;
    private final NoticeService noticeService;

    // ============ 1. 管理端公告分页查询 ============

    @PostMapping("/page")
    public Result<Page<Notice>> pageNotices(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody NoticeAdminPageVo vo
    ) {
        authService.requireAdmin();
        DatabaseType useDb = (db == null ? noticeService.defaultDb() : db);
        Page<Notice> page = noticeService.pageAdminNoticesInDb(useDb, vo);
        return Result.success(page);
    }


    // ============ 2. 新建公告 ============

    @PostMapping
    public Result<?> createNotice(@RequestBody NoticeAdminSaveVo vo) {
        authService.requireAdmin();
        User admin = authService.requireLoginUser();

        // 基本校验（还是应该留在这里或 Service）
        if (vo.getTitle() == null || vo.getTitle().isBlank()) {
            return Result.fail("标题不能为空");
        }
        if (vo.getContent() == null || vo.getContent().isBlank()) {
            return Result.fail("内容不能为空");
        }

        // 决定状态默认值（业务规则）
        String status = (vo.getStatus() == null || vo.getStatus().isBlank())
                ? "DRAFT"
                : vo.getStatus().trim();

        // 用 Converter 把 VO 转成实体
        Notice notice = NoticeConverter.toEntityForCreate(vo, admin.getId());
        notice.setStatus(status);
        notice.setViewCount(0L);

        // 如果是发布状态且未指定发布时间，则由后端设置为当前时间
        if ("PUBLISHED".equalsIgnoreCase(status) && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }

        Collection<String> targetDeptIds = vo.getTargetDeptIds();

        // 源库选择（默认为 MYSQL）
        DatabaseType sourceDb = vo.getSourceDb() == null
                ? DatabaseType.MYSQL
                : vo.getSourceDb();

        if (sourceDb == DatabaseType.MYSQL) {
            noticeService.createNotice(notice, targetDeptIds);
        } else {
            noticeService.createNoticeInDb(sourceDb, notice, targetDeptIds);
        }

        return Result.success("公告创建成功");
    }

    // ============ 3. 修改公告 ============

    @PutMapping("/{id}")
    public Result<?> updateNotice(
            @PathVariable("id") String id,
            @RequestBody NoticeAdminSaveVo vo
    ) {
        authService.requireAdmin();

        Notice notice = noticeService.getById(id);
        if (notice == null) {
            return Result.fail("公告不存在");
        }

        // 把 VO 的改动应用到实体
        NoticeConverter.applyAdminUpdate(vo, notice);

        // 如果状态改成 PUBLISHED 且 publishTime 仍为空，则补当前时间
        if ("PUBLISHED".equalsIgnoreCase(notice.getStatus())
                && notice.getPublishTime() == null) {
            notice.setPublishTime(LocalDateTime.now());
        }

        Collection<String> targetDeptIds = vo.getTargetDeptIds();

        // 源库选择（默认为 MYSQL）
        DatabaseType sourceDb = vo.getSourceDb() == null
                ? DatabaseType.MYSQL
                : vo.getSourceDb();

        if (sourceDb == DatabaseType.MYSQL) {
            noticeService.updateNotice(notice, targetDeptIds);
        } else {
            noticeService.updateNoticeInDb(sourceDb, notice, targetDeptIds);
        }

        return Result.success("公告更新成功");
    }

    // ============ 4. 撤回公告 ============

    @PostMapping("/{id}/recall")
    public Result<?> recallNotice(
            @PathVariable("id") String id,
            @RequestParam(name = "sourceDb", required = false) DatabaseType sourceDb
    ) {
        authService.requireAdmin();

        DatabaseType db = (sourceDb == null) ? DatabaseType.MYSQL : sourceDb;

        if (db == DatabaseType.MYSQL) {
            noticeService.recallNotice(id);
        } else {
            noticeService.recallNoticeInDb(db, id);
        }

        return Result.success("公告已撤回");
    }
}

