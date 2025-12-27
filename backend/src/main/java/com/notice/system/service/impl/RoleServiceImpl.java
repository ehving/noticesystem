package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.service.RoleService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.sync.SyncMetadataRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

/**
 * 角色服务实现
 *
 * <p>约定：</p>
 * <ul>
 *   <li>defaultDb = MYSQL（作为 Role 的默认工作库/源库）</li>
 *   <li>写操作走父类 save/update/remove：成功后自动提交同步任务</li>
 *   <li>删除前做引用校验：同库下仍有 User.roleId 指向该 roleId 则禁止删除</li>
 * </ul>
 */
@Slf4j
@Service
public class RoleServiceImpl extends MultiDbSyncServiceImpl<Role> implements RoleService {

    public RoleServiceImpl(SyncService syncService, SyncMetadataRegistry metadataRegistry) {
        super(syncService, metadataRegistry, SyncEntityType.ROLE, DatabaseType.MYSQL);
    }

    /* ========================= 查询（支持选库） ========================= */

    @Override
    public Role findByName(String name) {
        return findByNameInDb(defaultDb(), name);
    }

    @Override
    public Role findByNameInDb(DatabaseType db, String name) {
        if (name == null || name.isBlank()) return null;

        DatabaseType useDb = useDb(db);
        BaseMapper<Role> mapper = resolveMapper(useDb);

        return mapper.selectOne(new LambdaQueryWrapper<Role>()
                .eq(Role::getName, name.trim()));
    }

    @Override
    public List<Role> listAll() {
        return listAllFromDb(defaultDb());
    }

    @Override
    public List<Role> listAllFromDb(DatabaseType db) {
        // 父类 listAll(db) 已经做了 null -> empty 的兜底
        return super.listAll(useDb(db));
    }

    /* ========================= 写操作增强：删除前校验引用 ========================= */

    @Override
    public boolean removeById(Serializable id) {
        return removeByIdInDb(defaultDb(), id);
    }

    @Override
    public boolean removeByIdInDb(DatabaseType db, Serializable id) {
        if (id == null) return false;

        DatabaseType useDb = useDb(db);
        String roleId = String.valueOf(id);

        // 1) 在同一个库里检查：是否存在用户引用该角色
        BaseMapper<User> userMapper = resolveBaseMapperOf(SyncEntityType.USER, useDb);

        Long cnt = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRoleId, roleId));

        if (cnt != null && cnt > 0) {
            // 这里你也可以换成 BusinessException（更适合前端展示）
            throw new IllegalStateException("角色仍被 " + cnt + " 个用户使用，无法删除：roleId=" + roleId + "，db=" + useDb);
        }

        // 2) 通过父类删除（删除成功后会触发同步）
        return super.removeByIdInDb(useDb, id);
    }
}





