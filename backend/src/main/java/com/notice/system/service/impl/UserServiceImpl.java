package com.notice.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notice.system.entity.Dept;
import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.service.SyncService;
import com.notice.system.service.UserService;
import com.notice.system.service.base.MultiDbSyncServiceImpl;
import com.notice.system.sync.SyncMetadataRegistry;
import com.notice.system.vo.user.UserAdminPageVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

/**
 * 用户服务实现
 *
 * <p>约定：</p>
 * <ul>
 *   <li>defaultDb = MYSQL（User 默认工作库/源库）</li>
 *   <li>save/update 前校验：roleId/deptId 在“同库”必须存在，避免写入非法外键（逻辑外键）</li>
 *   <li>resetPassword/updateStatus 通过 updateByIdInDb 触发多库同步</li>
 * </ul>
 */
@Slf4j
@Service
public class UserServiceImpl extends MultiDbSyncServiceImpl<User> implements UserService {

    public UserServiceImpl(SyncService syncService, SyncMetadataRegistry metadataRegistry) {
        super(syncService, metadataRegistry, SyncEntityType.USER, DatabaseType.MYSQL);
    }

    /* ========================= 查询（支持选库） ========================= */

    @Override
    public User findByUsername(String username) {
        return findByUsernameInDb(defaultDb(), username);
    }

    @Override
    public User findByUsernameInDb(DatabaseType db, String username) {
        if (username == null || username.isBlank()) return null;

        DatabaseType useDb = useDb(db);
        BaseMapper<User> mapper = resolveMapper(useDb);

        return mapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username.trim()));
    }

    @Override
    public List<User> listByRoleId(String roleId) {
        return listByRoleIdInDb(defaultDb(), roleId);
    }

    @Override
    public List<User> listByRoleIdInDb(DatabaseType db, String roleId) {
        if (roleId == null || roleId.isBlank()) return List.of();

        DatabaseType useDb = useDb(db);
        BaseMapper<User> mapper = resolveMapper(useDb);

        return mapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRoleId, roleId.trim()));
    }

    @Override
    public boolean existsByUsername(String username) {
        return existsByUsernameInDb(defaultDb(), username);
    }

    @Override
    public boolean existsByUsernameInDb(DatabaseType db, String username) {
        if (username == null || username.isBlank()) return false;

        DatabaseType useDb = useDb(db);
        BaseMapper<User> mapper = resolveMapper(useDb);

        Long cnt = mapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username.trim()));

        return cnt != null && cnt > 0;
    }

    /* ========================= 写操作增强：校验 role/dept 合法性 ========================= */

    @Override
    public boolean save(User entity) {
        return saveInDb(defaultDb(), entity);
    }

    @Override
    public boolean saveInDb(DatabaseType db, User entity) {
        DatabaseType useDb = useDb(db);
        validateRelationsInDb(useDb, entity);
        return super.saveInDb(useDb, entity);
    }

    @Override
    public boolean updateById(User entity) {
        return updateByIdInDb(defaultDb(), entity);
    }

    @Override
    public boolean updateByIdInDb(DatabaseType db, User entity) {
        DatabaseType useDb = useDb(db);
        validateRelationsInDb(useDb, entity);
        return super.updateByIdInDb(useDb, entity);
    }

    @Override
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }

    @Override
    public boolean removeByIdInDb(DatabaseType db, Serializable id) {
        return super.removeByIdInDb(useDb(db), id);
    }

    /* ========================= 管理端分页 ========================= */

    @Override
    public Page<User> pageAdminUsersInDb(DatabaseType db, UserAdminPageVo vo) {
        DatabaseType useDb = useDb(db);
        UserAdminPageVo q = (vo == null ? new UserAdminPageVo() : vo);

        // ⚠️ 注意：pageNo/pageSize 通常是 Long（可能为 null），这里先兜底避免拆箱坑
        Long pageNoRaw = q.getPageNo();
        Long pageSizeRaw = q.getPageSize();
        long pageNo = (pageNoRaw == null || pageNoRaw < 1) ? 1L : pageNoRaw;
        long pageSize = (pageSizeRaw == null || pageSizeRaw <= 0) ? 10L : pageSizeRaw;

        Page<User> page = new Page<>(pageNo, pageSize);

        LambdaQueryWrapper<User> w = new LambdaQueryWrapper<>();
        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            String kw = "%" + q.getKeyword().trim() + "%";
            w.and(x -> x.like(User::getUsername, kw).or().like(User::getNickname, kw));
        }
        if (q.getRoleId() != null && !q.getRoleId().isBlank()) w.eq(User::getRoleId, q.getRoleId().trim());
        if (q.getDeptId() != null && !q.getDeptId().isBlank()) w.eq(User::getDeptId, q.getDeptId().trim());
        if (q.getStatus() != null) w.eq(User::getStatus, q.getStatus());

        w.orderByDesc(User::getCreateTime);

        return resolveMapper(useDb).selectPage(page, w);
    }

    /* ========================= 管理端操作：重置密码 / 修改状态 ========================= */

    @Override
    public boolean resetPasswordInDb(DatabaseType db, String userId, String newPassword) {
        if (userId == null || userId.isBlank() || newPassword == null) return false;

        DatabaseType useDb = useDb(db);
        User user = getById(useDb, userId);
        if (user == null) return false;

        // 目前明文；建议外层先做加密，再写入
        user.setPassword(newPassword);
        return updateByIdInDb(useDb, user);
    }

    @Override
    public boolean updateStatusInDb(DatabaseType db, String userId, Integer status) {
        if (userId == null || userId.isBlank() || status == null) return false;

        DatabaseType useDb = useDb(db);
        User user = getById(useDb, userId);
        if (user == null) return false;

        user.setStatus(status);
        return updateByIdInDb(useDb, user);
    }

    /**
     * 校验用户的“逻辑外键”（roleId / deptId）在同库存在。
     *
     * <p>为什么要校验：</p>
     * <ul>
     *   <li>你现在是多库同步，跨库外键无法强约束</li>
     *   <li>在源库先挡掉非法引用，后续同步链路更稳定</li>
     * </ul>
     */
    private void validateRelationsInDb(DatabaseType db, User user) {
        if (user == null) return;

        // 1) roleId：若非空，必须存在
        String roleId = user.getRoleId();
        if (roleId != null && !roleId.isBlank()) {
            BaseMapper<Role> roleMapper = resolveBaseMapperOf(SyncEntityType.ROLE, db);
            Role role = roleMapper.selectById(roleId.trim());
            if (role == null) {
                throw new IllegalArgumentException("角色不存在或已被删除：roleId=" + roleId + "，db=" + db);
            }
        }

        // 2) deptId：若非空，必须存在
        String deptId = user.getDeptId();
        if (deptId != null && !deptId.isBlank()) {
            BaseMapper<Dept> deptMapper = resolveBaseMapperOf(SyncEntityType.DEPT, db);
            Dept dept = deptMapper.selectById(deptId.trim());
            if (dept == null) {
                throw new IllegalArgumentException("部门不存在或已被删除：deptId=" + deptId + "，db=" + db);
            }
        }
    }
}




