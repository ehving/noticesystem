package com.notice.system.sync;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.Dept;
import com.notice.system.entity.Notice;
import com.notice.system.entity.NoticeRead;
import com.notice.system.entity.NoticeTargetDept;
import com.notice.system.entity.Role;
import com.notice.system.entity.SyncLog;
import com.notice.system.entity.User;
import com.notice.system.mapper.mysql.*;
import com.notice.system.mapper.pg.*;
import com.notice.system.mapper.sqlserver.*;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 多库同步元数据注册中心：
 *  - 为每一种 SyncEntityType 绑定三库 Mapper + 主键获取方法
 *  - 供 SyncExecutor / SyncService / MultiDbSyncServiceImpl 使用
 */
@Component
@RequiredArgsConstructor
public class SyncMetadataRegistry {

    // ======== 注入各实体在三库中的 Mapper ========

    // ROLE
    private final RoleMysqlMapper roleMysqlMapper;
    private final RolePgMapper rolePgMapper;
    private final RoleSqlserverMapper roleSqlserverMapper;

    // USER
    private final UserMysqlMapper userMysqlMapper;
    private final UserPgMapper userPgMapper;
    private final UserSqlserverMapper userSqlserverMapper;

    // DEPT
    private final DeptMysqlMapper deptMysqlMapper;
    private final DeptPgMapper deptPgMapper;
    private final DeptSqlserverMapper deptSqlserverMapper;

    // NOTICE
    private final NoticeMysqlMapper noticeMysqlMapper;
    private final NoticePgMapper noticePgMapper;
    private final NoticeSqlserverMapper noticeSqlserverMapper;

    // NOTICE_TARGET_DEPT
    private final NoticeTargetDeptMysqlMapper noticeTargetDeptMysqlMapper;
    private final NoticeTargetDeptPgMapper noticeTargetDeptPgMapper;
    private final NoticeTargetDeptSqlserverMapper noticeTargetDeptSqlserverMapper;

    // NOTICE_READ
    private final NoticeReadMysqlMapper noticeReadMysqlMapper;
    private final NoticeReadPgMapper noticeReadPgMapper;
    private final NoticeReadSqlserverMapper noticeReadSqlserverMapper;

    // SYNC_LOG
    private final SyncLogMysqlMapper syncLogMysqlMapper;
    private final SyncLogPgMapper syncLogPgMapper;
    private final SyncLogSqlserverMapper syncLogSqlserverMapper;

    // ======== 实体同步定义注册表 ========

    private final Map<SyncEntityType, EntitySyncDefinition<?>> registry =
            new EnumMap<>(SyncEntityType.class);

    @PostConstruct
    public void init() {

        // ===== NOTICE =====
        EntitySyncDefinition<Notice> noticeDef = new EntitySyncDefinition<>(
                "NOTICE",
                Notice::new,
                Notice::getId
        );
        noticeDef.addMapper(DatabaseType.MYSQL, noticeMysqlMapper);
        noticeDef.addMapper(DatabaseType.PG, noticePgMapper);
        noticeDef.addMapper(DatabaseType.SQLSERVER, noticeSqlserverMapper);
        registry.put(SyncEntityType.NOTICE, noticeDef);

        // ===== USER =====
        EntitySyncDefinition<User> userDef = new EntitySyncDefinition<>(
                "USER",
                User::new,
                User::getId
        );
        userDef.addMapper(DatabaseType.MYSQL, userMysqlMapper);
        userDef.addMapper(DatabaseType.PG, userPgMapper);
        userDef.addMapper(DatabaseType.SQLSERVER, userSqlserverMapper);
        registry.put(SyncEntityType.USER, userDef);

        // ===== ROLE =====
        EntitySyncDefinition<Role> roleDef = new EntitySyncDefinition<>(
                "ROLE",
                Role::new,
                Role::getId
        );
        roleDef.addMapper(DatabaseType.MYSQL, roleMysqlMapper);
        roleDef.addMapper(DatabaseType.PG, rolePgMapper);
        roleDef.addMapper(DatabaseType.SQLSERVER, roleSqlserverMapper);
        registry.put(SyncEntityType.ROLE, roleDef);

        // ===== DEPT =====
        EntitySyncDefinition<Dept> deptDef = new EntitySyncDefinition<>(
                "DEPT",
                Dept::new,
                Dept::getId
        );
        deptDef.addMapper(DatabaseType.MYSQL, deptMysqlMapper);
        deptDef.addMapper(DatabaseType.PG, deptPgMapper);
        deptDef.addMapper(DatabaseType.SQLSERVER, deptSqlserverMapper);
        registry.put(SyncEntityType.DEPT, deptDef);

        // ===== NOTICE_TARGET_DEPT =====
        EntitySyncDefinition<NoticeTargetDept> ntdDef = new EntitySyncDefinition<>(
                "NOTICE_TARGET_DEPT",
                NoticeTargetDept::new,
                NoticeTargetDept::getId
        );
        ntdDef.addMapper(DatabaseType.MYSQL, noticeTargetDeptMysqlMapper);
        ntdDef.addMapper(DatabaseType.PG, noticeTargetDeptPgMapper);
        ntdDef.addMapper(DatabaseType.SQLSERVER, noticeTargetDeptSqlserverMapper);
        registry.put(SyncEntityType.NOTICE_TARGET_DEPT, ntdDef);

        // ===== NOTICE_READ =====
        EntitySyncDefinition<NoticeRead> nrDef = new EntitySyncDefinition<>(
                "NOTICE_READ",
                NoticeRead::new,
                NoticeRead::getId
        );
        nrDef.addMapper(DatabaseType.MYSQL, noticeReadMysqlMapper);
        nrDef.addMapper(DatabaseType.PG, noticeReadPgMapper);
        nrDef.addMapper(DatabaseType.SQLSERVER, noticeReadSqlserverMapper);
        registry.put(SyncEntityType.NOTICE_READ, nrDef);

        // ===== SYNC_LOG =====
        EntitySyncDefinition<SyncLog> logDef = new EntitySyncDefinition<>(
                "SYNC_LOG",
                SyncLog::new,
                SyncLog::getId
        );
        logDef.addMapper(DatabaseType.MYSQL, syncLogMysqlMapper);
        logDef.addMapper(DatabaseType.PG, syncLogPgMapper);
        logDef.addMapper(DatabaseType.SQLSERVER, syncLogSqlserverMapper);
        registry.put(SyncEntityType.SYNC_LOG, logDef);
    }

    @SuppressWarnings("unchecked")
    public <T> EntitySyncDefinition<T> getDefinition(SyncEntityType entityType) {
        return (EntitySyncDefinition<T>) registry.get(entityType);
    }

    public boolean supports(SyncEntityType entityType) {
        return registry.containsKey(entityType);
    }

    @Data
    public static class EntitySyncDefinition<T> {
        /**
         * 实体名称（日志用）
         */
        private final String entityName;

        /**
         * 实例构造器（目前用得不多，保留以备扩展）
         */
        private final Supplier<T> entitySupplier;

        /**
         * ID 获取函数，例如 User::getId
         */
        private final Function<T, String> idGetter;

        /**
         * 三个库对应的 Mapper
         */
        private final Map<DatabaseType, BaseMapper<T>> mapperByDb =
                new EnumMap<>(DatabaseType.class);

        public void addMapper(DatabaseType db, BaseMapper<T> mapper) {
            mapperByDb.put(db, mapper);
        }

        public BaseMapper<T> getMapper(DatabaseType db) {
            return mapperByDb.get(db);
        }
    }
}



