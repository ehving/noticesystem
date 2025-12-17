package com.notice.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.Notice;
import com.notice.system.service.base.MultiDbSyncService;
import com.notice.system.sync.DatabaseType;
import com.notice.system.vo.notice.NoticeAdminPageVo;

import java.util.Collection;

/**
 * 公告服务：
 *  - 提供多库同步的基础 CRUD
 *  - 封装公告 + 部门关联的业务操作
 */
public interface NoticeService extends MultiDbSyncService<Notice> {

    /**
     * 使用默认源库创建公告并关联目标部门
     */
    void createNotice(Notice notice, Collection<String> targetDeptIds);

    /**
     * 在指定源库中创建公告并关联目标部门，
     * 然后以该源库为基准同步到其它数据库。
     */
    void createNoticeInDb(DatabaseType sourceDb,
                          Notice notice,
                          Collection<String> targetDeptIds);

    /**
     * 使用默认源库更新公告及其目标部门关联
     */
    void updateNotice(Notice notice, Collection<String> targetDeptIds);

    /**
     * 在指定源库中更新公告及其目标部门关联，
     * 然后以该源库为基准同步到其它数据库。
     */
    void updateNoticeInDb(DatabaseType sourceDb,
                          Notice notice,
                          Collection<String> targetDeptIds);

    /**
     * 使用默认源库撤回公告（仅修改状态为 RECALLED）
     */
    void recallNotice(String noticeId);

    /**
     * 在指定源库中撤回公告，并以该源库为基准同步到其它数据库。
     */
    void recallNoticeInDb(DatabaseType sourceDb, String noticeId);

    /* ==================== 带条件分页 ==================== */

    /**
     * 用户侧：分页查询已发布且在有效期内的公告（默认库）。
     *
     * @param pageNo   页码
     * @param pageSize 页大小
     * @param keyword  标题/内容关键字（可空）
     * @param level    公告等级 NORMAL/IMPORTANT/URGENT（可空）
     */
    Page<Notice> pagePublishedForUser(long pageNo,
                                      long pageSize,
                                      String keyword,
                                      String level);

    /**
     * 管理端：使用默认库按条件分页查询公告。
     */
    Page<Notice> pageAdminNotices(NoticeAdminPageVo vo);

    /**
     * 管理端：使用指定库按条件分页查询公告。
     */
    Page<Notice> pageAdminNoticesInDb(DatabaseType db, NoticeAdminPageVo vo);
}






