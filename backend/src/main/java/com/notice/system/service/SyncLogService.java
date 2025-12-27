package com.notice.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.SyncLog;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.aggBy.SyncLogAggBy;
import com.notice.system.service.base.MultiDbSyncService;
import com.notice.system.vo.report.AggVo;
import com.notice.system.vo.synclog.SyncLogDailyReportVo;
import com.notice.system.vo.synclog.SyncLogVo;

import java.time.LocalDateTime;
import java.util.List;

public interface SyncLogService extends MultiDbSyncService<SyncLog> {

    /** 按条件分页查询同步日志。 */
    Page<SyncLog> pageLogs(DatabaseType logDb, SyncLogVo vo);

    /** 从指定日志库按主键读取日志记录。 */
    SyncLog getById(DatabaseType logDb, String id);

    /** 将指定日志的 retryCount +1，并同步到其它库。 */
    void incrementRetryCount(DatabaseType logDb, String logId);

    /** 根据日志记录重试一次同步，并更新 retryCount 与状态。 */
    boolean retrySyncByLogId(DatabaseType logDb, String logId);

    /** 按天统计同步日志（用于报表）。 */
    List<SyncLogDailyReportVo> listDailyReport(DatabaseType logDb, SyncLogVo condition);

    /** 调用存储过程清理日志（按保留天数/最大数量）。 */
    long cleanLogsInDb(DatabaseType db, Integer retainDays, Long maxCount);

    /** 拉取最近 SUCCESS 日志并按 entityType+entityId 去重（用于冲突候选集扫描）。 */
    List<SyncLog> listRecentSuccessLogsDedup(LocalDateTime fromTime,
                                             LocalDateTime toTime,
                                             int perDbLimit,
                                             int entityLimit);

    /** 通用聚合统计接口（返回 key/count）。 */
    List<AggVo> aggLogs(DatabaseType db,
                        LocalDateTime begin,
                        LocalDateTime end,
                        SyncLogAggBy by,
                        SyncLogVo filter);
}



