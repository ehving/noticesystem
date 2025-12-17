package com.notice.system.mapper.pg;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.notice.system.mapper.base.SyncLogBaseMapper;
import com.notice.system.mapper.dto.SyncLogDailyStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@DS("pg")
public interface SyncLogPgMapper extends SyncLogBaseMapper {

    @Override
    @Select("""
        SELECT
          DATE(create_time) AS statDate,
          source_db AS sourceDb,
          target_db AS targetDb,
          COUNT(*) AS totalCount,
          SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS successCount,
          SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) AS failedCount
        FROM sync_log
        WHERE create_time >= #{beginTime}
          AND create_time <= #{endTime}
          AND (#{entityType} IS NULL OR entity_type = #{entityType})
          AND (#{action} IS NULL OR action = #{action})
          AND (#{status} IS NULL OR status = #{status})
          AND (#{sourceDb} IS NULL OR source_db = #{sourceDb})
          AND (#{targetDb} IS NULL OR target_db = #{targetDb})
        GROUP BY DATE(create_time), source_db, target_db
        ORDER BY statDate DESC, sourceDb, targetDb
        """)
    List<SyncLogDailyStat> listDailyStats(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("status") String status,
            @Param("sourceDb") String sourceDb,
            @Param("targetDb") String targetDb
    );

    @Override
    @Select("CALL sp_clean_sync_log(#{retainDays}, #{maxCount})")
    void callCleanProc(@Param("retainDays") Integer retainDays,
                       @Param("maxCount") Long maxCount);
}