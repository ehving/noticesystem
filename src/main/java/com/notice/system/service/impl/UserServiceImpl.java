package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.Dept;
import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.service.UserService;
import com.notice.system.service.SyncService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.sync.DatabaseType;
import com.notice.system.sync.SyncEntityType;
import com.notice.system.sync.SyncMetadataRegistry;
import com.notice.system.vo.user.UserAdminPageVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 用户服务实现：
 *  - 默认源库：MYSQL
 *  - 写操作带多库同步
 *  - 保存 / 更新时校验：角色、部门在该源库中必须存在（如不合法则抛异常）
 *  - 提供按库查询用户名 / 校验是否存在
 */
@Slf4j
@Service
public class UserServiceImpl
        extends MultiDbSyncServiceImpl<User>
        implements UserService {

    public UserServiceImpl(SyncService syncService,
                           SyncMetadataRegistry metadataRegistry) {
        super(syncService, metadataRegistry,
                SyncEntityType.USER, DatabaseType.MYSQL);
    }

    // ===================== 业务查询（支持选库） =====================

    @Override
    public User findByUsername(String username) {
        return findByUsernameInDb(defaultDb(), username);
    }

    @Override
    public User findByUsernameInDb(DatabaseType db, String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        SyncMetadataRegistry.EntitySyncDefinition<User> def =
                metadataRegistry.getDefinition(SyncEntityType.USER);
        if (def == null) {
            log.warn("[USER] 未在 SyncMetadataRegistry 中找到定义");
            return null;
        }

        BaseMapper<User> mapper = def.getMapper(db);
        if (mapper == null) {
            log.warn("[USER] 未找到指定库的 User Mapper，db={}", db);
            return null;
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return mapper.selectOne(wrapper);
    }

    @Override
    public List<User> listByRoleId(String roleId) {
        return listByRoleIdInDb(defaultDb(), roleId);
    }

    @Override
    public List<User> listByRoleIdInDb(DatabaseType db, String roleId) {
        if (roleId == null || roleId.isBlank() || db == null) {
            return Collections.emptyList();
        }

        SyncMetadataRegistry.EntitySyncDefinition<User> def =
                 metadataRegistry.getDefinition(SyncEntityType.USER);
        if (def == null) {
            log.warn("[USER] 未在 SyncMetadataRegistry 中找到 USER 定义");
            return Collections.emptyList();
        }

        BaseMapper<User> mapper = def.getMapper(db);
        if (mapper == null) {
            log.warn("[USER] 未找到指定库的 User Mapper，db={}", db);
            return Collections.emptyList();
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRoleId, roleId);

        return mapper.selectList(wrapper);
    }

    @Override
    public boolean existsByUsername(String username) {
        return existsByUsernameInDb(defaultDb(), username);
    }

    @Override
    public boolean existsByUsernameInDb(DatabaseType db, String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        SyncMetadataRegistry.EntitySyncDefinition<User> def =
                metadataRegistry.getDefinition(SyncEntityType.USER);
        if (def == null) {
            log.warn("[USER] 未在 SyncMetadataRegistry 中找到定义");
            return false;
        }

        BaseMapper<User> mapper = def.getMapper(db);
        if (mapper == null) {
            log.warn("[USER] 未找到指定库的 User Mapper，db={}", db);
            return false;
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        Long count = mapper.selectCount(wrapper);
        return count != null && count > 0;
    }

    // ===================== 写操作增强：校验 role/dept 合法性 =====================

    @Override
    public boolean save(User entity) {
        // 默认源库：defaultDb()
        return saveInDb(defaultDb(), entity);
    }

    @Override
    public boolean saveInDb(DatabaseType db, User entity) {
        validateRelationsInDb(db, entity);
        return super.saveInDb(db, entity);
    }

    @Override
    public boolean updateById(User entity) {
        return updateByIdInDb(defaultDb(), entity);
    }

    @Override
    public boolean updateByIdInDb(DatabaseType db, User entity) {
        validateRelationsInDb(db, entity);
        return super.updateByIdInDb(db, entity);
    }

    @Override
    public boolean removeById(Serializable id) {
        // 删除用户目前不需要额外检查，直接走同步删除
        return super.removeById(id);
    }

    @Override
    public boolean removeByIdInDb(DatabaseType db, Serializable id) {
        return super.removeByIdInDb(db, id);
    }

    /* ========== 管理端分页查询 ========== */

    @Override
    public Page<User> pageAdminUsersInDb(DatabaseType db, UserAdminPageVo vo) {
        DatabaseType useDb = (db == null ? defaultDb() : db);

        BaseMapper<User> mapper = resolveMapper(useDb);
        if (mapper == null) {
            log.warn("[USER] pageAdminUsersInDb 未找到 mapper，db={}", useDb);
            return new Page<>();
        }

        long pageNo = (vo.getPageNo() == null || vo.getPageNo() < 1) ? 1 : vo.getPageNo();
        long pageSize = (vo.getPageSize() == null || vo.getPageSize() <= 0) ? 10 : vo.getPageSize();

        Page<User> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (vo.getKeyword() != null && !vo.getKeyword().isBlank()) {
            String kw = "%" + vo.getKeyword().trim() + "%";
            wrapper.and(w -> w.like(User::getUsername, kw)
                    .or()
                    .like(User::getNickname, kw));
        }
        if (vo.getRoleId() != null && !vo.getRoleId().isBlank()) {
            wrapper.eq(User::getRoleId, vo.getRoleId());
        }
        if (vo.getDeptId() != null && !vo.getDeptId().isBlank()) {
            wrapper.eq(User::getDeptId, vo.getDeptId());
        }
        if (vo.getStatus() != null) {
            wrapper.eq(User::getStatus, vo.getStatus());
        }

        wrapper.orderByDesc(User::getCreateTime);

        return mapper.selectPage(page, wrapper);
    }

    /* ========== 管理端重置密码 / 修改状态（通过同步框架） ========== */

    @Override
    public boolean resetPasswordInDb(DatabaseType db, String userId, String newPassword) {
        if (userId == null || userId.isBlank() || newPassword == null) {
            return false;
        }
        DatabaseType useDb = (db == null ? defaultDb() : db);

        // 读取指定库的用户
        User user = getById(useDb, userId);
        if (user == null) {
            return false;
        }

        user.setPassword(newPassword);   // 目前明文 + 同步，后续可在外层先做加密

        // 关键点：走 updateByIdInDb(useDb, user)，会调用 SyncService 提交同步
        return updateByIdInDb(useDb, user);
    }

    @Override
    public boolean updateStatusInDb(DatabaseType db, String userId, Integer status) {
        if (userId == null || userId.isBlank() || status == null) {
            return false;
        }
        DatabaseType useDb = (db == null ? defaultDb() : db);

        User user = getById(useDb, userId);
        if (user == null) {
            return false;
        }

        user.setStatus(status);

        // 同样使用 updateByIdInDb 触发多库同步
        return updateByIdInDb(useDb, user);
    }

    /**
     * 在指定库中校验用户的 roleId / deptId：
     *  - 如果设置了 roleId，则该库中必须存在对应 Role
     *  - 如果设置了 deptId，则该库中必须存在对应 Dept
     *  否则抛出 IllegalArgumentException
     */
    private void validateRelationsInDb(DatabaseType db, User user) {
        if (user == null) {
            return;
        }

        // 校验角色
        String roleId = user.getRoleId();
        if (roleId != null && !roleId.isBlank()) {
            SyncMetadataRegistry.EntitySyncDefinition<Role> roleDef =
                    metadataRegistry.getDefinition(SyncEntityType.ROLE);
            if (roleDef == null) {
                throw new IllegalStateException("[USER] 未在 SyncMetadataRegistry 中找到 ROLE 定义");
            }
            BaseMapper<Role> roleMapper = roleDef.getMapper(db);
            if (roleMapper == null) {
                throw new IllegalStateException("[USER] 未找到指定库的 Role Mapper，db=" + db);
            }
            Role role = roleMapper.selectById(roleId);
            if (role == null) {
                throw new IllegalArgumentException("角色不存在或已被删除，roleId=" + roleId + "，db=" + db);
            }
        }

        // 校验部门（可以允许为空）
        String deptId = user.getDeptId();
        if (deptId != null && !deptId.isBlank()) {
            SyncMetadataRegistry.EntitySyncDefinition<Dept> deptDef =
                    metadataRegistry.getDefinition(SyncEntityType.DEPT);
            if (deptDef == null) {
                throw new IllegalStateException("[USER] 未在 SyncMetadataRegistry 中找到 DEPT 定义");
            }
            BaseMapper<Dept> deptMapper = deptDef.getMapper(db);
            if (deptMapper == null) {
                throw new IllegalStateException("[USER] 未找到指定库的 Dept Mapper，db=" + db);
            }
            Dept dept = deptMapper.selectById(deptId);
            if (dept == null) {
                throw new IllegalArgumentException("部门不存在或已被删除，deptId=" + deptId + "，db=" + db);
            }
        }
    }
}



