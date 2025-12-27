package com.notice.system.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.common.Result;
import com.notice.system.converter.NoticeReadConverter;
import com.notice.system.entity.Dept;
import com.notice.system.entity.Notice;
import com.notice.system.entity.NoticeRead;
import com.notice.system.entity.User;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.service.AuthService;
import com.notice.system.service.DeptService;
import com.notice.system.service.NoticeReadService;
import com.notice.system.service.NoticeService;
import com.notice.system.service.UserService;
import com.notice.system.vo.noticeread.NoticeReadUserVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 公告阅读数据（管理端）
 * 路径前缀：/api/admin/notices
 * 权限：管理员
 * 选库：?db=MYSQL/PG/SQLSERVER（默认 noticeService.defaultDb()）
 * 功能：
 *  - 查询某条公告已读人数
 *  - 分页查询某条公告的已读用户列表
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class NoticeReadAdminController {

    private final AuthService authService;
    private final NoticeService noticeService;
    private final NoticeReadService noticeReadService;
    private final UserService userService;
    private final DeptService deptService;

    /**
     * 获取某条公告的已读人数
     * 返回：countRead(noticeId)
     */
    @GetMapping("/{noticeId}/read-count")
    public Result<Long> getReadCount(
            @PathVariable("noticeId") String noticeId,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = (db == null ? noticeService.defaultDb() : db);
        authService.requireAdmin(useDb);

        if (noticeId == null || noticeId.isBlank()) {
            return Result.fail("noticeId 不能为空");
        }

        Notice notice = noticeService.getById(useDb, noticeId);
        if (notice == null) {
            return Result.fail("公告不存在");
        }

        long count = noticeReadService.countReadInDb(useDb, noticeId);
        return Result.success(count);
    }

    /**
     * 分页查询某条公告的阅读记录（已读用户列表）
     * 返回：Page<NoticeReadUserVo>
     */
    @GetMapping("/{noticeId}/reads")
    public Result<Page<NoticeReadUserVo>> pageNoticeReads(
            @PathVariable("noticeId") String noticeId,
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestParam(name = "pageNo", required = false, defaultValue = "1") long pageNo,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") long pageSize
    ) {
        DatabaseType useDb = (db == null ? noticeService.defaultDb() : db);
        authService.requireAdmin(useDb);

        if (noticeId == null || noticeId.isBlank()) {
            return Result.fail("noticeId 不能为空");
        }

        Notice notice = noticeService.getById(useDb, noticeId);
        if (notice == null) {
            return Result.fail("公告不存在");
        }

        // 1) 先查 NoticeRead 分页
        Page<NoticeRead> page = noticeReadService.pageNoticeReadsInDb(useDb, noticeId, pageNo, pageSize);

        // 2) 组装 VO（项目规模小：循环 getById 足够；如要优化可批量查用户/部门）
        List<NoticeReadUserVo> voList = new ArrayList<>();
        for (NoticeRead read : page.getRecords()) {
            if (read == null) continue;

            User user = userService.getById(useDb, read.getUserId());
            Dept dept = (user == null || user.getDeptId() == null) ? null : deptService.getById(useDb, user.getDeptId());

            voList.add(NoticeReadConverter.toUserReadVo(read, user, dept));
        }

        // 3) 返回 Page<VO>
        Page<NoticeReadUserVo> voPage = new Page<>(pageNo, pageSize, page.getTotal());
        voPage.setRecords(voList);
        return Result.success(voPage);
    }
}


