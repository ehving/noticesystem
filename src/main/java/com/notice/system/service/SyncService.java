package com.notice.system.service;

import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncAction;
import com.notice.system.sync.SyncEntityType;

import java.util.Collection;

public interface SyncService {

    /**
     * 提交一次同步任务：
     *  - 由某个源库（sourceDb）发起
     *  - 通常会对所有「目标库」依次执行同步
     * 典型调用场景：
     *  - 业务 Service 在单条 CRUD 成功后调用
     *  - 冲突处理时，管理员选择某个库为「正确数据来源」后调用
     */
    void submitSync(SyncEntityType entityType,
                    String entityId,
                    SyncAction action,
                    DatabaseType sourceDb);

    /**
     * 提交一批同步任务：
     *  - 同一实体类型 / 同一源库 / 同一动作
     *  - 多个主键 ID
     * 典型调用场景：
     *  - 批量导入公告 / 用户等业务
     *  - 批量修复某一批数据
     */
    void submitBatchSync(SyncEntityType entityType,
                         Collection<String> entityIds,
                         SyncAction action,
                         DatabaseType sourceDb);

    /**
     * 从指定源库同步一条数据到「指定目标库」。
     * 相比 submitSync（源库 -> 所有其它库），这个接口更细粒度：
     *  - 只对一个目标库执行同步
     * 典型调用场景：
     *  - 某个目标库曾经宕机，只需要补齐这一个库的数据
     *  - 冲突处理时，管理员只希望「用 PG 覆盖 SQLSERVER」，而不是推给所有库
     *  - SyncLogService / 冲突服务在做「重试 / 修复」时，精确控制来源和目标
     */
    boolean syncToTarget(SyncEntityType entityType,
                      String entityId,
                      SyncAction action,
                      DatabaseType sourceDb,
                      DatabaseType targetDb);
    boolean syncToTargetWithoutLog(SyncEntityType entityType,
                                   String entityId,
                                   SyncAction action,
                                   DatabaseType sourceDb,
                                   DatabaseType targetDb);


    /**
     * 对某一种实体做「全量同步」，从一个源库推送到其它数据库。
     * 典型调用场景：
     *  - 第一次初始化某张表（例如：ROLE、SYSTEM_CONFIG）
     *  - 管理端页面针对某个实体类型点「全量同步」
     */
    void fullSyncEntityFromSource(SyncEntityType entityType,
                                  DatabaseType sourceDb);

    /**
     * 对系统中所有支持同步的实体，做一次「全量同步」，
     * 从一个源库推送到其它数据库。
     * 典型调用场景：
     *  - 管理端「一键全量同步」按钮
     *  - 定时任务（每日凌晨跑一次全量校正）
     */
    void fullSyncAllFromSource(DatabaseType sourceDb);
}



