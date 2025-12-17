package com.notice.system.mapper.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 用于 mapper 查询的同步日志日报统计结果
 */
@Data
public class SyncLogDailyStat {

    private LocalDate statDate;
    private String sourceDb;
    private String targetDb;

    private Long totalCount;
    private Long successCount;
    private Long failedCount;
}
