package com.notice.system.controller.admin;

import com.notice.system.common.Result;
import com.notice.system.converter.DeptConverter;
import com.notice.system.entity.Dept;
import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.service.AuthService;
import com.notice.system.service.DeptService;
import com.notice.system.service.RoleService;
import com.notice.system.vo.dept.DeptVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门后台管理接口：
 *  - 需要管理员权限
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/dept")
@RequiredArgsConstructor
public class DeptAdminController {

    private final AuthService authService;
    private final RoleService roleService;
    private final DeptService deptService;

    /* ==================== 列表 & 详情 ==================== */

    /**
     * 部门列表（简单版）：
     *  - 可按 name 模糊、status 过滤
     *  - 暂不做服务端分页，前端可自行分页
     */
    @GetMapping("/list")
    public Result<List<DeptVo>> list(
            @RequestParam(value = "name", required = false) String nameKeyword,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        requireAdmin();

        List<Dept> all = deptService.listAll();
        if (all.isEmpty()) {
            return Result.success(List.of());
        }

        // 过滤
        List<Dept> filtered = all.stream()
                .filter(d -> {
                    if (status != null && !Objects.equals(d.getStatus(), status)) {
                        return false;
                    }
                    if (nameKeyword != null && !nameKeyword.isBlank()) {
                        String n = Optional.ofNullable(d.getName()).orElse("");
                        return n.contains(nameKeyword.trim());
                    }
                    return true;
                })
                .sorted(Comparator
                        .comparing((Dept d) -> Optional.ofNullable(d.getSortOrder()).orElse(Integer.MAX_VALUE))
                        .thenComparing(Dept::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                )
                .collect(Collectors.toList());

        // parentName 映射
        Map<String, String> id2Name = all.stream()
                .collect(Collectors.toMap(Dept::getId, Dept::getName, (a, b) -> a));

        List<DeptVo> voList = DeptConverter.toDeptVoList(filtered, id2Name);
        return Result.success(voList);
    }

    /**
     * 部门详情
     */
    @GetMapping("/{id}")
    public Result<DeptVo> getDetail(@PathVariable("id") String id) {
        requireAdmin();

        Dept dept = deptService.getById(id);
        if (dept == null) {
            return Result.fail("部门不存在");
        }

        String parentName = null;
        if (dept.getParentId() != null) {
            Dept parent = deptService.getById(dept.getParentId());
            parentName = parent != null ? parent.getName() : null;
        }

        DeptVo vo = DeptConverter.toDeptVo(dept, parentName);
        return Result.success(vo);
    }

    /* ==================== 创建 ==================== */

    @PostMapping
    public Result<DeptVo> create(@RequestBody DeptVo vo) {
        requireAdmin();

        if (vo == null || vo.getName() == null || vo.getName().isBlank()) {
            return Result.fail("部门名称不能为空");
        }

        // 名称唯一校验
        Dept exist = deptService.findByName(vo.getName().trim());
        if (exist != null) {
            return Result.fail("部门名称已存在");
        }

        Dept dept = DeptConverter.toEntityForCreate(vo);

        // 父部门合法性校验（允许 parentId 空）
        if (dept.getParentId() != null) {
            Dept parent = deptService.getById(dept.getParentId());
            if (parent == null) {
                return Result.fail("上级部门不存在");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        dept.setCreateTime(now);
        dept.setUpdateTime(now);

        boolean ok = deptService.save(dept);
        if (!ok) {
            return Result.fail("创建部门失败");
        }

        String parentName = null;
        if (dept.getParentId() != null) {
            Dept parent = deptService.getById(dept.getParentId());
            parentName = parent != null ? parent.getName() : null;
        }

        DeptVo resultVo = DeptConverter.toDeptVo(dept, parentName);
        return Result.success(resultVo);
    }

    /* ==================== 更新 ==================== */

    @PutMapping("/{id}")
    public Result<DeptVo> update(@PathVariable("id") String id,
                                 @RequestBody DeptVo vo) {
        requireAdmin();

        if (vo == null) {
            return Result.fail("请求体不能为空");
        }

        Dept dept = deptService.getById(id);
        if (dept == null) {
            return Result.fail("部门不存在");
        }

        // 防止自己作为自己的父级
        if (vo.getParentId() != null && id.equals(vo.getParentId())) {
            return Result.fail("上级部门不能是自身");
        }

        // 名称唯一校验（排除自己）
        if (vo.getName() != null && !vo.getName().isBlank()) {
            Dept other = deptService.findByName(vo.getName().trim());
            if (other != null && !other.getId().equals(id)) {
                return Result.fail("部门名称已被其他部门使用");
            }
        }

        // 覆盖字段
        DeptConverter.copyForUpdate(vo, dept);

        // 父部门合法性校验（允许 null）
        if (dept.getParentId() != null) {
            Dept parent = deptService.getById(dept.getParentId());
            if (parent == null) {
                return Result.fail("上级部门不存在");
            }
        }

        dept.setUpdateTime(LocalDateTime.now());

        boolean ok = deptService.updateById(dept);
        if (!ok) {
            return Result.fail("更新部门失败");
        }

        String parentName = null;
        if (dept.getParentId() != null) {
            Dept parent = deptService.getById(dept.getParentId());
            parentName = parent != null ? parent.getName() : null;
        }

        DeptVo resultVo = DeptConverter.toDeptVo(dept, parentName);
        return Result.success(resultVo);
    }

    /* ==================== 删除 ==================== */

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") String id) {
        requireAdmin();

        Dept dept = deptService.getById(id);
        if (dept == null) {
            return Result.fail("部门不存在");
        }

        // 1) 有子部门则禁止删除
        if (deptService.hasChildren(id)) {
            return Result.fail("存在子部门，无法删除，请先删除子部门");
        }

        // 2) 如需保证没有用户关联，可在这里补充调用 UserService 检查 dept_id
        //    例如：long count = userService.countByDeptId(id); if (count > 0) { ... }
        //    这里先不强依赖这个方法，避免你现在额外改 UserService。

        boolean ok = deptService.removeById(id);
        if (!ok) {
            return Result.fail("删除部门失败");
        }
        return Result.success(null);
    }

    /* ==================== 启用 / 停用 ==================== */

    @PutMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable("id") String id,
                                     @RequestParam("status") Integer status) {
        requireAdmin();

        if (status == null || (status != 0 && status != 1)) {
            return Result.fail("状态值非法，应为 0 或 1");
        }

        Dept dept = deptService.getById(id);
        if (dept == null) {
            return Result.fail("部门不存在");
        }

        dept.setStatus(status);
        dept.setUpdateTime(LocalDateTime.now());

        boolean ok = deptService.updateById(dept);
        if (!ok) {
            return Result.fail("修改状态失败");
        }
        return Result.success(null);
    }

    /* ==================== 权限校验 ==================== */

    /**
     * 判断当前登录用户是否为“管理员”角色
     */
    private void requireAdmin() {
        User current = authService.requireLoginUser();
        if (current == null) {
            throw new RuntimeException("未登录");
        }

        if (current.getRoleId() == null) {
            throw new RuntimeException("无权限：仅管理员可操作");
        }

        Role role = roleService.getById(current.getRoleId());
        if (role == null || !"管理员".equals(role.getName())) {
            throw new RuntimeException("无权限：仅管理员可操作");
        }
    }
}
