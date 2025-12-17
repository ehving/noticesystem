package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.NoticeRead;
import com.notice.system.service.NoticeReadService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncEntityType;
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

    @Override
    public void markAsReadInDb(DatabaseType db,
                               String noticeId,
                               String userId,
                               String deviceType) {
        if (noticeId == null || userId == null) {
            return;
        }

        SyncMetadataRegistry.EntitySyncDefinition<NoticeRead> def =
                metadataRegistry.getDefinition(SyncEntityType.NOTICE_READ);
        if (def == null) {
            log.warn("[NOTICE_READ] 未在 SyncMetadataRegistry 中找到定义");
            return;
        }

        BaseMapper<NoticeRead> mapper = def.getMapper(db);
        if (mapper == null) {
            log.warn("[NOTICE_READ] 未找到指定库的 Mapper，db={}", db);
            return;
        }

        // 1. 检查是否已经存在阅读记录（幂等）
        LambdaQueryWrapper<NoticeRead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeRead::getNoticeId, noticeId)
                .eq(NoticeRead::getUserId, userId);
        NoticeRead existing = mapper.selectOne(wrapper);
        if (existing != null) {
            // 已存在，视为已读，不重复写入
            return;
        }

        // 2. 不存在则写入，并以 db 为源库进行多库同步
        NoticeRead record = new NoticeRead();
        record.setNoticeId(noticeId);
        record.setUserId(userId);
        record.setReadTime(LocalDateTime.now());
        record.setDeviceType(deviceType);

        this.saveInDb(db, record);
    }

    @Override
    public boolean hasRead(String noticeId, String userId) {
        return hasReadInDb(defaultDb(), noticeId, userId);
    }

    @Override
    public boolean hasReadInDb(DatabaseType db,
                               String noticeId,
                               String userId) {
        if (noticeId == null || userId == null) {
            return false;
        }

        SyncMetadataRegistry.EntitySyncDefinition<NoticeRead> def =
                metadataRegistry.getDefinition(SyncEntityType.NOTICE_READ);
        if (def == null) {
            log.warn("[NOTICE_READ] 未在 SyncMetadataRegistry 中找到定义");
            return false;
        }

        BaseMapper<NoticeRead> mapper = def.getMapper(db);
        if (mapper == null) {
            log.warn("[NOTICE_READ] 未找到指定库的 Mapper，db={}", db);
            return false;
        }

        LambdaQueryWrapper<NoticeRead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeRead::getNoticeId, noticeId)
                .eq(NoticeRead::getUserId, userId);
        Long count = mapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    @Override
    public long countRead(String noticeId) {
        return countReadInDb(defaultDb(), noticeId);
    }

    @Override
    public long countReadInDb(DatabaseType db, String noticeId) {
        if (noticeId == null) {
            return 0L;
        }

        SyncMetadataRegistry.EntitySyncDefinition<NoticeRead> def =
                metadataRegistry.getDefinition(SyncEntityType.NOTICE_READ);
        if (def == null) {
            log.warn("[NOTICE_READ] 未在 SyncMetadataRegistry 中找到定义");
            return 0L;
        }

        BaseMapper<NoticeRead> mapper = def.getMapper(db);
        if (mapper == null) {
            log.warn("[NOTICE_READ] 未找到指定库的 Mapper，db={}", db);
            return 0L;
        }

        LambdaQueryWrapper<NoticeRead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeRead::getNoticeId, noticeId);
        Long count = mapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    @Override
    public Page<NoticeRead> pageNoticeReadsInDb(DatabaseType db,
                                                String noticeId,
                                                long pageNo,
                                                long pageSize) {
        if (noticeId == null || noticeId.isBlank()) {
            return new Page<>(pageNo, pageSize);
        }

        DatabaseType useDb = (db == null ? defaultDb() : db);

        SyncMetadataRegistry.EntitySyncDefinition<NoticeRead> def =
                metadataRegistry.getDefinition(SyncEntityType.NOTICE_READ);
        if (def == null) {
            log.warn("[NOTICE_READ] pageNoticeReadsInDb 未在 SyncMetadataRegistry 中找到定义");
            return new Page<>(pageNo, pageSize);
        }

        BaseMapper<NoticeRead> mapper = def.getMapper(useDb);
        if (mapper == null) {
            log.warn("[NOTICE_READ] pageNoticeReadsInDb 未找到指定库的 Mapper，db={}", useDb);
            return new Page<>(pageNo, pageSize);
        }

        Page<NoticeRead> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<NoticeRead> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeRead::getNoticeId, noticeId)
                .orderByDesc(NoticeRead::getReadTime);

        return mapper.selectPage(page, wrapper);
    }

}


