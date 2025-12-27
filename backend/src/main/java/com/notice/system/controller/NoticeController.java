package com.notice.system.controller;

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
 * 公告接口（用户端）
 * 路径前缀：/api/notices
 * 说明：
 *  - 不提供选库参数，统一使用默认库
 *  - /page 匿名可访问
 *  - /{id} 匿名可访问；若已登录则自动记录已读
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
     * 分页查询已发布且在有效期内的公告（匿名可访问）
     *
     * @param pageNo   页码，默认 1
     * @param pageSize 每页大小，默认 10
     * @param keyword  标题/内容关键字（可空）
     * @param level    公告等级 NORMAL/IMPORTANT/URGENT（可空）
     */
    @GetMapping("/page")
    public Result<Page<Notice>> pageNotices(
            @RequestParam(name = "pageNo", required = false, defaultValue = "1") long pageNo,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") long pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "level", required = false) String level
    ) {
        return Result.success(noticeService.pagePublishedForUser(pageNo, pageSize, keyword, level));
    }

    /**
     * 公告详情（匿名可访问）
     * 规则：
     *  - 仅允许查看已发布且在有效期内的公告
     *  - 若用户已登录：自动记录已读（deviceType 固定为 "PC"）
     */
    @GetMapping("/{id}")
    public Result<Notice> getNoticeDetail(@PathVariable("id") String id) {
        Notice notice = noticeService.getById(id);
        if (notice == null) {
            return Result.fail("公告不存在");
        }

        String invalidMsg = validateReadable(notice);
        if (invalidMsg != null) {
            return Result.fail(invalidMsg);
        }

        // 登录用户：记录已读；未登录：忽略
        try {
            User user = authService.requireLoginUser();
            noticeReadService.markAsRead(id, user.getId(), "PC");
        } catch (UnauthenticatedException e) {
            log.debug("未登录用户访问公告详情，不记录已读：noticeId={}", id);
        }

        return Result.success(notice);
    }

    /**
     * 校验公告是否允许用户查看：
     *  - 必须 PUBLISHED
     *  - publishTime <= now
     *  - expireTime == null 或 expireTime > now
     *
     * @return null 表示可读；非 null 表示错误信息
     */
    private String validateReadable(Notice notice) {
        if (!"PUBLISHED".equalsIgnoreCase(notice.getStatus())) {
            return "公告未发布或已被撤回";
        }

        LocalDateTime now = LocalDateTime.now();

        if (notice.getPublishTime() != null && notice.getPublishTime().isAfter(now)) {
            return "公告尚未生效";
        }

        if (notice.getExpireTime() != null && notice.getExpireTime().isBefore(now)) {
            return "公告已过期";
        }

        return null;
    }
}







