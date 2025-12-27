package com.notice.system.sync;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.*;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.mapper.mysql.*;
import com.notice.system.mapper.pg.*;
import com.notice.system.mapper.sqlserver.*;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/** 维护每个实体在三库的 Mapper 映射与主键提取方式。 */
@Component
@RequiredArgsConstructor
public class SyncMetadataRegistry {

    private final RoleMysqlMapper roleMysqlMapper;
    private final RolePgMapper rolePgMapper;
    private final RoleSqlserverMapper roleSqlserverMapper;

    private final UserMysqlMapper userMysqlMapper;
    private final UserPgMapper userPgMapper;
    private final UserSqlserverMapper userSqlserverMapper;

    private final DeptMysqlMapper deptMysqlMapper;
    private final DeptPgMapper deptPgMapper;
    private final DeptSqlserverMapper deptSqlserverMapper;

    private final NoticeMysqlMapper noticeMysqlMapper;
    private final NoticePgMapper noticePgMapper;
    private final NoticeSqlserverMapper noticeSqlserverMapper;

    private final NoticeTargetDeptMysqlMapper noticeTargetDeptMysqlMapper;
    private final NoticeTargetDeptPgMapper noticeTargetDeptPgMapper;
    private final NoticeTargetDeptSqlserverMapper noticeTargetDeptSqlserverMapper;

    private final NoticeReadMysqlMapper noticeReadMysqlMapper;
    private final NoticeReadPgMapper noticeReadPgMapper;
    private final NoticeReadSqlserverMapper noticeReadSqlserverMapper;

    private final SyncLogMysqlMapper syncLogMysqlMapper;
    private final SyncLogPgMapper syncLogPgMapper;
    private final SyncLogSqlserverMapper syncLogSqlserverMapper;

    private final SyncConflictMysqlMapper syncConflictMysqlMapper;
    private final SyncConflictPgMapper syncConflictPgMapper;
    private final SyncConflictSqlserverMapper syncConflictSqlserverMapper;

    private final SyncConflictItemMysqlMapper syncConflictItemMysqlMapper;
    private final SyncConflictItemPgMapper syncConflictItemPgMapper;
    private final SyncConflictItemSqlserverMapper syncConflictItemSqlserverMapper;

    private final Map<SyncEntityType, EntitySyncDefinition<?>> registry = new EnumMap<>(SyncEntityType.class);

    @PostConstruct
    public void init() {
        register(SyncEntityType.NOTICE, "NOTICE", Notice::new, Notice::getId,
                noticeMysqlMapper, noticePgMapper, noticeSqlserverMapper);

        register(SyncEntityType.USER, "USER", User::new, User::getId,
                userMysqlMapper, userPgMapper, userSqlserverMapper);

        register(SyncEntityType.ROLE, "ROLE", Role::new, Role::getId,
                roleMysqlMapper, rolePgMapper, roleSqlserverMapper);

        register(SyncEntityType.DEPT, "DEPT", Dept::new, Dept::getId,
                deptMysqlMapper, deptPgMapper, deptSqlserverMapper);

        register(SyncEntityType.NOTICE_TARGET_DEPT, "NOTICE_TARGET_DEPT", NoticeTargetDept::new, NoticeTargetDept::getId,
                noticeTargetDeptMysqlMapper, noticeTargetDeptPgMapper, noticeTargetDeptSqlserverMapper);

        register(SyncEntityType.NOTICE_READ, "NOTICE_READ", NoticeRead::new, NoticeRead::getId,
                noticeReadMysqlMapper, noticeReadPgMapper, noticeReadSqlserverMapper);

        register(SyncEntityType.SYNC_LOG, "SYNC_LOG", SyncLog::new, SyncLog::getId,
                syncLogMysqlMapper, syncLogPgMapper, syncLogSqlserverMapper);

        register(SyncEntityType.SYNC_CONFLICT, "SYNC_CONFLICT", SyncConflict::new, SyncConflict::getId,
                syncConflictMysqlMapper, syncConflictPgMapper, syncConflictSqlserverMapper);

        register(SyncEntityType.SYNC_CONFLICT_ITEM, "SYNC_CONFLICT_ITEM", SyncConflictItem::new, SyncConflictItem::getId,
                syncConflictItemMysqlMapper, syncConflictItemPgMapper, syncConflictItemSqlserverMapper);
    }

    /** 获取指定实体的同步定义（未注册则抛异常）。 */
    @SuppressWarnings("unchecked")
    public <T> EntitySyncDefinition<T> getDefinition(SyncEntityType entityType) {
        Objects.requireNonNull(entityType, "entityType must not be null");
        EntitySyncDefinition<?> def = registry.get(entityType);
        if (def == null) {
            throw new IllegalArgumentException("No EntitySyncDefinition registered for " + entityType);
        }
        return (EntitySyncDefinition<T>) def;
    }

    /** 判断是否注册了该实体的同步定义。 */
    public boolean supports(SyncEntityType entityType) {
        return registry.containsKey(entityType);
    }

    private <T> void register(
            SyncEntityType type,
            String entityName,
            Supplier<T> supplier,
            Function<T, String> idGetter,
            BaseMapper<T> mysqlMapper,
            BaseMapper<T> pgMapper,
            BaseMapper<T> sqlserverMapper
    ) {
        EntitySyncDefinition<T> def = new EntitySyncDefinition<>(entityName, supplier, idGetter);
        def.addMapper(DatabaseType.MYSQL, mysqlMapper);
        def.addMapper(DatabaseType.PG, pgMapper);
        def.addMapper(DatabaseType.SQLSERVER, sqlserverMapper);
        registry.put(type, def);
    }

    @Data
    public static class EntitySyncDefinition<T> {
        /** 业务实体名（用于日志/展示）。 */
        private final String entityName;
        /** 预留：用于某些场景动态构造实体。 */
        private final Supplier<T> entitySupplier;
        /** 提取实体主键字符串的函数。 */
        private final Function<T, String> idGetter;

        /** 三库对应的 Mapper。 */
        private final Map<DatabaseType, BaseMapper<T>> mapperByDb = new EnumMap<>(DatabaseType.class);

        public void addMapper(DatabaseType db, BaseMapper<T> mapper) {
            mapperByDb.put(db, mapper);
        }

        public BaseMapper<T> getMapper(DatabaseType db) {
            return mapperByDb.get(db);
        }
    }
}




