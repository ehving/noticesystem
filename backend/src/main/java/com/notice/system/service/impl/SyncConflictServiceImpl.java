package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notice.system.common.GlobalProperties;
import com.notice.system.common.SnapshotUtil;
import com.notice.system.converter.SyncConflictConverter;
import com.notice.system.entity.*;
import com.notice.system.entityEnum.*;
import com.notice.system.entityEnum.aggBy.SyncConflictAggBy;
import com.notice.system.mapper.base.SyncConflictBaseMapper;
import com.notice.system.mapper.base.SyncConflictItemBaseMapper;
import com.notice.system.mapper.dto.SyncConflictWithItemRow;
import com.notice.system.service.*;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.support.event.SyncBatchPostCheckEvent;
import com.notice.system.sync.SyncMetadataRegistry;
import com.notice.system.vo.conflict.SyncConflictDetailVo;
import com.notice.system.vo.conflict.SyncConflictItemVo;
import com.notice.system.vo.conflict.SyncConflictQueryVo;
import com.notice.system.vo.conflict.SyncConflictWithItemsVo;
import com.notice.system.vo.report.AggVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 冲突工单服务（SyncConflict + SyncConflictItem）
 */
@Slf4j
@Service
public class SyncConflictServiceImpl
        extends MultiDbSyncServiceImpl<SyncConflict>
        implements SyncConflictService {

    private final GlobalProperties globalProperties;
    private final MailService mailService;
    private final SyncConflictItemService conflictItemService;
    private final SyncLogService syncLogService;
    private final ObjectMapper objectMapper;

    public SyncConflictServiceImpl(SyncService syncService,
                                   SyncMetadataRegistry metadataRegistry,
                                   MailService mailService,
                                   SyncConflictItemService conflictItemService,
                                   GlobalProperties globalProperties,
                                   SyncLogService syncLogService,
                                   ObjectMapper objectMapper) {
        super(syncService, metadataRegistry, SyncEntityType.SYNC_CONFLICT, DatabaseType.MYSQL);
        this.mailService = mailService;
        this.conflictItemService = conflictItemService;
        this.globalProperties = globalProperties;
        this.syncLogService = syncLogService;
        this.objectMapper = objectMapper;
    }

    /* ======================== Mapper helpers（查询/分页/报表） ======================== */

    private SyncConflictBaseMapper conflictMapper(DatabaseType db) {
        return resolveMapperAs(db, SyncConflictBaseMapper.class);
    }

    private SyncConflictItemBaseMapper conflictItemMapper(DatabaseType db) {
        return resolveMapperOf(SyncEntityType.SYNC_CONFLICT_ITEM, db, SyncConflictItemBaseMapper.class);
    }

    /* ======================== 同步后检查：post-check 事件 ======================== */

    @EventListener
    public void onSyncBatchPostCheck(SyncBatchPostCheckEvent ev) {
        if (ev == null || ev.getEntityType() == null) return;

        Map<DatabaseType, Boolean> ok = ev.getTargetApplyOk();
        boolean anyOk = ok != null && ok.values().stream().anyMatch(Boolean::booleanValue);
        if (!anyOk) return;

        String conflictId = checkAndUpsertConflictIfNeeded(
                ev.getEntityType(),
                ev.getEntityId(),
                ev.getAction(),
                ev.getSourceDb()
        );
        ev.setConflictId(conflictId);
    }

    @Override
    public String checkAndUpsertConflictIfNeeded(SyncEntityType entityType,
                                                 String entityId,
                                                 SyncAction action,
                                                 DatabaseType sourceDb) {
        if (shouldSkipCheck(entityType, entityId, action)) return null;

        SnapshotUtil.Snapshot snap = readSnapshot(entityType, entityId);
        ConflictType conflictType = SnapshotUtil.judgeConflictType(action, snap);
        if (conflictType == null) return null;

        return upsertConflictBySnapshot(
                entityType,
                entityId,
                conflictType,
                snap,
                LocalDateTime.now(),
                true
        );
    }

    private boolean shouldSkipCheck(SyncEntityType type, String id, SyncAction action) {
        if (type == null) return true;
        if (id == null || id.isBlank()) return true;
        if (action == SyncAction.DELETE) return true;

        return type == SyncEntityType.SYNC_LOG
                || type == SyncEntityType.SYNC_CONFLICT
                || type == SyncEntityType.SYNC_CONFLICT_ITEM;
    }

    /* ======================== 管理员操作：修复 / 忽略 / 重开 ======================== */

    @Override
    public boolean resolveConflict(String conflictId, DatabaseType sourceDb, String note) {
        if (conflictId == null || conflictId.isBlank() || sourceDb == null) return false;

        DatabaseType workDb = this.defaultDb();
        SyncConflict conflict = getById(workDb, conflictId);
        if (conflict == null) return false;

        LocalDateTime now = LocalDateTime.now();

        if (conflict.getStatus() == ConflictStatus.IGNORED) {
            conflict.setStatus(ConflictStatus.OPEN);
            conflict.setResolvedAt(null);
            conflict.setResolutionSourceDb(null);

            String old = conflict.getResolutionNote();
            String reopen = "[AUTO-REOPEN] 冲突原状态为 IGNORED，修复前已自动恢复为 OPEN。";
            conflict.setResolutionNote((old == null || old.isBlank()) ? reopen : (old + "\n" + reopen));

            conflict.setLastSeenAt(now);
            conflict.setLastCheckedAt(now);

            updateByIdInDb(workDb, conflict);

            conflict = getById(workDb, conflictId);
            if (conflict == null) return false;
        }

        SyncEntityType entityType = conflict.getEntityType();
        String entityId = conflict.getEntityId();
        if (entityType == null || entityId == null || entityId.isBlank()) return false;

        boolean allOk = true;
        for (DatabaseType target : DatabaseType.syncDbs()) {
            if (target == sourceDb) continue;
            boolean ok = syncService.syncToTargetWithoutLog(entityType, entityId, SyncAction.UPDATE, sourceDb, target);
            if (!ok) allOk = false;
        }

        boolean recheckOk;
        try {
            recheckOk = recheckConflict(conflictId, now);
        } catch (Exception e) {
            log.warn("[CONFLICT] resolveConflict recheck failed, conflictId={}, err={}", conflictId, e.getMessage(), e);
            recheckOk = false;
        }

        if (allOk && recheckOk) {
            SyncConflict latest = getById(workDb, conflictId);
            if (latest != null && latest.getStatus() == ConflictStatus.RESOLVED) {
                latest.setResolutionSourceDb(sourceDb);
                latest.setResolutionNote(note);
                latest.setResolvedAt(now);
                latest.setLastCheckedAt(now);
                latest.setLastSeenAt(now);
                updateByIdInDb(workDb, latest);
                return true;
            }
        }

        if (!allOk) {
            log.warn("[CONFLICT] resolveConflict not fully ok, conflictId={}, sourceDb={}", conflictId, sourceDb);
        } else {
            log.warn("[CONFLICT] resolveConflict allOk but still conflict after recheck, conflictId={}, sourceDb={}",
                    conflictId, sourceDb);
        }
        return false;
    }

    @Override
    public boolean ignoreConflict(String conflictId, String note) {
        if (conflictId == null || conflictId.isBlank()) return false;

        DatabaseType workDb = this.defaultDb();
        SyncConflict c = getById(workDb, conflictId);
        if (c == null) return false;

        c.setStatus(ConflictStatus.IGNORED);

        String old = c.getResolutionNote();
        String msg = "[IGNORE] " + (note == null ? "" : note);
        c.setResolutionNote(old == null || old.isBlank() ? msg : (old + "\n" + msg));

        LocalDateTime now = LocalDateTime.now();
        c.setLastCheckedAt(now);
        c.setLastSeenAt(now);

        return updateByIdInDb(workDb, c);
    }

    @Override
    public boolean reopenConflict(String conflictId, String note, LocalDateTime now) {
        if (conflictId == null || conflictId.isBlank()) return false;
        LocalDateTime t = nvlNow(now);

        DatabaseType workDb = this.defaultDb();
        SyncConflict c = getById(workDb, conflictId);
        if (c == null) return false;

        if (c.getStatus() == ConflictStatus.OPEN) {
            String old = c.getResolutionNote();
            String msg = "[REOPEN-TOUCH] " + (note == null ? "" : note);
            c.setResolutionNote(old == null || old.isBlank() ? msg : (old + "\n" + msg));
            c.setLastSeenAt(t);
            c.setLastCheckedAt(t);
            return updateByIdInDb(workDb, c);
        }

        c.setStatus(ConflictStatus.OPEN);
        c.setResolvedAt(null);
        c.setResolutionSourceDb(null);
        c.setLastNotifiedAt(null);

        String old = c.getResolutionNote();
        String msg = "[REOPEN] " + (note == null ? "" : note);
        c.setResolutionNote(old == null || old.isBlank() ? msg : (old + "\n" + msg));

        c.setLastSeenAt(t);
        c.setLastCheckedAt(t);

        return updateByIdInDb(workDb, c);
    }

    /* ======================== 扫描 / 通知 / 重检 ======================== */

    @Override
    public int detectNewConflictsFromSuccessLogs(LocalDateTime fromTime,
                                                 LocalDateTime toTime,
                                                 int perDbLogLimit,
                                                 int entityLimit) {
        LocalDateTime to = nvlNow(toTime);
        LocalDateTime from = (fromTime == null ? to.minusDays(30) : fromTime);

        int perDbLim = (perDbLogLimit <= 0 ? 200 : perDbLogLimit);
        int entLim = (entityLimit <= 0 ? 200 : entityLimit);

        List<SyncLog> logs = syncLogService.listRecentSuccessLogsDedup(from, to, perDbLim, entLim);
        if (logs == null || logs.isEmpty()) return 0;

        int found = 0;
        for (SyncLog lg : logs) {
            if (lg == null) continue;

            try {
                String entityId = lg.getEntityId();
                if (entityId == null || entityId.isBlank()) continue;

                SyncEntityType entityType;
                SyncAction action;
                try {
                    entityType = SyncEntityType.valueOf(String.valueOf(lg.getEntityType()));
                    action = SyncAction.valueOf(String.valueOf(lg.getAction()));
                } catch (Exception ignore) {
                    continue;
                }

                if (shouldSkipCheck(entityType, entityId, action)) continue;

                SnapshotUtil.Snapshot snap = readSnapshot(entityType, entityId);
                ConflictType conflictType = SnapshotUtil.judgeConflictType(action, snap);
                if (conflictType == null) continue;

                String conflictId = upsertConflictBySnapshot(
                        entityType, entityId, conflictType, snap, LocalDateTime.now(), false
                );
                if (conflictId != null) {
                    found++;
                    log.info("[CONFLICT-DETECT] found by success-log: logId={}, entityType={}, entityId={}, conflictId={}",
                            lg.getId(), entityType, entityId, conflictId);
                }
            } catch (Exception e) {
                log.warn("[CONFLICT-DETECT] scan failed, logId={}, err={}", lg.getId(), e.getMessage(), e);
            }
        }

        return found;
    }

    private void tryNotifyConflict(LocalDateTime now) {
        notifyPendingConflicts(nvlNow(now), 20);
    }

    @Override
    public int notifyPendingConflicts(LocalDateTime now, int limit) {
        int lim = (limit <= 0 ? 20 : limit);
        LocalDateTime t = nvlNow(now);

        int cooldownMinutes = 30;
        GlobalProperties.Sync.Conflict cfg = globalProperties.getSync().getConflict();
        if (cfg != null && cfg.getNotifyCooldownMinutes() > 0) {
            cooldownMinutes = cfg.getNotifyCooldownMinutes();
        }

        DatabaseType workDb = this.defaultDb();
        SyncConflictBaseMapper cm = conflictMapper(workDb);
        SyncConflictItemBaseMapper im = conflictItemMapper(workDb);

        List<SyncConflict> list = Optional.ofNullable(cm.listNeedNotify(t, cooldownMinutes, lim)).orElseGet(List::of);
        if (list.isEmpty()) return 0;

        int sent = 0;
        for (SyncConflict c : list) {
            if (c == null || c.getId() == null) continue;

            try {
                SyncConflict latest = getById(workDb, c.getId());
                if (latest == null || latest.getStatus() != ConflictStatus.OPEN) continue;

                List<SyncConflictItem> items = im.listByConflictId(c.getId());
                mailService.sendConflictAlert(latest, items);

                latest.setLastNotifiedAt(t);
                latest.setNotifyCount((latest.getNotifyCount() == null ? 0 : latest.getNotifyCount()) + 1);
                updateByIdInDb(workDb, latest);

                sent++;
            } catch (Exception e) {
                log.warn("[CONFLICT] notify failed, conflictId={}, err={}", c.getId(), e.getMessage(), e);
            }
        }
        return sent;
    }

    @Override
    public boolean recheckConflict(String conflictId, LocalDateTime now) {
        if (conflictId == null || conflictId.isBlank()) return false;

        LocalDateTime t = nvlNow(now);
        DatabaseType workDb = this.defaultDb();

        SyncConflict c = getById(workDb, conflictId);
        if (c == null) return false;

        SnapshotUtil.Snapshot snap = readSnapshot(c.getEntityType(), c.getEntityId());

        conflictItemService.upsertSnapshotItems(conflictId, snap.exists, snap.hash, t);

        c.setLastCheckedAt(t);
        c.setLastSeenAt(t);

        boolean stillConflict = snap.anyMissing || snap.mismatch;

        if (!stillConflict) {
            c.setConflictType(null);

            if (c.getStatus() != ConflictStatus.IGNORED) {
                if (c.getStatus() == ConflictStatus.OPEN) {
                    c.setStatus(ConflictStatus.RESOLVED);

                    String old = c.getResolutionNote();
                    String msg = "[AUTO-RESOLVE] recheck 后三库数据一致，自动标记 RESOLVED。";
                    c.setResolutionNote(old == null || old.isBlank() ? msg : (old + "\n" + msg));
                    c.setResolvedAt(t);
                }
            }
        } else {
            // 仍有冲突：按 UPDATE 语义判断即可
            ConflictType newType = SnapshotUtil.judgeConflictType(SyncAction.UPDATE, snap);
            c.setConflictType(newType);

            if (c.getStatus() == ConflictStatus.RESOLVED) {
                c.setStatus(ConflictStatus.OPEN);
                c.setResolvedAt(null);

                String old = c.getResolutionNote();
                String msg = "[AUTO-REOPEN] recheck 后仍检测到冲突，自动恢复为 OPEN。";
                c.setResolutionNote(old == null || old.isBlank() ? msg : (old + "\n" + msg));
            }
        }

        return updateByIdInDb(workDb, c);
    }

    @Override
    public int recheckOpenConflicts(LocalDateTime now, int limit) {
        int lim = (limit <= 0 ? 50 : limit);
        LocalDateTime t = nvlNow(now);

        DatabaseType workDb = this.defaultDb();

        Page<SyncConflict> page = new Page<>(1, lim);
        LambdaQueryWrapper<SyncConflict> qw = new LambdaQueryWrapper<SyncConflict>()
                .eq(SyncConflict::getStatus, ConflictStatus.OPEN)
                .orderByAsc(SyncConflict::getLastCheckedAt)
                .orderByAsc(SyncConflict::getLastSeenAt);

        List<SyncConflict> list = resolveMapper(workDb).selectPage(page, qw).getRecords();
        if (list == null || list.isEmpty()) return 0;

        int processed = 0;
        for (SyncConflict c : list) {
            if (c == null || c.getId() == null) continue;
            try {
                if (recheckConflict(c.getId(), t)) processed++;
            } catch (Exception e) {
                log.warn("[CONFLICT] recheckOpenConflicts failed, id={}, err={}", c.getId(), e.getMessage(), e);
            }
        }
        return processed;
    }

    /* ======================== 前端：分页/详情 ======================== */

    @Override
    public Page<SyncConflictWithItemsVo> pageConflicts(DatabaseType db, SyncConflictQueryVo query) {
        DatabaseType useDb = useDb(db);
        SyncConflictQueryVo vo = (query == null ? new SyncConflictQueryVo() : query);

        long pageNo = (vo.getPageNo() == null || vo.getPageNo() <= 0) ? 1 : vo.getPageNo();
        long pageSize = (vo.getPageSize() == null || vo.getPageSize() <= 0) ? 10 : vo.getPageSize();

        LocalDateTime begin = vo.getBeginTime();
        LocalDateTime end = vo.getEndTime();

        String entityType = (vo.getEntityType() == null ? null : vo.getEntityType().name());
        String status = normalizeEnumCode(vo.getStatus() == null ? null : vo.getStatus().name());
        String conflictType = normalizeEnumCode(vo.getConflictType() == null ? null : vo.getConflictType().name());
        String sourceDb = (vo.getSourceDb() == null ? null : vo.getSourceDb().name());
        Boolean openOnly = vo.getLimitToOpenOnly();

        SyncConflictBaseMapper cm = conflictMapper(useDb);

        Long total = cm.countConflictsForPage(begin, end, entityType, status, conflictType, sourceDb, openOnly);
        long t = (total == null ? 0L : total);

        long offset = (pageNo - 1) * pageSize;
        List<SyncConflictWithItemRow> rows = cm.selectConflictPageWithItems(
                begin, end, entityType, status, conflictType, sourceDb, openOnly, offset, pageSize
        );

        List<SyncConflictWithItemsVo> records = SyncConflictConverter.toListVosFromRows(rows);

        Page<SyncConflictWithItemsVo> page = new Page<>(pageNo, pageSize);
        page.setTotal(t);
        page.setRecords(records);
        return page;
    }

    private static String normalizeEnumCode(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t.toUpperCase();
    }

    @Override
    public SyncConflictDetailVo getConflictDetail(DatabaseType db, String conflictId) {
        DatabaseType useDb = useDb(db);
        if (conflictId == null || conflictId.isBlank()) return null;

        SyncConflict conflict = conflictMapper(useDb).selectById(conflictId);
        if (conflict == null) return null;

        List<SyncConflictItem> items = conflictItemMapper(useDb).listByConflictId(conflictId);
        return SyncConflictConverter.toDetailVo(conflict, items);
    }

    /* ======================== 报表：聚合 ======================== */

    @Override
    public List<AggVo> aggConflicts(DatabaseType db,
                                    LocalDateTime begin,
                                    LocalDateTime end,
                                    SyncConflictAggBy by,
                                    SyncConflictQueryVo filter) {
        DatabaseType useDb = useDb(db);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime b = (begin == null ? now.minusDays(6).toLocalDate().atStartOfDay() : begin);
        LocalDateTime e = (end == null ? now : end);
        if (b.isAfter(e)) { LocalDateTime tmp = b; b = e; e = tmp; }

        if (by == null) throw new IllegalArgumentException("agg by 不能为空");

        String groupCol = mapConflictGroupCol(useDb, by);

        SyncConflictQueryVo f = (filter == null ? new SyncConflictQueryVo() : filter);
        String entityTypeStr = (f.getEntityType() == null ? null : f.getEntityType().name());
        String statusStr = (f.getStatus() == null ? null : f.getStatus().name());
        String conflictTypeStr = (f.getConflictType() == null ? null : f.getConflictType().name());

        return conflictMapper(useDb).agg(b, e, groupCol, entityTypeStr, statusStr, conflictTypeStr);
    }

    private String mapConflictGroupCol(DatabaseType db, SyncConflictAggBy by) {
        boolean isSqlServer = db == DatabaseType.SQLSERVER;
        return switch (by) {
            case STATUS -> isSqlServer ? "c.[status]" : "c.status";
            case CONFLICT_TYPE -> "c.conflict_type";
            case ENTITY_TYPE -> "c.entity_type";
        };
    }

    @Override
    public List<AggVo> aggOpenByEntityType(DatabaseType db, LocalDateTime begin, LocalDateTime end) {
        List<SyncConflict> list = listOpenBetween(db, begin, end);
        Map<String, Long> map = new HashMap<>();
        for (SyncConflict c : list) {
            if (c == null || c.getEntityType() == null) continue;
            map.merge(c.getEntityType().name(), 1L, Long::sum);
        }
        return toAggList(map);
    }

    @Override
    public List<AggVo> aggOpenByConflictType(DatabaseType db, LocalDateTime begin, LocalDateTime end) {
        List<SyncConflict> list = listOpenBetween(db, begin, end);
        Map<String, Long> map = new HashMap<>();
        for (SyncConflict c : list) {
            if (c == null || c.getConflictType() == null) continue;
            map.merge(c.getConflictType().name(), 1L, Long::sum);
        }
        return toAggList(map);
    }

    private List<SyncConflict> listOpenBetween(DatabaseType db, LocalDateTime begin, LocalDateTime end) {
        DatabaseType useDb = useDb(db);

        LambdaQueryWrapper<SyncConflict> qw = new LambdaQueryWrapper<SyncConflict>()
                .eq(SyncConflict::getStatus, ConflictStatus.OPEN);

        if (begin != null) qw.ge(SyncConflict::getLastSeenAt, begin);
        if (end != null) qw.le(SyncConflict::getLastSeenAt, end);

        qw.orderByDesc(SyncConflict::getLastSeenAt);
        return resolveMapper(useDb).selectList(qw);
    }

    private List<AggVo> toAggList(Map<String, Long> map) {
        List<AggVo> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : map.entrySet()) {
            AggVo vo = new AggVo();
            vo.setKey(e.getKey());
            vo.setCount(e.getValue());
            list.add(vo);
        }
        list.sort((a, b) -> Long.compare(
                b.getCount() == null ? 0 : b.getCount(),
                a.getCount() == null ? 0 : a.getCount()
        ));
        return list;
    }

    @Override
    public SyncConflictDetailVo getDetailWithRows(DatabaseType db, String conflictId) {
        // conflict 在哪个库读？按你现有设计：从请求 db 读（db 为空会 useDb 回落）
        SyncConflict conflict = this.resolveMapper(db).selectById(conflictId);
        if (conflict == null) return null;

        List<SyncConflictItem> items = conflictItemService.listByConflictId(db, conflictId);

        SyncConflictDetailVo vo = SyncConflictConverter.toDetailVo(conflict, items);

        // 补 rowJson：演示版（原样 JSON）
        if (vo != null && vo.getItems() != null) {
            for (SyncConflictItemVo it : vo.getItems()) {
                if (it == null) continue;

                // MISSING：不存在就不给 json
                if (it.getExistsFlag() == null || it.getExistsFlag() == 0) {
                    it.setRowJson(null);
                    continue;
                }

                Object entity = fetchEntity(it.getDbType(), vo.getEntityType(), vo.getEntityId());
                if (entity == null) {
                    it.setRowJson(null);
                } else {
                    try {
                        it.setRowJson(objectMapper.writeValueAsString(entity));
                    } catch (Exception e) {
                        it.setRowJson("{\"_error\":\"json_serialize_failed\"}");
                    }
                }
            }
        }

        return vo;
    }

    /**
     * 跨实体、跨库读取 entity，并返回实体对象（后续序列化为 JSON）
     */
    private Object fetchEntity(DatabaseType dbType, SyncEntityType entityType, String entityId) {
        if (dbType == null || entityType == null || entityId == null || entityId.isBlank()) {
            return null;
        }
        // 关键：用你基类自带的跨实体取 mapper
        BaseMapper<?> mapper = this.resolveBaseMapperOf(entityType, dbType);
        return (mapper == null) ? null : mapper.selectById(entityId);
    }


    /* ======================== Snapshot adapter：把“取 mapper”注入 SnapshotUtil ======================== */

    private SnapshotUtil.Snapshot readSnapshot(SyncEntityType entityType, String entityId) {
        return SnapshotUtil.readSnapshot(entityType, entityId, this::resolveBaseMapperOf);
    }

    private String upsertConflictBySnapshot(SyncEntityType entityType,
                                            String entityId,
                                            ConflictType conflictType,
                                            SnapshotUtil.Snapshot snap,
                                            LocalDateTime now,
                                            boolean doNotify) {
        if (conflictType == null || snap == null) return null;

        LocalDateTime t = nvlNow(now);
        DatabaseType workDb = this.defaultDb();

        SyncConflict conflict = findByEntity(workDb, entityType, entityId);

        if (conflict == null) {
            conflict = new SyncConflict();
            conflict.setId(UUID.randomUUID().toString().replace("-", ""));
            conflict.setEntityType(entityType);
            conflict.setEntityId(entityId);

            conflict.setStatus(ConflictStatus.OPEN);
            conflict.setConflictType(conflictType);

            conflict.setFirstSeenAt(t);
            conflict.setLastSeenAt(t);
            conflict.setLastCheckedAt(t);
            conflict.setNotifyCount(0);

            try {
                saveInDb(workDb, conflict);
            } catch (Exception e) {
                log.warn("[CONFLICT] save conflict maybe duplicated, will re-query. entityType={}, entityId={}, err={}",
                        entityType, entityId, e.getMessage());
            }

            conflict = findByEntity(workDb, entityType, entityId);
            if (conflict == null) {
                log.warn("[CONFLICT] create conflict failed after re-query, entityType={}, entityId={}", entityType, entityId);
                return null;
            }
        } else {
            if (conflict.getStatus() == ConflictStatus.RESOLVED) {
                conflict.setStatus(ConflictStatus.OPEN);
                conflict.setResolvedAt(null);
                conflict.setResolutionSourceDb(null);
                conflict.setResolutionNote(null);
                conflict.setLastNotifiedAt(null);
            }

            conflict.setConflictType(conflictType);
            conflict.setLastSeenAt(t);
            conflict.setLastCheckedAt(t);
            if (conflict.getFirstSeenAt() == null) conflict.setFirstSeenAt(t);

            updateByIdInDb(workDb, conflict);
        }

        conflictItemService.upsertSnapshotItems(conflict.getId(), snap.exists, snap.hash, t);

        if (doNotify) {
            tryNotifyConflict(t);
        }

        log.info("[CONFLICT] detected: type={}, entityType={}, entityId={}, conflictId={}",
                conflictType, entityType, entityId, conflict.getId());

        return conflict.getId();
    }

    private SyncConflict findByEntity(DatabaseType db, SyncEntityType entityType, String entityId) {
        return resolveMapper(db).selectOne(new QueryWrapper<SyncConflict>()
                .eq("entity_type", entityType.name())
                .eq("entity_id", entityId));
    }

    private static LocalDateTime nvlNow(LocalDateTime now) {
        return now == null ? LocalDateTime.now() : now;
    }
}




