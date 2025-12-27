package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.NoticeRead;
import com.notice.system.service.NoticeReadService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.sync.SyncMetadataRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 公告阅读记录服务实现
 */
@Slf4j
@Service
public class NoticeReadServiceImpl
        extends MultiDbSyncServiceImpl<NoticeRead>
        implements NoticeReadService {

    public NoticeReadServiceImpl(SyncService syncService,
                                 SyncMetadataRegistry metadataRegistry) {
        super(syncService, metadataRegistry,
                SyncEntityType.NOTICE_READ, DatabaseType.MYSQL);
    }

    @Override
    public void markAsRead(String noticeId, String userId, String deviceType) {
        markAsReadInDb(defaultDb(), noticeId, userId, deviceType);
    }

    /**
     * 标记已读（幂等）：
     * - 先查是否存在 noticeId+userId 的记录，存在则直接返回
     * - 不存在则插入，并触发多库同步
     * - 并发情况下可能出现重复插入冲突：插入失败时按“已读”处理即可（幂等语义）
     */
    @Override
    public void markAsReadInDb(DatabaseType db,
                               String noticeId,
                               String userId,
                               String deviceType) {
        if (isBlank(noticeId) || isBlank(userId)) return;

        DatabaseType useDb = (db == null ? defaultDb() : db);
        BaseMapper<NoticeRead> mapper = resolveMapper(useDb);

        // 1) 幂等：已存在则直接返回
        NoticeRead existing = mapper.selectOne(new LambdaQueryWrapper<NoticeRead>()
                .eq(NoticeRead::getNoticeId, noticeId)
                .eq(NoticeRead::getUserId, userId)
                .last("LIMIT 1"));
        if (existing != null) return;

        // 2) 不存在则插入（走同步链路）
        NoticeRead record = new NoticeRead();
        record.setNoticeId(noticeId);
        record.setUserId(userId);
        record.setReadTime(LocalDateTime.now());
        record.setDeviceType(deviceType);

        try {
            saveInDb(useDb, record);
        } catch (Exception ex) {
            // 并发/唯一键冲突等：幂等语义下视为“已读”，避免重复报错影响用户体验
            log.debug("[NOTICE_READ] markAsRead ignore duplicate/exception, noticeId={}, userId={}, db={}, err={}",
                    noticeId, userId, useDb, ex.getMessage());
        }
    }

    @Override
    public boolean hasRead(String noticeId, String userId) {
        return hasReadInDb(defaultDb(), noticeId, userId);
    }

    @Override
    public boolean hasReadInDb(DatabaseType db, String noticeId, String userId) {
        if (isBlank(noticeId) || isBlank(userId)) return false;

        DatabaseType useDb = (db == null ? defaultDb() : db);
        BaseMapper<NoticeRead> mapper = resolveMapper(useDb);

        Long count = mapper.selectCount(new LambdaQueryWrapper<NoticeRead>()
                .eq(NoticeRead::getNoticeId, noticeId)
                .eq(NoticeRead::getUserId, userId));
        return count != null && count > 0;
    }

    @Override
    public long countRead(String noticeId) {
        return countReadInDb(defaultDb(), noticeId);
    }

    @Override
    public long countReadInDb(DatabaseType db, String noticeId) {
        if (isBlank(noticeId)) return 0L;

        DatabaseType useDb = (db == null ? defaultDb() : db);
        BaseMapper<NoticeRead> mapper = resolveMapper(useDb);

        Long count = mapper.selectCount(new LambdaQueryWrapper<NoticeRead>()
                .eq(NoticeRead::getNoticeId, noticeId));
        return count == null ? 0L : count;
    }

    @Override
    public Page<NoticeRead> pageNoticeReadsInDb(DatabaseType db,
                                                String noticeId,
                                                long pageNo,
                                                long pageSize) {
        long pn = pageNo <= 0 ? 1 : pageNo;
        long ps = pageSize <= 0 ? 10 : pageSize;
        if (isBlank(noticeId)) return new Page<>(pn, ps);

        DatabaseType useDb = (db == null ? defaultDb() : db);
        BaseMapper<NoticeRead> mapper = resolveMapper(useDb);

        return mapper.selectPage(new Page<>(pn, ps),
                new LambdaQueryWrapper<NoticeRead>()
                        .eq(NoticeRead::getNoticeId, noticeId)
                        .orderByDesc(NoticeRead::getReadTime));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}



