package com.notice.system.mapper.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.SyncLog;
import com.notice.system.mapper.dto.SyncLogDailyStat;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SyncLog 公共 Mapper 接口：定义通用报表方法
 * 三个具体 Mapper（MySQL / PG / SQLServer）都 extends 它
 */
public interface SyncLogBaseMapper extends BaseMapper<SyncLog> {

    /**
     * 按天统计同步日志（返回内部统计结果）
     */
    List<SyncLogDailyStat> listDailyStats(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("status") String status,
            @Param("sourceDb") String sourceDb,
            @Param("targetDb") String targetDb
    );

    // 调用清理存储过程
    void callCleanProc(@Param("retainDays") Integer retainDays,
                       @Param("maxCount") Long maxCount);
}

