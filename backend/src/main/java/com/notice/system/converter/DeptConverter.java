package com.notice.system.converter;

import com.notice.system.entity.Dept;
import com.notice.system.vo.dept.DeptOptionVo;
import com.notice.system.vo.dept.DeptTreeVo;
import com.notice.system.vo.dept.DeptVo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeptConverter {

    private DeptConverter() {}

    /* ============ Entity -> VO ============ */

    public static DeptOptionVo toOptionVo(Dept dept) {
        if (dept == null) return null;
        DeptOptionVo vo = new DeptOptionVo();
        vo.setId(dept.getId());
        vo.setName(dept.getName());
        vo.setParentId(dept.getParentId());
        return vo;
    }

    public static DeptTreeVo toTreeVo(Dept dept) {
        if (dept == null) return null;
        DeptTreeVo vo = new DeptTreeVo();
        vo.setId(dept.getId());
        vo.setName(dept.getName());
        vo.setParentId(dept.getParentId());
        vo.setSortOrder(dept.getSortOrder());
        vo.setStatus(dept.getStatus());
        // children 在 VO 中初始化为 new ArrayList<>()
        return vo;
    }

    public static DeptVo toDeptVo(Dept dept, String parentName) {
        if (dept == null) return null;
        DeptVo vo = new DeptVo();
        vo.setId(dept.getId());
        vo.setName(dept.getName());
        vo.setParentId(dept.getParentId());
        vo.setParentName(parentName);
        vo.setDescription(dept.getDescription());
        vo.setSortOrder(dept.getSortOrder());
        vo.setStatus(dept.getStatus());
        vo.setCreateTime(dept.getCreateTime());
        vo.setUpdateTime(dept.getUpdateTime());
        return vo;
    }

    public static List<DeptOptionVo> toOptionList(List<Dept> list) {
        if (list == null || list.isEmpty()) return List.of();
        List<DeptOptionVo> result = new ArrayList<>(list.size());
        for (Dept dept : list) {
            DeptOptionVo vo = toOptionVo(dept);
            if (vo != null) {
                result.add(vo);
            }
        }
        return result;
    }

    public static List<DeptVo> toDeptVoList(List<Dept> list, java.util.Map<String, String> parentNameMap) {
        if (list == null || list.isEmpty()) return List.of();
        List<DeptVo> result = new ArrayList<>(list.size());
        for (Dept dept : list) {
            String parentName = null;
            if (dept.getParentId() != null && parentNameMap != null) {
                parentName = parentNameMap.get(dept.getParentId());
            }
            result.add(toDeptVo(dept, parentName));
        }
        return result;
    }

    /* ============ VO -> Entity ============ */

    /**
     * 创建时，从 DeptVo 构建 Dept 实体（不设置 id）
     */
    public static Dept toEntityForCreate(DeptVo vo) {
        if (vo == null) return null;
        Dept dept = new Dept();
        dept.setName(vo.getName());
        dept.setParentId(trimToNull(vo.getParentId()));
        dept.setDescription(vo.getDescription());
        dept.setSortOrder(vo.getSortOrder());
        dept.setStatus(Objects.requireNonNullElse(vo.getStatus(), 1));
        // createTime / updateTime 可在 Controller 中统一设置
        return dept;
    }

    /**
     * 更新时，把 DeptVo 的字段覆盖到已有实体（保持 id / createTime 不变）
     */
    public static void copyForUpdate(DeptVo vo, Dept target) {
        if (vo == null || target == null) return;
        target.setName(vo.getName());
        target.setParentId(trimToNull(vo.getParentId()));
        target.setDescription(vo.getDescription());
        target.setSortOrder(vo.getSortOrder());
        if (vo.getStatus() != null) {
            target.setStatus(vo.getStatus());
        }
        // updateTime 在 Controller 里统一更新
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}

