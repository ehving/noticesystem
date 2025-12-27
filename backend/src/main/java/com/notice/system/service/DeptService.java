package com.notice.system.service;

import com.notice.system.entity.Dept;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.service.base.MultiDbSyncService;
import com.notice.system.vo.dept.DeptTreeVo;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 部门服务（Dept）
 *
 * <p>职责：</p>
 * <ul>
 *   <li>多库同步 CRUD（继承 MultiDbSyncService）</li>
 *   <li>部门列表/树结构构建</li>
 *   <li>父子层级查询、删除前校验等</li>
 * </ul>
 */
public interface DeptService extends MultiDbSyncService<Dept> {

    /** 指定库：按 id 批量查询部门。 */
    List<Dept> listByIdsFromDb(DatabaseType db, Collection<String> ids);

    /** 默认库：按名称查找部门。 */
    Dept findByName(String name);

    /** 指定库：按名称查找部门。 */
    Dept findByNameInDb(DatabaseType db, String name);

    /** 默认库：列出所有启用状态的部门。 */
    List<Dept> listEnabled();

    /** 指定库：列出所有启用状态的部门。 */
    List<Dept> listEnabledFromDb(DatabaseType db);

    /**
     * 指定库：用于“选择父部门”的下拉数据。
     * <p>会排除 childDeptId 自身及其所有子孙，避免形成环。</p>
     */
    List<Dept> listEnabledForParentSelect(DatabaseType db, String childDeptId);

    /** 默认库：列出 parentId 的直接子部门（parentId 为空表示根部门）。 */
    List<Dept> listByParentId(String parentId);

    /** 指定库：列出 parentId 的直接子部门（parentId 为空表示根部门）。 */
    List<Dept> listByParentIdFromDb(DatabaseType db, String parentId);

    /** 指定库：列出 parentId 自身及其所有子孙部门（包含自身）。 */
    Set<Dept> listAllChildByParentIdFromDb(DatabaseType db, String parentId);

    /** 指定库：列出 deptId 自身及其所有祖先 id（包含自身）。 */
    Set<String> listSelfAndAncestorsIdsFromDb(DatabaseType db, String deptId);

    /** 默认库：是否存在子部门。 */
    boolean hasChildren(String deptId);

    /** 指定库：是否存在子部门。 */
    boolean hasChildrenInDb(DatabaseType db, String deptId);

    /** 将扁平部门列表构建为树结构。 */
    List<DeptTreeVo> buildTree(List<Dept> depts);
}



