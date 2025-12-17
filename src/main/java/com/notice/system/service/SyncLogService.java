package com.notice.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.SyncLog;
import com.notice.system.service.base.MultiDbSyncService;
import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncAction;
import com.notice.system.sync.SyncEntityType;
import com.notice.system.vo.synclog.SyncLogDailyReportVo;
import com.notice.system.vo.synclog.SyncLogVo;

import java.util.List;

/**
 * 多库同步日志服务：
 *  - 继承通用 MultiDbSyncService，具备“可选库 + 自动同步”的 CRUD 能力
 *  - 额外提供 recordSuccess/recordFailure、分页查询等日志特有方法
 */
public interface SyncLogService extends MultiDbSyncService<SyncLog> {

    /**
     * 记录一次同步成功。
     * 日志会写入默认库（一般为 MYSQL），并通过同步机制同步到其他库。
     */
    void recordSuccess(SyncEntityType entityType,
                       String entityId,
                       SyncAction action,
                       DatabaseType sourceDb,
                       DatabaseType targetDb);

    /**
     * 记录一次同步失败。
     * 日志会写入默认库（一般为 MYSQL），并通过同步机制同步到其他库。
     */
    void recordFailure(SyncEntityType entityType,
                       String entityId,
                       SyncAction action,
                       DatabaseType sourceDb,
                       DatabaseType targetDb,
                       String errorMsg);

    /**
     * 分页查询同步日志。
     *
     * @param logDb 从哪个数据库查询日志（MYSQL / PG / SQLSERVER），为空则默认 MYSQL
     * @param vo    查询条件 + 分页参数
     */
    Page<SyncLog> pageLogs(DatabaseType logDb, SyncLogVo vo);

    /**
     * 从指定日志库中按主键查询单条日志
     */
    SyncLog getById(DatabaseType logDb, String id);

    /**
     * 将指定日志记录的重试次数 +1，并同步到其它库
     */
    void incrementRetryCount(DatabaseType logDb, String logId);

    /**
     * 根据日志记录重新发起一次同步，并将该日志的重试次数 +1。
     *
     * @param logDb 从哪个库读取日志记录（为 null 则使用默认库）
     * @param logId 日志主键 ID
     * @return true 表示已成功提交重试任务并更新重试次数；false 表示日志不存在或解析失败
     */
    boolean retrySyncByLogId(DatabaseType logDb, String logId);

    /**
     * 按天统计同步日志（可选库）
     */
    List<SyncLogDailyReportVo> listDailyReport(DatabaseType logDb, SyncLogVo condition);

    /**
     * 清理某个库的同步日志（调用数据库存储过程）：
     *  - retainDays 为 null 或 <=0 时使用默认值（如 90）
     *  - maxCount   为 null 或 <=0 时使用默认值（如 100000）
     * @return 实际删除的条数（仅用于统计展示）
     */
    long cleanLogsInDb(DatabaseType db, Integer retainDays, Long maxCount);

}


