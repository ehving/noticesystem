package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.NoticeTargetDept;
import com.notice.system.service.NoticeTargetDeptService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncEntityType;
import com.notice.system.sync.SyncMetadataRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 公告-部门关联服务实现
 */
@Slf4j
@Service
public class NoticeTargetDeptServiceImpl
        extends MultiDbSyncServiceImpl<NoticeTargetDept>
        implements NoticeTargetDeptService {

    public NoticeTargetDeptServiceImpl(SyncService syncService,
                                       SyncMetadataRegistry metadataRegistry) {
        super(syncService, metadataRegistry,
                SyncEntityType.NOTICE_TARGET_DEPT, DatabaseType.MYSQL);
    }

    @Override
    public List<NoticeTargetDept> listByNoticeId(String noticeId) {
        return listByNoticeIdFromDb(defaultDb(), noticeId);
    }

    @Override
    public List<NoticeTargetDept> listByNoticeIdFromDb(DatabaseType db, String noticeId) {
        if (noticeId == null || noticeId.isBlank()) {
            return Collections.emptyList();
        }

        SyncMetadataRegistry.EntitySyncDefinition<NoticeTargetDept> def =
                metadataRegistry.getDefinition(SyncEntityType.NOTICE_TARGET_DEPT);
        if (def == null) {
            log.warn("[NOTICE_TARGET_DEPT] 未在 SyncMetadataRegistry 中找到定义");
            return Collections.emptyList();
        }

        BaseMapper<NoticeTargetDept> mapper = def.getMapper(db);
        if (mapper == null) {
            log.warn("[NOTICE_TARGET_DEPT] 未找到指定库的 Mapper，db={}", db);
            return Collections.emptyList();
        }

        LambdaQueryWrapper<NoticeTargetDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeTargetDept::getNoticeId, noticeId);
        return mapper.selectList(wrapper);
    }

    @Override
    public void removeByNoticeId(String noticeId) {
        removeByNoticeIdInDb(defaultDb(), noticeId);
    }

    @Override
    public void removeByNoticeIdInDb(DatabaseType db, String noticeId) {
        if (noticeId == null || noticeId.isBlank()) {
            return;
        }

        SyncMetadataRegistry.EntitySyncDefinition<NoticeTargetDept> def =
                metadataRegistry.getDefinition(SyncEntityType.NOTICE_TARGET_DEPT);
        if (def == null) {
            log.warn("[NOTICE_TARGET_DEPT] 未在 SyncMetadataRegistry 中找到定义");
            return;
        }

        BaseMapper<NoticeTargetDept> mapper = def.getMapper(db);
        if (mapper == null) {
            log.warn("[NOTICE_TARGET_DEPT] 未找到指定库的 Mapper，db={}", db);
            return;
        }

        LambdaQueryWrapper<NoticeTargetDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeTargetDept::getNoticeId, noticeId);
        List<NoticeTargetDept> list = mapper.selectList(wrapper);

        if (list == null || list.isEmpty()) {
            log.debug("[NOTICE_TARGET_DEPT] 无关联记录可删除，noticeId={}，db={}", noticeId, db);
            return;
        }

        // 关键：对每条记录调用 removeByIdInDb，写操作走同步链路
        for (NoticeTargetDept ntd : list) {
            this.removeByIdInDb(db, ntd.getId());
        }

        log.info("[NOTICE_TARGET_DEPT] 已删除公告的所有部门关联，noticeId={}，db={}，count={}",
                noticeId, db, list.size());
    }
}


