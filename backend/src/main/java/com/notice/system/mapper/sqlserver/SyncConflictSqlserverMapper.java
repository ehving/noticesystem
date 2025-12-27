package com.notice.system.mapper.sqlserver;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.notice.system.entity.SyncConflict;
import com.notice.system.mapper.base.SyncConflictBaseMapper;
import com.notice.system.mapper.dto.SyncConflictWithItemRow;
import com.notice.system.vo.report.AggVo;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
@DS("sqlserver")
public interface SyncConflictSqlserverMapper extends SyncConflictBaseMapper {

    @Override
    @Select("""
        SELECT TOP (#{limit, jdbcType=INTEGER}) *
        FROM sync_conflict
        WHERE status = 'OPEN'
          AND (
                last_notified_at IS NULL
                OR last_notified_at <= DATEADD(minute, -#{cooldownMinutes, jdbcType=INTEGER}, #{now, jdbcType=TIMESTAMP})
          )
        ORDER BY ISNULL(last_notified_at, CAST('1970-01-01' AS datetime2)) ASC, last_seen_at DESC
        """)
    List<SyncConflict> listNeedNotify(@Param("now") LocalDateTime now,
                                      @Param("cooldownMinutes") Integer cooldownMinutes,
                                      @Param("limit") Integer limit);

    @Override
    @Select("""
    <script>
    SELECT
      ${groupCol} AS [key],
      COUNT(1) AS [count]
    FROM sync_conflict c
    WHERE c.last_seen_at &gt;= #{beginTime, jdbcType=TIMESTAMP}
      AND c.last_seen_at &lt;= #{endTime,   jdbcType=TIMESTAMP}
      <if test="entityType != null and entityType != ''">
        AND c.entity_type = #{entityType, jdbcType=VARCHAR}
      </if>
      <if test="status != null and status != ''">
        AND c.[status] = #{status, jdbcType=VARCHAR}
      </if>
      <if test="conflictType != null and conflictType != ''">
        AND c.conflict_type = #{conflictType, jdbcType=VARCHAR}
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
            @Param("status") String status,
            @Param("conflictType") String conflictType
    );

    @Override
    @Select("""
        SELECT COUNT(1)
        FROM sync_conflict c
        WHERE 1=1
          AND (#{limitToOpenOnly, jdbcType=BIT} IS NULL
               OR #{limitToOpenOnly, jdbcType=BIT} = 0
               OR c.status = 'OPEN')

          AND (NULLIF(#{status, jdbcType=VARCHAR}, '') IS NULL
               OR c.status = #{status, jdbcType=VARCHAR})

          AND (NULLIF(#{entityType, jdbcType=VARCHAR}, '') IS NULL
               OR c.entity_type = #{entityType, jdbcType=VARCHAR})

          AND (NULLIF(#{conflictType, jdbcType=VARCHAR}, '') IS NULL
               OR c.conflict_type = #{conflictType, jdbcType=VARCHAR})

          AND (#{beginTime, jdbcType=TIMESTAMP} IS NULL
               OR c.last_seen_at >= #{beginTime, jdbcType=TIMESTAMP})

          AND (#{endTime, jdbcType=TIMESTAMP} IS NULL
               OR c.last_seen_at <= #{endTime, jdbcType=TIMESTAMP})

          AND (NULLIF(#{sourceDb, jdbcType=VARCHAR}, '') IS NULL
               OR EXISTS (
                    SELECT 1
                    FROM sync_conflict_item i
                    WHERE i.conflict_id = c.id
                      AND i.db_type = #{sourceDb, jdbcType=VARCHAR}
               ))
        """)
    Long countConflictsForPage(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("entityType") String entityType,
            @Param("status") String status,
            @Param("conflictType") String conflictType,
            @Param("sourceDb") String sourceDb,
            @Param("limitToOpenOnly") Boolean limitToOpenOnly
    );

    @Override
    @Select("""
        WITH page_ids AS (
          SELECT c0.id
          FROM sync_conflict c0
          WHERE 1=1
            AND (#{limitToOpenOnly, jdbcType=BIT} IS NULL
                 OR #{limitToOpenOnly, jdbcType=BIT} = 0
                 OR c0.status = 'OPEN')

            AND (NULLIF(#{status, jdbcType=VARCHAR}, '') IS NULL
                 OR c0.status = #{status, jdbcType=VARCHAR})

            AND (NULLIF(#{entityType, jdbcType=VARCHAR}, '') IS NULL
                 OR c0.entity_type = #{entityType, jdbcType=VARCHAR})

            AND (NULLIF(#{conflictType, jdbcType=VARCHAR}, '') IS NULL
                 OR c0.conflict_type = #{conflictType, jdbcType=VARCHAR})

            AND (#{beginTime, jdbcType=TIMESTAMP} IS NULL
                 OR c0.last_seen_at >= #{beginTime, jdbcType=TIMESTAMP})

            AND (#{endTime, jdbcType=TIMESTAMP} IS NULL
                 OR c0.last_seen_at <= #{endTime, jdbcType=TIMESTAMP})

            AND (NULLIF(#{sourceDb, jdbcType=VARCHAR}, '') IS NULL
                 OR EXISTS (
                      SELECT 1
                      FROM sync_conflict_item i0
                      WHERE i0.conflict_id = c0.id
                        AND i0.db_type = #{sourceDb, jdbcType=VARCHAR}
                 ))
          ORDER BY c0.last_seen_at DESC, c0.id DESC
          OFFSET #{offset, jdbcType=BIGINT} ROWS FETCH NEXT #{limit, jdbcType=BIGINT} ROWS ONLY
        )
        SELECT
          c.id                   AS conflictId,
          c.entity_type          AS entityType,
          c.entity_id            AS entityId,
          c.status               AS status,
          c.conflict_type        AS conflictType,
          c.first_seen_at        AS firstSeenAt,
          c.last_seen_at         AS lastSeenAt,
          c.last_checked_at      AS lastCheckedAt,
          c.last_notified_at     AS lastNotifiedAt,
          c.notify_count         AS notifyCount,
          c.resolution_source_db AS resolutionSourceDb,
          c.resolution_note      AS resolutionNote,
          c.resolved_at          AS resolvedAt,

          i.id                   AS itemId,
          i.db_type              AS dbType,
          i.exists_flag          AS existsFlag,
          i.row_hash             AS rowHash,
          i.row_version          AS rowVersion,
          i.row_update_time      AS rowUpdateTime,
          i.last_checked_at      AS itemLastCheckedAt
        FROM page_ids p
        JOIN sync_conflict c ON c.id = p.id
        LEFT JOIN sync_conflict_item i ON i.conflict_id = c.id
        ORDER BY c.last_seen_at DESC, c.id DESC, i.db_type ASC
        """)
    List<SyncConflictWithItemRow> selectConflictPageWithItems(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("entityType") String entityType,
            @Param("status") String status,
            @Param("conflictType") String conflictType,
            @Param("sourceDb") String sourceDb,
            @Param("limitToOpenOnly") Boolean limitToOpenOnly,
            @Param("offset") long offset,
            @Param("limit") long limit
    );
}


