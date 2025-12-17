package com.notice.system.service;

import com.notice.system.entity.NoticeTargetDept;
import com.notice.system.service.base.MultiDbSyncService;
import com.notice.system.sync.DatabaseType;

import java.util.List;

/**
 * 公告-部门关联服务：
 *  - 记录公告发送到哪些部门
 *  - 所有写操作走 MultiDbSyncService 带多库同步
 */
public interface NoticeTargetDeptService extends MultiDbSyncService<NoticeTargetDept> {

    /**
     * 根据公告 ID 查询所有目标部门关联记录（默认从 defaultDb 查）
     */
    List<NoticeTargetDept> listByNoticeId(String noticeId);

    /**
     * 根据公告 ID 查询所有目标部门关联记录（从指定库查）
     */
    List<NoticeTargetDept> listByNoticeIdFromDb(DatabaseType db, String noticeId);

    /**
     * 删除某个公告的所有部门关联（默认以 defaultDb 为源库进行同步）
     */
    void removeByNoticeId(String noticeId);

    /**
     * 删除某个公告的所有部门关联，
     * 以指定源库为基准进行多库同步。
     */
    void removeByNoticeIdInDb(DatabaseType db, String noticeId);
}


