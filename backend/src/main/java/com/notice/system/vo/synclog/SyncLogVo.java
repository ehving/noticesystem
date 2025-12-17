package com.notice.system.vo.synclog;

import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncAction;
import com.notice.system.sync.SyncEntityType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同步日志分页查询 VO
 */
@Data
public class SyncLogVo {

    /** 页码，从 1 开始 */
    private long pageNo = 1L;

    /** 每页大小 */
    private long pageSize = 10L;

    /** 过滤条件：实体类型，可选，例如 USER / NOTICE / DEPT / SYNC_LOG 等 */
    private SyncEntityType entityType;

    /** 过滤条件：实体主键 ID，可选 */
    private String entityId;

    /** 过滤条件：操作类型 CREATE / UPDATE / DELETE，可选 */
    private SyncAction action;

    /** 过滤条件：源库，可选 */
    private DatabaseType sourceDb;

    /** 过滤条件：目标库，可选 */
    private DatabaseType targetDb;

    /** 过滤条件：状态 SUCCESS / FAILED，可选 */
    private String status;

    /** 起始时间（按 createTime 过滤），可选 */
    private LocalDateTime beginTime;

    /** 截止时间（按 createTime 过滤），可选 */
    private LocalDateTime endTime;
}


