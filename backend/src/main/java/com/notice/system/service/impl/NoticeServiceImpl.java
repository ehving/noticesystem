package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.Dept;
import com.notice.system.entity.Notice;
import com.notice.system.entity.NoticeTargetDept;
import com.notice.system.entity.User;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.service.*;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.sync.SyncMetadataRegistry;
import com.notice.system.vo.notice.NoticeAdminPageVo;
import com.notice.system.vo.notice.NoticeAdminRowVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 公告服务实现（Notice）
 *
 * <p>职责：</p>
 * <ul>
 *   <li>公告本体 CRUD（多库同步）</li>
 *   <li>公告与部门范围 notice_target_dept 的维护</li>
 *   <li>用户侧可见性过滤（GLOBAL / 定向部门，含祖先部门可见）</li>
 * </ul>
 */
@Slf4j
@Service
public class NoticeServiceImpl extends MultiDbSyncServiceImpl<Notice> implements NoticeService {

    private final NoticeTargetDeptService noticeTargetDeptService;
    private final AuthService authService;
    private final DeptService deptService;

    public NoticeServiceImpl(SyncService syncService,
                             SyncMetadataRegistry metadataRegistry,
                             NoticeTargetDeptService noticeTargetDeptService,
                             AuthService authService,
                             DeptService deptService) {
        super(syncService, metadataRegistry, SyncEntityType.NOTICE, DatabaseType.MYSQL);
        this.noticeTargetDeptService = noticeTargetDeptService;
        this.authService = authService;
        this.deptService = deptService;
    }

    /* ===================== 创建 / 更新（公告 + 目标部门） ===================== */

    @Override
    public void createNotice(Notice notice, Collection<String> targetDeptIds) {
        createNoticeInDb(defaultDb(), notice, targetDeptIds);
    }

    @Override
    public void createNoticeInDb(DatabaseType sourceDb, Notice notice, Collection<String> targetDeptIds) {
        DatabaseType db = useDb(sourceDb);
        Objects.requireNonNull(notice, "notice must not be null");

        LocalDateTime now = LocalDateTime.now();
        if (notice.getCreateTime() == null) notice.setCreateTime(now);
        notice.setUpdateTime(now);

        // 默认值：状态/等级/浏览数
        if (blank(notice.getStatus())) notice.setStatus("DRAFT");
        if (blank(notice.getLevel())) notice.setLevel("NORMAL");
        if (notice.getViewCount() == null) notice.setViewCount(0L);

        // 1) 保存公告（写成功后以 db 为源库触发同步）
        saveInDb(db, notice);

        // 2) 保存范围：空集合表示 GLOBAL（无关联记录）
        List<String> deptIds = normalizeDeptIds(targetDeptIds);
        for (String deptId : deptIds) {
            NoticeTargetDept rel = new NoticeTargetDept();
            rel.setNoticeId(notice.getId());
            rel.setDeptId(deptId);
            noticeTargetDeptService.saveInDb(db, rel);
        }

        log.info("[NOTICE] created: id={}, sourceDb={}, scope={}",
                notice.getId(), db, deptIds.isEmpty() ? "GLOBAL" : ("DEPT(" + deptIds.size() + ")"));
    }

    @Override
    public void updateNotice(Notice notice, Collection<String> targetDeptIds) {
        updateNoticeInDb(defaultDb(), notice, targetDeptIds);
    }

    @Override
    public void updateNoticeInDb(DatabaseType sourceDb, Notice notice, Collection<String> targetDeptIds) {
        DatabaseType db = useDb(sourceDb);
        Objects.requireNonNull(notice, "notice must not be null");
        if (blank(notice.getId())) throw new IllegalArgumentException("notice.id is required");

        LocalDateTime now = LocalDateTime.now();
        notice.setUpdateTime(now);

        // 1) 更新公告（以 db 为源库触发同步）
        updateByIdInDb(db, notice);

        // 2) 先删旧关联（以 db 为源库触发同步删除）
        noticeTargetDeptService.removeByNoticeIdInDb(db, notice.getId());

        // 3) 再插新关联（空集合表示 GLOBAL）
        List<String> deptIds = normalizeDeptIds(targetDeptIds);
        for (String deptId : deptIds) {
            NoticeTargetDept rel = new NoticeTargetDept();
            rel.setNoticeId(notice.getId());
            rel.setDeptId(deptId);
            noticeTargetDeptService.saveInDb(db, rel);
        }

        log.info("[NOTICE] updated: id={}, sourceDb={}, scope={}",
                notice.getId(), db, deptIds.isEmpty() ? "GLOBAL" : ("DEPT(" + deptIds.size() + ")"));
    }

    private List<String> normalizeDeptIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    /* ===================== 发布 / 撤回 ===================== */

    @Override
    public boolean publishNoticeNowInDb(DatabaseType sourceDb, String noticeId) {
        DatabaseType db = useDb(sourceDb);
        if (blank(noticeId)) return false;

        Notice n = getById(db, noticeId);
        if (n == null) return false;

        String st = n.getStatus();
        if ("PUBLISHED".equalsIgnoreCase(st)) return true; // 已发布视为成功
        if (!"DRAFT".equalsIgnoreCase(st) && !"RECALLED".equalsIgnoreCase(st)) return false;

        LocalDateTime now = LocalDateTime.now();
        n.setStatus("PUBLISHED");
        n.setPublishTime(now);
        n.setUpdateTime(now);

        return updateByIdInDb(db, n);
    }

    @Override
    public void publishDueDraftsInDb(DatabaseType sourceDb) {
        DatabaseType db = useDb(sourceDb);
        LocalDateTime now = LocalDateTime.now();

        // ⚠️ 不用 LIMIT，改用分页查询：MySQL/PG/SQLServer 通吃
        BaseMapper<Notice> mapper = resolveMapper(db);
        Page<Notice> page = new Page<>(1, 200);

        LambdaQueryWrapper<Notice> w = new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, "DRAFT")
                .isNotNull(Notice::getPublishTime)
                .le(Notice::getPublishTime, now)
                .orderByAsc(Notice::getPublishTime);

        List<Notice> due = mapper.selectPage(page, w).getRecords();
        if (due == null || due.isEmpty()) return;

        for (Notice n : due) {
            if (n == null || blank(n.getId())) continue;
            try {
                publishNoticeNowInDb(db, n.getId());
            } catch (Exception e) {
                log.error("[NOTICE] auto publish failed: id={}, db={}", n.getId(), db, e);
            }
        }
    }

    @Override
    public boolean recallNotice(String noticeId) {
        return recallNoticeInDb(defaultDb(), noticeId);
    }

    @Override
    public boolean recallNoticeInDb(DatabaseType sourceDb, String noticeId) {
        DatabaseType db = useDb(sourceDb);
        if (blank(noticeId)) return false;

        Notice n = getById(db, noticeId);
        if (n == null) return false;

        if (!"PUBLISHED".equalsIgnoreCase(n.getStatus())) {
            return false;
        }

        n.setStatus("RECALLED");
        n.setUpdateTime(LocalDateTime.now());

        return updateByIdInDb(db, n);
    }

    /* ===================== 用户侧分页：已发布 + 有效期 + 可见性 ===================== */

    @Override
    public Page<Notice> pagePublishedForUser(long pageNo, long pageSize, String keyword, String level) {
        User user = authService.requireLoginUser();

        long pn = (pageNo <= 0 ? 1 : pageNo);
        long ps = (pageSize <= 0 ? 10 : pageSize);

        DatabaseType db = defaultDb();
        BaseMapper<Notice> mapper = resolveMapper(db);

        // 1) 查候选：已发布 + 有效期 + keyword + level
        LambdaQueryWrapper<Notice> w = buildNoticeQuery(keyword, level, null, null, null, null, true);
        List<Notice> candidates = mapper.selectList(w);
        if (candidates == null || candidates.isEmpty()) return new Page<>(pn, ps);

        // 2) 可见性过滤（GLOBAL / 定向部门，祖先部门可见）
        List<Notice> visible = filterVisibleForUser(db, candidates, user.getDeptId());

        // 3) Java 分页（这里为了简单，不上 SQL join）
        return sliceToPage(pn, ps, visible);
    }

    private Page<Notice> sliceToPage(long pageNo, long pageSize, List<Notice> list) {
        Page<Notice> page = new Page<>(pageNo, pageSize);
        if (list == null || list.isEmpty()) {
            page.setTotal(0);
            page.setRecords(List.of());
            return page;
        }

        long total = list.size();
        long from = Math.max(0, (pageNo - 1) * pageSize);
        long to = Math.min(total, from + pageSize);

        page.setTotal(total);
        page.setRecords(from >= total ? List.of() : list.subList((int) from, (int) to));
        return page;
    }

    /* ===================== 管理端分页 ===================== */

    @Override
    public Page<Notice> pageAdminNotices(NoticeAdminPageVo vo) {
        return pageAdminNoticesInDb(defaultDb(), vo);
    }

    @Override
    public Page<Notice> pageAdminNoticesInDb(DatabaseType db, NoticeAdminPageVo vo) {
        DatabaseType useDb = useDb(db);
        NoticeAdminPageVo q = (vo == null ? new NoticeAdminPageVo() : vo);

        //  vo.getPageNo()/getPageSize 可能为 Long 且可为空，避免拆箱 NPE
        long pageNoRaw = q.getPageNo();
        long pageSizeRaw = q.getPageSize();
        long pageNo = pageNoRaw <= 0 ? 1L : pageNoRaw;
        long pageSize = pageSizeRaw <= 0 ? 10L : pageSizeRaw;

        BaseMapper<Notice> mapper = resolveMapper(useDb);

        LambdaQueryWrapper<Notice> w = buildNoticeQuery(
                q.getKeyword(),
                q.getLevel(),
                q.getStatus(),
                q.getPublisherId(),
                q.getStartTime(),
                q.getEndTime(),
                false
        );

        return mapper.selectPage(new Page<>(pageNo, pageSize), w);
    }

    @Override
    public Page<NoticeAdminRowVo> pageAdminNoticesWithScopeInDb(DatabaseType db, NoticeAdminPageVo vo) {
        DatabaseType useDb = useDb(db);

        // 1) 复用管理端分页
        Page<Notice> page = pageAdminNoticesInDb(useDb, vo);
        long current = page.getCurrent();
        long size = page.getSize();

        Page<NoticeAdminRowVo> out = new Page<>(current, size);
        out.setTotal(page.getTotal());

        if (page.getRecords() == null || page.getRecords().isEmpty()) {
            out.setRecords(List.of());
            return out;
        }

        List<Notice> records = page.getRecords();

        // 2) 收集 noticeIds
        List<String> noticeIds = records.stream()
                .filter(Objects::nonNull)
                .map(Notice::getId)
                .filter(id -> id != null && !id.isBlank())
                .toList();

        if (noticeIds.isEmpty()) {
            out.setRecords(List.of());
            return out;
        }

        // 3) 批量查 notice_target_dept
        List<NoticeTargetDept> rels = noticeTargetDeptService.listByNoticeIdsFromDb(useDb, noticeIds);

        Map<String, List<String>> noticeToDeptIds = new HashMap<>();
        Set<String> allDeptIds = new HashSet<>();

        if (rels != null) {
            for (NoticeTargetDept r : rels) {
                if (r == null) continue;
                String nid = r.getNoticeId();
                String did = r.getDeptId();
                if (blank(nid) || blank(did)) continue;

                noticeToDeptIds.computeIfAbsent(nid, k -> new ArrayList<>()).add(did);
                allDeptIds.add(did);
            }
        }

        // 4) 批量查 deptName
        Map<String, String> deptNameMap = new HashMap<>();
        if (!allDeptIds.isEmpty()) {
            List<Dept> depts = deptService.listByIdsFromDb(useDb, allDeptIds);
            for (Dept d : Optional.ofNullable(depts).orElseGet(List::of)) {
                if (d == null || blank(d.getId())) continue;
                String name = blank(d.getName()) ? d.getId() : d.getName().trim();
                deptNameMap.put(d.getId(), name);
            }
        }

        // 5) 组装输出
        final int PREVIEW_LIMIT = 3;

        List<NoticeAdminRowVo> rows = new ArrayList<>(records.size());
        for (Notice n : records) {
            if (n == null) continue;

            String nid = n.getId();
            List<String> deptIds = (nid == null ? List.of()
                    : noticeToDeptIds.getOrDefault(nid, List.of()));

            List<String> uniqDeptIds = deptIds.stream()
                    .filter(x -> x != null && !x.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();

            boolean global = uniqDeptIds.isEmpty();

            NoticeAdminRowVo row = new NoticeAdminRowVo();
            row.setNotice(n);

            if (global) {
                row.setScopeType("GLOBAL");
                row.setTargetDeptCount(0);
                row.setTargetDeptIds(List.of());
                row.setTargetDeptNamesPreview(List.of());
            } else {
                row.setScopeType("DEPT");
                row.setTargetDeptCount(uniqDeptIds.size());
                row.setTargetDeptIds(uniqDeptIds);

                List<String> preview = new ArrayList<>();
                for (String did : uniqDeptIds) {
                    preview.add(deptNameMap.getOrDefault(did, did));
                    if (preview.size() >= PREVIEW_LIMIT) break;
                }
                row.setTargetDeptNamesPreview(preview);
            }

            rows.add(row);
        }

        out.setRecords(rows);
        return out;
    }

    /* ===================== 可见性过滤（GLOBAL / 定向部门 + 祖先可见） ===================== */

    private List<Notice> filterVisibleForUser(DatabaseType db, List<Notice> candidates, String userDeptId) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        List<String> noticeIds = candidates.stream()
                .map(Notice::getId)
                .filter(id -> id != null && !id.isBlank())
                .toList();

        List<NoticeTargetDept> rels = noticeTargetDeptService.listByNoticeIdsFromDb(db, noticeIds);

        Map<String, Set<String>> noticeToDeptIds = new HashMap<>();
        if (rels != null) {
            for (NoticeTargetDept r : rels) {
                if (r == null) continue;
                String nid = r.getNoticeId();
                String did = r.getDeptId();
                if (blank(nid) || blank(did)) continue;
                noticeToDeptIds.computeIfAbsent(nid, k -> new HashSet<>()).add(did);
            }
        }

        boolean userNoDept = blank(userDeptId);
        Set<String> selfAndAncestors = userNoDept ? Set.of() : deptService.listSelfAndAncestorsIdsFromDb(db, userDeptId.trim());

        List<Notice> visible = new ArrayList<>(candidates.size());
        for (Notice n : candidates) {
            if (n == null || blank(n.getId())) continue;

            Set<String> targets = noticeToDeptIds.get(n.getId());

            // GLOBAL：没有关联或关联为空
            boolean isGlobal = (targets == null || targets.isEmpty());
            if (isGlobal) {
                visible.add(n);
                continue;
            }

            // 用户无部门：只能看 GLOBAL
            if (userNoDept) continue;

            // 祖先可见：targets 与 selfAndAncestors 有交集即可
            boolean ok = false;
            for (String td : targets) {
                if (selfAndAncestors.contains(td)) { ok = true; break; }
            }
            if (ok) visible.add(n);
        }

        return visible;
    }

    /* ===================== 查询条件构建 ===================== */

    private LambdaQueryWrapper<Notice> buildNoticeQuery(
            String keyword,
            String level,
            String status,
            String publisherId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            boolean onlyPublishedEffective
    ) {
        LambdaQueryWrapper<Notice> w = new LambdaQueryWrapper<>();

        if (onlyPublishedEffective) {
            LocalDateTime now = LocalDateTime.now();
            w.eq(Notice::getStatus, "PUBLISHED")
                    .le(Notice::getPublishTime, now)
                    .and(x -> x.isNull(Notice::getExpireTime).or().gt(Notice::getExpireTime, now))
                    .orderByDesc(Notice::getPublishTime);
        } else {
            w.orderByDesc(Notice::getCreateTime);
        }

        if (!blank(keyword)) {
            String kw = keyword.trim();
            w.and(x -> x.like(Notice::getTitle, kw).or().like(Notice::getContent, kw));
        }
        if (!blank(level)) w.eq(Notice::getLevel, level.trim());
        if (!blank(status)) w.eq(Notice::getStatus, status.trim());
        if (!blank(publisherId)) w.eq(Notice::getPublisherId, publisherId.trim());

        if (startTime != null) w.ge(Notice::getPublishTime, startTime);
        if (endTime != null) w.le(Notice::getPublishTime, endTime);

        return w;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}






