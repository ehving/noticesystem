package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.NoticeTargetDept;
import com.notice.system.service.NoticeTargetDeptService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.sync.SyncMetadataRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
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
        if (isBlank(noticeId)) return Collections.emptyList();

        DatabaseType useDb = (db == null ? defaultDb() : db);
        BaseMapper<NoticeTargetDept> mapper = resolveMapper(useDb);

        return mapper.selectList(new LambdaQueryWrapper<NoticeTargetDept>()
                .eq(NoticeTargetDept::getNoticeId, noticeId));
    }

    @Override
    public List<NoticeTargetDept> listByNoticeIdsFromDb(DatabaseType db, Collection<String> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) return Collections.emptyList();

        DatabaseType useDb = (db == null ? defaultDb() : db);
        BaseMapper<NoticeTargetDept> mapper = resolveMapper(useDb);

        // 去空 + 去重，避免 IN () 带空值或重复
        List<String> ids = noticeIds.stream()
                .filter(x -> x != null && !x.isBlank())
                .distinct()
                .toList();
        if (ids.isEmpty()) return Collections.emptyList();

        return mapper.selectList(new LambdaQueryWrapper<NoticeTargetDept>()
                .in(NoticeTargetDept::getNoticeId, ids));
    }

    @Override
    public void removeByNoticeId(String noticeId) {
        removeByNoticeIdInDb(defaultDb(), noticeId);
    }

    /**
     * 删除某公告的全部范围关联：
     * - 先查出关联记录
     * - 再逐条走 removeByIdInDb（每条删除都会触发同步链路）
     */
    @Override
    public void removeByNoticeIdInDb(DatabaseType db, String noticeId) {
        if (isBlank(noticeId)) return;

        DatabaseType useDb = (db == null ? defaultDb() : db);
        BaseMapper<NoticeTargetDept> mapper = resolveMapper(useDb);

        List<NoticeTargetDept> list = mapper.selectList(new LambdaQueryWrapper<NoticeTargetDept>()
                .eq(NoticeTargetDept::getNoticeId, noticeId));

        if (list == null || list.isEmpty()) {
            log.debug("[NOTICE_TARGET_DEPT] nothing to remove, noticeId={}, db={}", noticeId, useDb);
            return;
        }

        for (NoticeTargetDept ntd : list) {
            if (ntd == null || isBlank(ntd.getId())) continue;
            removeByIdInDb(useDb, ntd.getId());
        }

        log.info("[NOTICE_TARGET_DEPT] removed scope relations, noticeId={}, db={}, count={}",
                noticeId, useDb, list.size());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
