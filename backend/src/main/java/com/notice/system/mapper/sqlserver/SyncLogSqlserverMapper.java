package com.notice.system.mapper.sqlserver;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.notice.system.entity.SyncLog;
import com.notice.system.mapper.base.SyncLogBaseMapper;
import com.notice.system.mapper.dto.SyncLogDailyStat;
import com.notice.system.vo.report.AggVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@DS("sqlserver")
public interface SyncLogSqlserverMapper extends SyncLogBaseMapper {

    @Override
    @Select("""
SELECT
  CONVERT(date, create_time) AS statDate,
  source_db AS sourceDb,
  target_db AS targetDb,
  COUNT(*) AS totalCount,
  SUM(IIF([status] = 'SUCCESS', 1, 0))  AS successCount,
  SUM(IIF([status] = 'FAILED', 1, 0))   AS failedCount,
  SUM(IIF([status] = 'CONFLICT', 1, 0)) AS conflictCount,
  SUM(IIF([status] = 'ERROR', 1, 0))    AS errorCount
FROM sync_log
WHERE create_time >= #{beginTime, jdbcType=TIMESTAMP}
  AND create_time <= #{endTime, jdbcType=TIMESTAMP}
  AND (NULLIF(#{entityType}, '') IS NULL OR entity_type = #{entityType})
  AND (NULLIF(#{action}, '') IS NULL OR action = #{action})
  AND (NULLIF(#{status}, '') IS NULL OR [status] = #{status})
  AND (NULLIF(#{sourceDb}, '') IS NULL OR source_db = #{sourceDb})
  AND (NULLIF(#{targetDb}, '') IS NULL OR target_db = #{targetDb})
GROUP BY CONVERT(date, create_time), source_db, target_db
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
    @Select("""
    <script>
    SELECT
      ${groupCol} AS [key],
      COUNT(1) AS [count]
    FROM sync_log l
    WHERE l.create_time &gt;= #{beginTime, jdbcType=TIMESTAMP}
      AND l.create_time &lt;= #{endTime,   jdbcType=TIMESTAMP}
      <if test="entityType != null and entityType != ''">
        AND l.entity_type = #{entityType, jdbcType=VARCHAR}
      </if>
      <if test="action != null and action != ''">
        AND l.action = #{action, jdbcType=VARCHAR}
      </if>
      <if test="status != null and status != ''">
        AND l.[status] = #{status, jdbcType=VARCHAR}
      </if>
      <if test="sourceDb != null and sourceDb != ''">
        AND l.source_db = #{sourceDb, jdbcType=VARCHAR}
      </if>
      <if test="targetDb != null and targetDb != ''">
        AND l.target_db = #{targetDb, jdbcType=VARCHAR}
      </if>
    GROUP BY ${groupCol}
    ORDER BY [count] DESC
    </script>
    """)
    List<AggVo> agg(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("groupCol") String groupCol,   // 注意：来自白名单映射
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("status") String status,
            @Param("sourceDb") String sourceDb,
            @Param("targetDb") String targetDb
    );

    @Override
    @Select("EXEC dbo.sp_clean_sync_log #{retainDays}, #{maxCount}")
    void callCleanProc(@Param("retainDays") Integer retainDays,
                       @Param("maxCount") Long maxCount);

    @Override
    @Select("""
  SELECT TOP (#{limit})
    id,
    entity_type  AS entityType,
    entity_id    AS entityId,
    action,
    source_db    AS sourceDb,
    target_db    AS targetDb,
    [status]     AS status,
    error_msg    AS errorMsg,
    retry_count  AS retryCount,
    create_time  AS createTime,
    update_time  AS updateTime
  FROM sync_log
  WHERE [status] = 'SUCCESS'
    AND update_time >= #{fromTime, jdbcType=TIMESTAMP}
    AND (#{toTime} IS NULL OR update_time <= #{toTime, jdbcType=TIMESTAMP})
  ORDER BY update_time DESC
""")
    List<SyncLog> listRecentSuccessLogs(@Param("fromTime") LocalDateTime fromTime,
                                        @Param("toTime") LocalDateTime toTime,
                                        @Param("limit") Integer limit);

}
