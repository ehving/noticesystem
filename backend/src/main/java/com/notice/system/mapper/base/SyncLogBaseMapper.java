package com.notice.system.mapper.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.SyncLog;
import com.notice.system.mapper.dto.SyncLogDailyStat;
import com.notice.system.vo.report.AggVo;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**SyncLog 公共 Mapper 接口：定义通用报表方法
 三个具体 Mapper（MySQL / PG / SQLServer）都 extends */
public interface SyncLogBaseMapper extends BaseMapper<SyncLog> {

    /**按天统计同步日志（返回内部统计结果）*/
    List<SyncLogDailyStat> listDailyStats(
            @Param("beginTime") LocalDateTime beginTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("status") String status,
            @Param("sourceDb") String sourceDb,
            @Param("targetDb") String targetDb
    );

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

    // 调用清理存储过程
    void callCleanProc(@Param("retainDays") Integer retainDays,
                       @Param("maxCount") Long maxCount);

    /**
     * 增量扫描：拉取最近更新的 SUCCESS 日志（用于离线发现“新冲突”候选集）
     *
     * @param fromTime  起始时间（包含）
     * @param toTime    结束时间（可为 null；为 null 表示不限制上界）
     * @param limit     最大返回条数（<=0 时建议由 Service 兜底）
     */
    List<SyncLog> listRecentSuccessLogs(@Param("fromTime") LocalDateTime fromTime,
                                        @Param("toTime") LocalDateTime toTime,
                                        @Param("limit") Integer limit);



}

