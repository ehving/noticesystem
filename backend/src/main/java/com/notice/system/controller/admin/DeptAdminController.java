package com.notice.system.controller.admin;

import com.notice.system.common.Result;
import com.notice.system.converter.DeptConverter;
import com.notice.system.entity.Dept;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.service.AuthService;
import com.notice.system.service.DeptService;
import com.notice.system.vo.dept.DeptOptionVo;
import com.notice.system.vo.dept.DeptTreeVo;
import com.notice.system.vo.dept.DeptVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端-部门管理接口
 *
 * <p>特点：</p>
 * <ul>
 *   <li>所有接口支持 ?db=MYSQL/PG/SQLSERVER 选库</li>
 *   <li>写操作通过 DeptService 的多库同步框架落库并触发同步</li>
 *   <li>更新时防止出现“自己/子孙当父级”的环</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/dept")
@RequiredArgsConstructor
public class DeptAdminController {

    private final AuthService authService;
    private final DeptService deptService;

    /* ==================== 列表 / 详情 / 树 ==================== */

    /** 部门列表（简单筛选 + 排序） */
    @GetMapping("/list")
    public Result<List<DeptVo>> list(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestParam(value = "name", required = false) String nameKeyword,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        DatabaseType useDb = useDb(db);
        authService.requireAdmin(useDb);

        List<Dept> all = deptService.listAll(useDb);
        if (all == null || all.isEmpty()) {
            return Result.success(List.of());
        }

        String kw = trimToNull(nameKeyword);

        List<Dept> filtered = all.stream()
                .filter(Objects::nonNull)
                .filter(d -> status == null || Objects.equals(d.getStatus(), status))
                .filter(d -> kw == null || Optional.ofNullable(d.getName()).orElse("").contains(kw))
                .sorted(Comparator
                        .comparing((Dept d) -> Optional.ofNullable(d.getSortOrder()).orElse(Integer.MAX_VALUE))
                        .thenComparing(Dept::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                )
                .collect(Collectors.toList());

        // parentName 映射（同库）
        Map<String, String> id2Name = all.stream()
                .filter(Objects::nonNull)
                .filter(d -> d.getId() != null)
                .collect(Collectors.toMap(Dept::getId, Dept::getName, (a, b) -> a));

        return Result.success(DeptConverter.toDeptVoList(filtered, id2Name));
    }

    /** 部门详情 */
    @GetMapping("/{id}")
    public Result<DeptVo> getDetail(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = useDb(db);
        authService.requireAdmin(useDb);

        Dept dept = deptService.getById(useDb, id);
        if (dept == null) {
            return Result.fail("部门不存在");
        }

        String parentName = null;
        String parentId = trimToNull(dept.getParentId());
        if (parentId != null) {
            Dept parent = deptService.getById(useDb, parentId);
            parentName = (parent == null ? null : parent.getName());
        }

        return Result.success(DeptConverter.toDeptVo(dept, parentName));
    }

    /** 部门树 */
    @GetMapping("/tree")
    public Result<List<DeptTreeVo>> getDeptTree(
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = useDb(db);
        authService.requireAdmin(useDb);

        List<Dept> all = deptService.listAll(useDb);
        return Result.success(deptService.buildTree(all));
    }

    /* ==================== 创建 ==================== */

    @PostMapping
    public Result<DeptVo> create(
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody DeptVo vo
    ) {
        DatabaseType useDb = useDb(db);
        authService.requireAdmin(useDb);

        String name = (vo == null ? null : trimToNull(vo.getName()));
        if (name == null) {
            return Result.fail("部门名称不能为空");
        }

        // 名称唯一（同库）
        Dept exist = deptService.findByNameInDb(useDb, name);
        if (exist != null) {
            return Result.fail("部门名称已存在");
        }

        Dept dept = DeptConverter.toEntityForCreate(vo);

        // 父部门合法性（同库）
        String parentId = trimToNull(dept.getParentId());
        if (parentId != null) {
            Dept parent = deptService.getById(useDb, parentId);
            if (parent == null) {
                return Result.fail("上级部门不存在");
            }
            dept.setParentId(parentId);
        }

        LocalDateTime now = LocalDateTime.now();
        dept.setCreateTime(now);
        dept.setUpdateTime(now);

        boolean ok = deptService.saveInDb(useDb, dept);
        if (!ok) {
            return Result.fail("创建部门失败");
        }

        String parentName = null;
        if (parentId != null) {
            Dept parent = deptService.getById(useDb, parentId);
            parentName = (parent == null ? null : parent.getName());
        }

        return Result.success(DeptConverter.toDeptVo(dept, parentName));
    }

    /* ==================== 更新 ==================== */

    @PutMapping("/{id}")
    public Result<DeptVo> update(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestBody DeptVo vo
    ) {
        DatabaseType useDb = useDb(db);
        authService.requireAdmin(useDb);

        if (vo == null) {
            return Result.fail("请求体不能为空");
        }

        Dept dept = deptService.getById(useDb, id);
        if (dept == null) {
            return Result.fail("部门不存在");
        }

        // 1) 先做“父级”环校验：parentId 不能是自身或子孙
        String newParentId = trimToNull(vo.getParentId());
        if (newParentId != null) {
            // 不能把自己当父级
            if (id.equals(newParentId)) {
                return Result.fail("上级部门不能是自身");
            }
            // 不能把子孙当父级：取当前部门的所有子孙集合，parentId 不允许出现在其中
            Set<Dept> selfAndChildren = deptService.listAllChildByParentIdFromDb(useDb, id);
            for (Dept child : selfAndChildren) {
                if (child != null && idNotBlank(child.getId()) && newParentId.equals(child.getId())) {
                    return Result.fail("上级部门不能是自身及子孙节点");
                }
            }
        }

        // 2) 名称唯一（同库，排除自己）
        String newName = trimToNull(vo.getName());
        if (newName != null) {
            Dept other = deptService.findByNameInDb(useDb, newName);
            if (other != null && !id.equals(other.getId())) {
                return Result.fail("部门名称已被其他部门使用");
            }
        }

        // 3) 写回实体（converter 内部按你的字段策略复制）
        DeptConverter.copyForUpdate(vo, dept);

        // 4) 父部门合法性（同库）
        String parentId = trimToNull(dept.getParentId());
        if (parentId != null) {
            Dept parent = deptService.getById(useDb, parentId);
            if (parent == null) {
                return Result.fail("上级部门不存在");
            }
            dept.setParentId(parentId);
        }

        dept.setUpdateTime(LocalDateTime.now());

        boolean ok = deptService.updateByIdInDb(useDb, dept);
        if (!ok) {
            return Result.fail("更新部门失败");
        }

        String parentName = null;
        if (parentId != null) {
            Dept parent = deptService.getById(useDb, parentId);
            parentName = (parent == null ? null : parent.getName());
        }

        return Result.success(DeptConverter.toDeptVo(dept, parentName));
    }

    /* ==================== 删除 ==================== */

    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = useDb(db);
        authService.requireAdmin(useDb);

        Dept dept = deptService.getById(useDb, id);
        if (dept == null) {
            return Result.fail("部门不存在");
        }

        // 子部门校验（同库）
        if (deptService.hasChildrenInDb(useDb, id)) {
            return Result.fail("存在子部门，无法删除，请先删除子部门");
        }

        // ✅ 建议：后续补充“用户引用 / 公告范围引用”校验（我下一步可以直接帮你加进 DeptServiceImpl.removeByIdInDb）
        boolean ok = deptService.removeByIdInDb(useDb, id);
        if (!ok) {
            return Result.fail("删除部门失败");
        }
        return Result.success(null);
    }

    /* ==================== 启用 / 停用 ==================== */

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(
            @PathVariable("id") String id,
            @RequestParam(name = "db", required = false) DatabaseType db,
            @RequestParam("status") Integer status
    ) {
        DatabaseType useDb = useDb(db);
        authService.requireAdmin(useDb);

        if (status == null || (status != 0 && status != 1)) {
            return Result.fail("状态值非法，应为 0 或 1");
        }

        Dept dept = deptService.getById(useDb, id);
        if (dept == null) {
            return Result.fail("部门不存在");
        }

        dept.setStatus(status);
        dept.setUpdateTime(LocalDateTime.now());

        boolean ok = deptService.updateByIdInDb(useDb, dept);
        if (!ok) {
            return Result.fail("修改状态失败");
        }
        return Result.success(null);
    }

    /* ==================== 下拉选项 ==================== */

    /**
     * 父部门下拉选项（用于编辑部门时选择父级）
     * <p>会排除 ChildDeptId 自身及其子孙，避免出现环。</p>
     */
    @GetMapping("/parent-options")
    public Result<List<DeptOptionVo>> parentOptions(
            @RequestParam(name = "ChildDeptId", required = false) String childDeptId,
            @RequestParam(name = "db", required = false) DatabaseType db
    ) {
        DatabaseType useDb = useDb(db);
        authService.requireAdmin(useDb);

        List<Dept> list = deptService.listEnabledForParentSelect(useDb, childDeptId);
        return Result.success(DeptConverter.toOptionList(list));
    }

    /* ==================== utils ==================== */

    private DatabaseType useDb(DatabaseType db) {
        return (db == null ? deptService.defaultDb() : db);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean idNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}


