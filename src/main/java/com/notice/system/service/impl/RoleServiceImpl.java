package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.service.RoleService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncEntityType;
import com.notice.system.sync.SyncMetadataRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 角色服务实现：
 *  - 默认源库：MYSQL
 *  - 写操作带多库同步
 *  - 删除前检查是否仍有用户使用该角色
 */
@Slf4j
@Service
public class RoleServiceImpl
        extends MultiDbSyncServiceImpl<Role>
        implements RoleService {

    public RoleServiceImpl(SyncService syncService,
                           SyncMetadataRegistry metadataRegistry) {
        super(syncService, metadataRegistry,
                SyncEntityType.ROLE, DatabaseType.MYSQL);
    }

    // ===================== 查询（支持选库） =====================

    @Override
    public Role findByName(String name) {
        return findByNameInDb(defaultDb(), name);
    }

    @Override
    public Role findByNameInDb(DatabaseType db, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        SyncMetadataRegistry.EntitySyncDefinition<Role> def =
                metadataRegistry.getDefinition(SyncEntityType.ROLE);
        if (def == null) {
            log.warn("[ROLE] 未在 SyncMetadataRegistry 中找到定义");
            return null;
        }

        BaseMapper<Role> mapper = def.getMapper(db);
        if (mapper == null) {
            log.warn("[ROLE] 未找到指定库的 Role Mapper，db={}", db);
            return null;
        }

        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getName, name);
        return mapper.selectOne(wrapper);
    }

    @Override
    public List<Role> listAll() {
        return listAllFromDb(defaultDb());
    }

    @Override
    public List<Role> listAllFromDb(DatabaseType db) {
        SyncMetadataRegistry.EntitySyncDefinition<Role> def =
                metadataRegistry.getDefinition(SyncEntityType.ROLE);
        if (def == null) {
            log.warn("[ROLE] 未在 SyncMetadataRegistry 中找到定义");
            return Collections.emptyList();
        }

        BaseMapper<Role> mapper = def.getMapper(db);
        if (mapper == null) {
            log.warn("[ROLE] 未找到指定库的 Role Mapper，db={}", db);
            return Collections.emptyList();
        }

        return mapper.selectList(null);
    }

    // ===================== 写操作增强：删除前检查用户引用 =====================

    @Override
    public boolean removeById(Serializable id) {
        // 默认源库：defaultDb()
        return removeByIdInDb(defaultDb(), id);
    }

    @Override
    public boolean removeByIdInDb(DatabaseType db, Serializable id) {
        if (id == null) {
            return false;
        }
        String roleId = id.toString();

        // 1. 检查该库中是否仍有用户引用该角色
        SyncMetadataRegistry.EntitySyncDefinition<User> userDef =
                metadataRegistry.getDefinition(SyncEntityType.USER);
        if (userDef == null) {
            throw new IllegalStateException("[ROLE] 未在 SyncMetadataRegistry 中找到 USER 定义");
        }

        BaseMapper<User> userMapper = userDef.getMapper(db);
        if (userMapper == null) {
            throw new IllegalStateException("[ROLE] 未找到指定库的 User Mapper，db=" + db);
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRoleId, roleId);
        Long count = userMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            // 这里你也可以换成自定义 BusinessException
            throw new IllegalStateException("角色仍被 " + count + " 个用户使用，无法删除，roleId=" + roleId + "，db=" + db);
        }

        // 2. 确认无用户引用后，走带同步的删除逻辑
        return super.removeByIdInDb(db, id);
    }
}




