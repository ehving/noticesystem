package com.notice.system.mapper.pg;

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
@DS("pg")
public interface SyncLogPgMapper extends SyncLogBaseMapper {

    @Override
    @Select("""
<script>
SELECT
  CAST(create_time AS date) AS statDate,
  source_db AS sourceDb,
  target_db AS targetDb,
  COUNT(*) AS totalCount,
  SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END)  AS successCount,
  SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END)   AS failedCount,
  SUM(CASE WHEN status = 'CONFLICT' THEN 1 ELSE 0 END) AS conflictCount,
  SUM(CASE WHEN status = 'ERROR' THEN 1 ELSE 0 END)    AS errorCount
FROM sync_log
WHERE create_time &gt;= #{beginTime, jdbcType=TIMESTAMP}
  AND create_time &lt;= #{endTime, jdbcType=TIMESTAMP}
  AND (NULLIF(#{entityType}, '') IS NULL OR entity_type = #{entityType})
  AND (NULLIF(#{action}, '') IS NULL OR action = #{action})
  AND (NULLIF(#{status}, '') IS NULL OR status = #{status})
  AND (NULLIF(#{sourceDb}, '') IS NULL OR source_db = #{sourceDb})
  AND (NULLIF(#{targetDb}, '') IS NULL OR target_db = #{targetDb})
GROUP BY CAST(create_time AS date), source_db, target_db
ORDER BY statDate DESC, sourceDb, targetDb
</script>
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
      ${groupCol} AS key,
      COUNT(*) AS count
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
        AND l.status = #{status, jdbcType=VARCHAR}
      </if>
      <if test="sourceDb != null and sourceDb != ''">
        AND l.source_db = #{sourceDb, jdbcType=VARCHAR}
      </if>
      <if test="targetDb != null and targetDb != ''">
        AND l.target_db = #{targetDb, jdbcType=VARCHAR}
      </if>
    GROUP BY ${groupCol}
    ORDER BY count DESC
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
    @Select("CALL sp_clean_sync_log(#{retainDays, jdbcType=INTEGER}, #{maxCount, jdbcType=BIGINT})")
    void callCleanProc(@Param("retainDays") Integer retainDays,
                       @Param("maxCount") Long maxCount);

    @Override
    @Select("""
<script>
SELECT
  id,
  entity_type AS "entityType",
  entity_id   AS "entityId",
  action,
  source_db   AS "sourceDb",
  target_db   AS "targetDb",
  status,
  error_msg   AS "errorMsg",
  retry_count AS "retryCount",
  create_time AS "createTime",
  update_time AS "updateTime"
FROM sync_log
WHERE status = 'SUCCESS'
  AND update_time &gt;= #{fromTime, jdbcType=TIMESTAMP}
  <if test="toTime != null">
    AND update_time &lt;= #{toTime, jdbcType=TIMESTAMP}
  </if>
ORDER BY update_time DESC
LIMIT #{limit, jdbcType=INTEGER}
</script>
""")
    List<SyncLog> listRecentSuccessLogs(@Param("fromTime") LocalDateTime fromTime,
                                        @Param("toTime") LocalDateTime toTime,
                                        @Param("limit") Integer limit);
}
