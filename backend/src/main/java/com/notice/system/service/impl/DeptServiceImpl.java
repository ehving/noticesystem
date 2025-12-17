package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.Dept;
import com.notice.system.service.DeptService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncEntityType;
import com.notice.system.sync.SyncMetadataRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 部门服务实现：
 *  - 默认源库：MYSQL
 *  - 写操作自动多库同步（由 MultiDbSyncServiceImpl 实现）
 *  - 读操作支持按库选择
 */
@Slf4j
@Service
public class DeptServiceImpl
        extends MultiDbSyncServiceImpl<Dept>
        implements DeptService {

    public DeptServiceImpl(SyncService syncService,
                           SyncMetadataRegistry metadataRegistry) {
        super(syncService, metadataRegistry,
                SyncEntityType.DEPT, DatabaseType.MYSQL);
    }

    /* ================== 按名称查询 ================== */

    @Override
    public Dept findByName(String name) {
        return findByNameInDb(defaultDb(), name);
    }

    @Override
    public Dept findByNameInDb(DatabaseType db, String name) {
        if (db == null || name == null || name.isBlank()) {
            return null;
        }

        BaseMapper<Dept> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[DEPT] findByNameInDb 未找到指定库的 Mapper，db={}", db);
            return null;
        }

        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dept::getName, name.trim());
        return mapper.selectOne(wrapper);
    }

    /* ================== 启用部门列表 ================== */

    @Override
    public List<Dept> listEnabled() {
        return listEnabledFromDb(defaultDb());
    }

    @Override
    public List<Dept> listEnabledFromDb(DatabaseType db) {
        if (db == null) {
            return Collections.emptyList();
        }

        BaseMapper<Dept> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[DEPT] listEnabledFromDb 未找到指定库的 Mapper，db={}", db);
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dept::getStatus, 1)
                .orderByAsc(Dept::getSortOrder, Dept::getCreateTime);

        return mapper.selectList(wrapper);
    }

    /* ================== 按父部门查询子部门 ================== */

    @Override
    public List<Dept> listByParentId(String parentId) {
        return listByParentIdFromDb(defaultDb(), parentId);
    }

    @Override
    public List<Dept> listByParentIdFromDb(DatabaseType db, String parentId) {
        if (db == null) {
            return Collections.emptyList();
        }

        BaseMapper<Dept> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[DEPT] listByParentIdFromDb 未找到指定库的 Mapper，db={}", db);
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        if (parentId == null || parentId.isBlank()) {
            // 根部门
            wrapper.isNull(Dept::getParentId);
        } else {
            wrapper.eq(Dept::getParentId, parentId);
        }
        wrapper.orderByAsc(Dept::getSortOrder, Dept::getCreateTime);

        return mapper.selectList(wrapper);
    }

    /* ================== 是否存在子部门 ================== */

    @Override
    public boolean hasChildren(String deptId) {
        return hasChildrenInDb(defaultDb(), deptId);
    }

    @Override
    public boolean hasChildrenInDb(DatabaseType db, String deptId) {
        if (db == null || deptId == null || deptId.isBlank()) {
            return false;
        }

        BaseMapper<Dept> mapper = resolveMapper(db);
        if (mapper == null) {
            log.warn("[DEPT] hasChildrenInDb 未找到指定库的 Mapper，db={}", db);
            return false;
        }

        LambdaQueryWrapper<Dept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dept::getParentId, deptId);

        Long count = mapper.selectCount(wrapper);
        return count != null && count > 0;
    }
}



