package com.notice.system.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.common.Result;
import com.notice.system.converter.NoticeReadConverter;
import com.notice.system.entity.Dept;
import com.notice.system.entity.Notice;
import com.notice.system.entity.NoticeRead;
import com.notice.system.entity.User;
import com.notice.system.service.AuthService;
import com.notice.system.service.DeptService;
import com.notice.system.service.NoticeReadService;
import com.notice.system.service.NoticeService;
import com.notice.system.service.UserService;
import com.notice.system.sync.DatabaseType;
import com.notice.system.vo.noticeread.NoticeReadUserVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

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
     */
    @GetMapping("/{noticeId}/read-count")
    public Result<Long> getReadCount(
            @PathVariable("noticeId") String noticeId,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        authService.requireAdmin();

        // 可选：先校验公告是否存在
        Notice notice = noticeService.getById(noticeId);
        if (notice == null) {
            return Result.fail("公告不存在");
        }

        DatabaseType useDb = (db == null ? noticeReadService.defaultDb() : db);
        long count = noticeReadService.countReadInDb(useDb, noticeId);
        return Result.success(count);
    }

    /**
     * 分页查询某条公告的阅读记录（已读用户列表）
     */
    @GetMapping("/{noticeId}/reads")
    public Result<Page<NoticeReadUserVo>> pageNoticeReads(
            @PathVariable("noticeId") String noticeId,
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestParam(name = "pageNo", required = false, defaultValue = "1") long pageNo,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") long pageSize
    ) {
        authService.requireAdmin();

        Notice notice = noticeService.getById(noticeId);
        if (notice == null) {
            return Result.fail("公告不存在");
        }

        DatabaseType useDb = (db == null ? noticeReadService.defaultDb() : db);

        // 1. 先查 NoticeRead 分页
        Page<NoticeRead> page = noticeReadService.pageNoticeReadsInDb(useDb, noticeId, pageNo, pageSize);

        // 2. 组装 VO（简单版：循环 getById，考虑你的项目规模，这样足够）
        List<NoticeReadUserVo> voList = new ArrayList<>();
        for (NoticeRead read : page.getRecords()) {
            if (read == null) {
                continue;
            }
            User user = userService.getById(useDb, read.getUserId());
            Dept dept = null;
            if (user != null && user.getDeptId() != null) {
                dept = deptService.getById(useDb, user.getDeptId());
            }
            NoticeReadUserVo vo = NoticeReadConverter.toUserReadVo(read, user, dept);
            if (vo != null) {
                voList.add(vo);
            }
        }

        // 3. 填充到新的 Page<VO> 中返回
        Page<NoticeReadUserVo> voPage = new Page<>(pageNo, pageSize, page.getTotal());
        voPage.setRecords(voList);

        return Result.success(voPage);
    }
}

