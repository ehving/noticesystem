package com.notice.system.support.event;

import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncAction;
import com.notice.system.sync.SyncEntityType;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用于在同步完成后，通过事件机制通知日志服务记录日志。
 */
@Data
@AllArgsConstructor
public class SyncLogEvent {

    /**
     * 实体类型
     */
    private SyncEntityType entityType;

    /**
     * 实体主键 id
     */
    private String entityId;

    /**
     * 动作：CREATE / UPDATE / DELETE
     */
    private SyncAction action;

    /**
     * 源库
     */
    private DatabaseType sourceDb;

    /**
     * 目标库
     */
    private DatabaseType targetDb;

    /**
     * 是否同步成功
     */
    private boolean success;

    /**
     * 错误信息（失败时有效）
     */
    private String errorMsg;
}
