package com.notice.system.vo.synclog;

import lombok.Data;

@Data
public class SyncLogDailyReportVo {
    private String statDate;
    private String sourceDb;
    private String targetDb;

    private Long totalCount;
    private Long successCount;
    private Long failedCount;
    private Long conflictCount;
    private Long errorCount;
}
