package com.notice.system.vo.synclog;

import lombok.Data;

/**
 * 同步日志每日统计报表 VO（返回给前端）
 */
@Data
public class SyncLogDailyReportVo {

    private String statDate;      // "2025-12-12" 这种形式即可
    private String sourceDb;
    private String targetDb;

    private Long totalCount;
    private Long successCount;
    private Long failedCount;
    private Double failedRate;    // 失败率 = failed / total
}
