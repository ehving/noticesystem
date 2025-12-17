package com.notice.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.common.Result;
import com.notice.system.entity.Notice;
import com.notice.system.entity.User;
import com.notice.system.exception.UnauthenticatedException;
import com.notice.system.service.AuthService;
import com.notice.system.service.NoticeReadService;
import com.notice.system.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 公告用户侧接口：
 *  - 分页查询已发布公告
 *  - 查看公告详情（登录用户自动标记已读）
 *
 * 说明：
 *  - 不提供选库参数，统一走默认数据源（例如 MYSQL）
 */
@Slf4j
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticeReadService noticeReadService;
    private final AuthService authService;

    /**
     * 分页查询已发布公告（匿名可访问）
     *
     * @param pageNo   页码，默认 1
     * @param pageSize 每页大小，默认 10
     * @param keyword  标题/内容模糊搜索（可空）
     * @param level    公告等级（NORMAL/IMPORTANT/URGENT，可空）
     */
    @GetMapping("/page")
    public Result<Page<Notice>> pageNotices(
            @RequestParam(name = "pageNo", required = false, defaultValue = "1") long pageNo,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") long pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "level", required = false) String level
    ) {
        Page<Notice> page = noticeService.pagePublishedForUser(pageNo, pageSize, keyword, level);
        return Result.success(page);
    }


    /**
     * 公告详情：
     *  - 匿名用户：仅返回详情，不记录已读
     *  - 登录用户：返回详情并自动记录已读
     */
    @GetMapping("/{id}")
    public Result<Notice> getNoticeDetail(@PathVariable("id") String id) {

        Notice notice = noticeService.getById(id);
        if (notice == null) {
            return Result.fail("公告不存在");
        }

        // 只允许查看已发布且在有效期内的公告
        if (!"PUBLISHED".equalsIgnoreCase(notice.getStatus())) {
            return Result.fail("公告未发布或已被撤回");
        }

        LocalDateTime now = LocalDateTime.now();
        if (notice.getPublishTime() != null && notice.getPublishTime().isAfter(now)) {
            return Result.fail("公告尚未生效");
        }
        if (notice.getExpireTime() != null && notice.getExpireTime().isBefore(now)) {
            return Result.fail("公告已过期");
        }

        // 如果是登录用户，则记录已读（使用默认源库）
        try {
            User user = authService.requireLoginUser();
            // 数据库触发器自动记录阅读
            noticeReadService.markAsRead(id, user.getId(), "PC");

        } catch (UnauthenticatedException e) {
            // 未登录则忽略已读记录
            log.debug("未登录用户访问公告详情，不记录已读：noticeId={}", id);
        }

        return Result.success(notice);
    }
}






