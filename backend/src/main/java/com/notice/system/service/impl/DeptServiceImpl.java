package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.converter.DeptConverter;
import com.notice.system.entity.Dept;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.service.DeptService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.sync.SyncMetadataRegistry;
import com.notice.system.vo.dept.DeptTreeVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 部门服务实现
 *
 * <p>约定：</p>
 * <ul>
 *   <li>defaultDb = MYSQL</li>
 *   <li>写操作走父类（成功后自动提交同步任务）</li>
 *   <li>读操作允许选库；db 为空时自动回落 defaultDb</li>
 * </ul>
 */
@Slf4j
@Service
public class DeptServiceImpl extends MultiDbSyncServiceImpl<Dept> implements DeptService {

    public DeptServiceImpl(SyncService syncService, SyncMetadataRegistry metadataRegistry) {
        super(syncService, metadataRegistry, SyncEntityType.DEPT, DatabaseType.MYSQL);
    }

    @Override
    public List<Dept> listByIdsFromDb(DatabaseType db, Collection<String> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        DatabaseType useDb = useDb(db);
        BaseMapper<Dept> mapper = resolveMapper(useDb);

        // 去空/去重，避免 selectBatchIds 多余查询
        Set<String> uniq = new HashSet<>();
        for (String id : ids) {
            if (id != null && !id.isBlank()) uniq.add(id.trim());
        }
        if (uniq.isEmpty()) return List.of();

        return Optional.ofNullable(mapper.selectBatchIds(uniq)).orElseGet(List::of);
    }

    /* ===================== 按名称查询 ===================== */

    @Override
    public Dept findByName(String name) {
        return findByNameInDb(defaultDb(), name);
    }

    @Override
    public Dept findByNameInDb(DatabaseType db, String name) {
        if (name == null || name.isBlank()) return null;

        DatabaseType useDb = useDb(db);
        BaseMapper<Dept> mapper = resolveMapper(useDb);

        return mapper.selectOne(new LambdaQueryWrapper<Dept>()
                .eq(Dept::getName, name.trim()));
    }

    /* ===================== 启用部门列表 ===================== */

    @Override
    public List<Dept> listEnabled() {
        return listEnabledFromDb(defaultDb());
    }

    @Override
    public List<Dept> listEnabledFromDb(DatabaseType db) {
        DatabaseType useDb = useDb(db);
        BaseMapper<Dept> mapper = resolveMapper(useDb);

        return Optional.ofNullable(mapper.selectList(new LambdaQueryWrapper<Dept>()
                        .eq(Dept::getStatus, 1)
                        .orderByAsc(Dept::getSortOrder, Dept::getCreateTime)))
                .orElseGet(List::of);
    }

    @Override
    public List<Dept> listEnabledForParentSelect(DatabaseType db, String childDeptId) {
        DatabaseType useDb = useDb(db);

        List<Dept> enabled = listEnabledFromDb(useDb);
        if (enabled.isEmpty()) return enabled;

        // 新增：不排除
        if (childDeptId == null || childDeptId.isBlank()) return enabled;

        Set<Dept> ban = listAllChildByParentIdFromDb(useDb, childDeptId.trim());
        if (ban.isEmpty()) return enabled;

        Set<String> banIds = new HashSet<>();
        for (Dept d : ban) {
            if (d != null && d.getId() != null && !d.getId().isBlank()) banIds.add(d.getId());
        }

        return enabled.stream()
                .filter(d -> d != null && d.getId() != null && !banIds.contains(d.getId()))
                .toList();
    }

    /* ===================== 按父部门查询 ===================== */

    @Override
    public List<Dept> listByParentId(String parentId) {
        return listByParentIdFromDb(defaultDb(), parentId);
    }

    @Override
    public List<Dept> listByParentIdFromDb(DatabaseType db, String parentId) {
        DatabaseType useDb = useDb(db);
        BaseMapper<Dept> mapper = resolveMapper(useDb);

        LambdaQueryWrapper<Dept> w = new LambdaQueryWrapper<>();
        if (parentId == null || parentId.isBlank()) {
            w.isNull(Dept::getParentId);
        } else {
            w.eq(Dept::getParentId, parentId.trim());
        }
        w.orderByAsc(Dept::getSortOrder, Dept::getCreateTime);

        return Optional.ofNullable(mapper.selectList(w)).orElseGet(List::of);
    }

    @Override
    public Set<Dept> listAllChildByParentIdFromDb(DatabaseType db, String parentId) {
        if (parentId == null || parentId.isBlank()) return Set.of();

        DatabaseType useDb = useDb(db);
        BaseMapper<Dept> mapper = resolveMapper(useDb);

        // BFS：包含自身 + 所有子孙，防环
        Set<Dept> result = new LinkedHashSet<>();
        Set<String> visited = new HashSet<>();
        Deque<String> q = new ArrayDeque<>();

        Dept self = mapper.selectById(parentId.trim());
        if (self != null && self.getId() != null && !self.getId().isBlank()) {
            result.add(self);
            visited.add(self.getId());
            q.add(self.getId());
        } else {
            return Set.of();
        }

        int guard = 0;
        while (!q.isEmpty()) {
            String cur = q.poll();
            List<Dept> children = listByParentIdFromDb(useDb, cur);

            for (Dept c : children) {
                if (c == null || c.getId() == null || c.getId().isBlank()) continue;
                if (visited.add(c.getId())) {
                    result.add(c);
                    q.add(c.getId());
                }
            }

            if (++guard > 200000) {
                log.warn("[DEPT] listAllChildByParentIdFromDb guard triggered, parentId={}, db={}", parentId, useDb);
                break;
            }
        }

        return result;
    }

    @Override
    public Set<String> listSelfAndAncestorsIdsFromDb(DatabaseType db, String deptId) {
        if (deptId == null || deptId.isBlank()) return Set.of();

        DatabaseType useDb = useDb(db);

        Set<String> ids = new LinkedHashSet<>();
        String cur = deptId.trim();

        while (cur != null && !cur.isBlank()) {
            if (!ids.add(cur)) break; // 防环
            Dept d = getById(useDb, cur);
            if (d == null) break;
            cur = d.getParentId();
        }
        return ids;
    }

    /* ===================== 是否存在子部门 ===================== */

    @Override
    public boolean hasChildren(String deptId) {
        return hasChildrenInDb(defaultDb(), deptId);
    }

    @Override
    public boolean hasChildrenInDb(DatabaseType db, String deptId) {
        if (deptId == null || deptId.isBlank()) return false;

        DatabaseType useDb = useDb(db);
        BaseMapper<Dept> mapper = resolveMapper(useDb);

        Long cnt = mapper.selectCount(new LambdaQueryWrapper<Dept>()
                .eq(Dept::getParentId, deptId.trim()));

        return cnt != null && cnt > 0;
    }

    /* ===================== 树结构构建 ===================== */

    @Override
    public List<DeptTreeVo> buildTree(List<Dept> depts) {
        if (depts == null || depts.isEmpty()) return List.of();

        // 1) id -> node（保持输入顺序稳定）
        Map<String, DeptTreeVo> id2Node = new LinkedHashMap<>();
        for (Dept d : depts) {
            if (d == null || d.getId() == null || d.getId().isBlank()) continue;
            id2Node.put(d.getId(), DeptConverter.toTreeVo(d));
        }
        if (id2Node.isEmpty()) return List.of();

        // 2) 组装父子
        List<DeptTreeVo> roots = new ArrayList<>();
        for (Dept d : depts) {
            if (d == null || d.getId() == null || d.getId().isBlank()) continue;

            DeptTreeVo node = id2Node.get(d.getId());
            if (node == null) continue;

            String pid = d.getParentId();
            if (pid == null || pid.isBlank()) {
                roots.add(node);
                continue;
            }

            DeptTreeVo parent = id2Node.get(pid);
            if (parent != null) {
                parent.getChildren().add(node);
            } else {
                // 父节点不存在：兜底当根
                roots.add(node);
            }
        }

        return roots;
    }
}




