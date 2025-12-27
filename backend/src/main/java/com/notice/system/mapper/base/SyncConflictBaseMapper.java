package com.notice.system.mapper.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.SyncConflict;
import com.notice.system.mapper.dto.SyncConflictWithItemRow;
import com.notice.system.vo.report.AggVo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SyncConflictBaseMapper extends BaseMapper<SyncConflict> {

    List<SyncConflict> listNeedNotify(
            @Param("now") LocalDateTime now,
            @Param("cooldownMinutes") Integer cooldownMinutes,
            @Param("limit") Integer limit
    );

    List<AggVo> agg(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("groupCol") String groupCol,   // 注意：来自白名单映射
            @Param("entityType") String entityType,
            @Param("status") String status,
            @Param("conflictType") String conflictType
    );

    Long countConflictsForPage(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("entityType") String entityType,
            @Param("status") String status,
            @Param("conflictType") String conflictType, // ✅ 新增
            @Param("sourceDb") String sourceDb,
            @Param("limitToOpenOnly") Boolean limitToOpenOnly
    );

    List<SyncConflictWithItemRow> selectConflictPageWithItems(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("entityType") String entityType,
            @Param("status") String status,
            @Param("conflictType") String conflictType, // ✅ 新增
            @Param("sourceDb") String sourceDb,
            @Param("limitToOpenOnly") Boolean limitToOpenOnly,
            @Param("offset") long offset,
            @Param("limit") long limit
    );


}


