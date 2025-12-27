package com.notice.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.SyncConflict;
import com.notice.system.entityEnum.aggBy.SyncConflictAggBy;
import com.notice.system.service.base.MultiDbSyncService;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncAction;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.vo.conflict.*;
import com.notice.system.vo.report.AggVo;

import java.time.LocalDateTime;
import java.util.List;

public interface SyncConflictService extends MultiDbSyncService<SyncConflict> {

    /**
     * 同步成功后校验：
     * - 如不一致：upsert conflict + items，并按冷却策略触发邮件
     * - 返回 具体syncConflictID 表示“发现冲突”,无冲突则为null
     */
    String checkAndUpsertConflictIfNeeded(SyncEntityType entityType,
                                           String entityId,
                                           SyncAction action,
                                           DatabaseType sourceDb);

    /**
     * 管理员修复：选择某库作为来源覆盖其它库
     * （你后面接 controller 时用）
     */
    boolean resolveConflict(String conflictId, DatabaseType sourceDb, String note);

    boolean ignoreConflict(String conflictId, String note);

    // ====== 前端：列表/详情 ======
    Page<SyncConflictWithItemsVo> pageConflicts(DatabaseType db, SyncConflictQueryVo query);

    SyncConflictDetailVo getConflictDetail(DatabaseType db, String conflictId);

    // ====== 管理员动作增强 ======
    boolean reopenConflict(String conflictId, String note, LocalDateTime now);

    boolean recheckConflict(String conflictId, LocalDateTime now);

    // ====== 定时任务 ======
    //发现新冲突，返回发现冲突的数量
    int detectNewConflictsFromSuccessLogs(LocalDateTime fromTime,
                                          LocalDateTime toTime,
                                          int perDbLogLimit,
                                          int entityLimit);

    //复查旧冲突
    int recheckOpenConflicts(LocalDateTime now,int limit);
    //发送邮件处理
    int notifyPendingConflicts(LocalDateTime now,int limit);

    // ====== 报表聚合 ======
    List<AggVo> aggConflicts(
            DatabaseType db,
            LocalDateTime begin,
            LocalDateTime end,
            SyncConflictAggBy by,
            SyncConflictQueryVo filter
    );


    List<AggVo> aggOpenByEntityType(DatabaseType db, LocalDateTime begin, LocalDateTime end);

    List<AggVo> aggOpenByConflictType(DatabaseType db, LocalDateTime begin, LocalDateTime end);

    SyncConflictDetailVo getDetailWithRows(DatabaseType db, String conflictId);
}
