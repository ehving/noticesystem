package com.notice.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.NoticeRead;
import com.notice.system.service.base.MultiDbSyncService;
import com.notice.system.entityEnum.DatabaseType;

public interface NoticeReadService extends MultiDbSyncService<NoticeRead> {

    /**使用默认源库标记已读（幂等）*/
    void markAsRead(String noticeId, String userId, String deviceType);

    /**在指定源库标记已读（幂等），并以该库为源库进行多库同步*/
    void markAsReadInDb(DatabaseType db, String noticeId, String userId, String deviceType);

    /**某用户是否已阅读某条公告（默认库）*/
    boolean hasRead(String noticeId, String userId);

    /**某用户是否已阅读某条公告（指定库）*/
    boolean hasReadInDb(DatabaseType db, String noticeId, String userId);

    /**查询某条公告的已读人数（默认库）*/
    long countRead(String noticeId);

    /**查询某条公告的已读人数（指定库）*/
    long countReadInDb(DatabaseType db, String noticeId);

    /**管理端：分页查询某条公告的阅读记录（指定日志库）*/
    Page<NoticeRead> pageNoticeReadsInDb(DatabaseType db,
                                         String noticeId,
                                         long pageNo,
                                         long pageSize);
}

