package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.Notice;
import com.notice.system.entity.NoticeTargetDept;
import com.notice.system.service.NoticeService;
import com.notice.system.service.NoticeTargetDeptService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncEntityType;
import com.notice.system.sync.SyncMetadataRegistry;
import com.notice.system.vo.notice.NoticeAdminPageVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 公告服务实现
 */
@Slf4j
@Service
public class NoticeServiceImpl
        extends MultiDbSyncServiceImpl<Notice>
        implements NoticeService {


    private final NoticeTargetDeptService noticeTargetDeptService;

    public NoticeServiceImpl(SyncService syncService,
                             SyncMetadataRegistry metadataRegistry,
                             NoticeTargetDeptService noticeTargetDeptService) {
        super(syncService, metadataRegistry,
                SyncEntityType.NOTICE, DatabaseType.MYSQL);
        this.noticeTargetDeptService = noticeTargetDeptService;
    }

    @Override
    //@Transactional
    public void createNotice(Notice notice, Collection<String> targetDeptIds) {
        createNoticeInDb(defaultDb(), notice, targetDeptIds);
    }

    @Override
    //@Transactional
    public void createNoticeInDb(DatabaseType sourceDb,
                                 Notice notice,
                                 Collection<String> targetDeptIds) {
        LocalDateTime now = LocalDateTime.now();

        if (notice.getCreateTime() == null) {
            notice.setCreateTime(now);
        }
        notice.setUpdateTime(now);

        // 默认值处理：状态/等级/浏览数
        if (notice.getStatus() == null || notice.getStatus().isBlank()) {
            notice.setStatus("DRAFT");
        }
        if (notice.getLevel() == null || notice.getLevel().isBlank()) {
            notice.setLevel("NORMAL");
        }
        if (notice.getViewCount() == null) {
            notice.setViewCount(0L);
        }

        // 1. 在指定源库中保存公告，并以该库为源库同步到其它数据库
        this.saveInDb(sourceDb, notice);

        // 2. 在指定源库中保存公告-部门关联（每条也走带同步的 saveInDb）
        if (targetDeptIds != null) {
            for (String deptId : targetDeptIds) {
                NoticeTargetDept ntd = new NoticeTargetDept();
                ntd.setNoticeId(notice.getId());
                ntd.setDeptId(deptId);
                noticeTargetDeptService.saveInDb(sourceDb, ntd);
            }
        }

        log.info("[NOTICE] 创建公告完成，id={}，sourceDb={}，targetDeptCount={}",
                notice.getId(),
                sourceDb,
                targetDeptIds == null ? 0 : targetDeptIds.size());
    }

    @Override
    //@Transactional
    public void updateNotice(Notice notice, Collection<String> targetDeptIds) {
        updateNoticeInDb(defaultDb(), notice, targetDeptIds);
    }

    @Override
    //@Transactional
    public void updateNoticeInDb(DatabaseType sourceDb,
                                 Notice notice,
                                 Collection<String> targetDeptIds) {
        LocalDateTime now = LocalDateTime.now();
        notice.setUpdateTime(now);

        // 1. 在指定源库中更新公告，并同步到其它数据库
        this.updateByIdInDb(sourceDb, notice);

        // 2. 删除该公告在指定源库中的旧关联（内部也会以 sourceDb 为源库同步删除）
        noticeTargetDeptService.removeByNoticeIdInDb(sourceDb, notice.getId());

        // 3. 新建关联
        if (targetDeptIds != null) {
            for (String deptId : targetDeptIds) {
                NoticeTargetDept ntd = new NoticeTargetDept();
                ntd.setNoticeId(notice.getId());
                ntd.setDeptId(deptId);
                noticeTargetDeptService.saveInDb(sourceDb, ntd);
            }
        }

        log.info("[NOTICE] 更新公告完成，id={}，sourceDb={}，targetDeptCount={}",
                notice.getId(),
                sourceDb,
                targetDeptIds == null ? 0 : targetDeptIds.size());
    }

    @Override
    //@Transactional
    public void recallNotice(String noticeId) {
        recallNoticeInDb(defaultDb(), noticeId);
    }

    @Override
    //@Transactional
    public void recallNoticeInDb(DatabaseType sourceDb, String noticeId) {
        // 1. 从指定源库读取公告
        Notice dbNotice= this.getById(sourceDb, noticeId);

        if (dbNotice == null) {
            log.warn("[NOTICE] 撤回失败，公告不存在，id={}，sourceDb={}", noticeId, sourceDb);
            return;
        }

        dbNotice.setStatus("RECALLED");
        dbNotice.setUpdateTime(LocalDateTime.now());

        // 2. 在指定源库中更新，并同步到其它数据库
        this.updateByIdInDb(sourceDb, dbNotice);

        log.info("[NOTICE] 已撤回公告，id={}，sourceDb={}", noticeId, sourceDb);
    }

    @Override
    public Page<Notice> pagePublishedForUser(long pageNo,
                                             long pageSize,
                                             String keyword,
                                             String level) {

        DatabaseType db = defaultDb();
        BaseMapper<Notice> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[NOTICE] pagePublishedForUser 未找到 mapper，db={}", db);
            return new Page<>(pageNo, pageSize);
        }

        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();

        // 已发布
        wrapper.eq(Notice::getStatus, "PUBLISHED");

        // 有效期内
        LocalDateTime now = LocalDateTime.now();
        wrapper.le(Notice::getPublishTime, now);
        wrapper.and(w -> w.isNull(Notice::getExpireTime)
                .or()
                .gt(Notice::getExpireTime, now));

        // 关键字
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Notice::getTitle, kw)
                    .or()
                    .like(Notice::getContent, kw));
        }

        // 等级
        if (level != null && !level.isBlank()) {
            wrapper.eq(Notice::getLevel, level.trim());
        }

        wrapper.orderByDesc(Notice::getPublishTime);

        Page<Notice> page = new Page<>(pageNo, pageSize);
        return mapper.selectPage(page, wrapper);
    }

    @Override
    public Page<Notice> pageAdminNotices(NoticeAdminPageVo vo) {
        return pageAdminNoticesInDb(defaultDb(), vo);
    }

    @Override
    public Page<Notice> pageAdminNoticesInDb(DatabaseType db, NoticeAdminPageVo vo) {

        long pageNo = (vo.getPageNo() <= 0 ? 1 : vo.getPageNo());
        long pageSize = (vo.getPageSize() <= 0 ? 10 : vo.getPageSize());

        BaseMapper<Notice> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[NOTICE] pageAdminNoticesInDb 未找到 mapper，db={}", db);
            return new Page<>(pageNo, pageSize);
        }

        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();

        if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
            String kw = vo.getKeyword().trim();
            wrapper.and(w -> w.like(Notice::getTitle, kw)
                    .or()
                    .like(Notice::getContent, kw));
        }

        if (vo.getStatus() != null && !vo.getStatus().isBlank()) {
            wrapper.eq(Notice::getStatus, vo.getStatus().trim());
        }

        if (vo.getLevel() != null && !vo.getLevel().isBlank()) {
            wrapper.eq(Notice::getLevel, vo.getLevel().trim());
        }

        if (vo.getPublisherId() != null && !vo.getPublisherId().isBlank()) {
            wrapper.eq(Notice::getPublisherId, vo.getPublisherId().trim());
        }

        if (vo.getStartTime() != null) {
            wrapper.ge(Notice::getPublishTime, vo.getStartTime());
        }
        if (vo.getEndTime() != null) {
            wrapper.le(Notice::getPublishTime, vo.getEndTime());
        }

        wrapper.orderByDesc(Notice::getCreateTime);

        Page<Notice> page = new Page<>(pageNo, pageSize);
        return mapper.selectPage(page, wrapper);
    }
}




