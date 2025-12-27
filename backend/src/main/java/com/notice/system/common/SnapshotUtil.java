package com.notice.system.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.*;
import com.notice.system.entityEnum.ConflictType;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncAction;
import com.notice.system.entityEnum.SyncEntityType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Snapshot 工具：
 * <ul>
 *   <li>读取三库快照：exists(0/1) + hash(业务指纹)</li>
 *   <li>判断 mismatch / missing</li>
 *   <li>根据 action + snapshot 判定 ConflictType</li>
 * </ul>
 *
 * <p>注意：该类只做“纯读 + 计算”，不做任何落库操作。</p>
 */
public final class SnapshotUtil {

    private SnapshotUtil() {}

    /** 三库快照：exists(0/1) + hash(业务指纹) + mismatch/缺失判定 */
    public static final class Snapshot {
        public final Map<DatabaseType, Integer> exists = new EnumMap<>(DatabaseType.class);
        public final Map<DatabaseType, String> hash = new EnumMap<>(DatabaseType.class);

        public boolean anyMissing;
        public boolean allExist;
        public boolean allMissing;
        public boolean mismatch;
    }

    /**
     * 读取三库快照：对每个库 selectById，生成 exists/hash，并计算 anyMissing/allExist/allMissing/mismatch。
     *
     * @param entityType 实体类型
     * @param entityId   主键
     * @param mapperFn   给定 db -> mapper 的函数（外部注入，避免工具类依赖你的 registry 细节）
     */
    public static Snapshot readSnapshot(SyncEntityType entityType,
                                        String entityId,
                                        MapperProvider mapperFn) {
        Snapshot s = new Snapshot();

        if (entityType == null || entityId == null || entityId.isBlank()) {
            // 返回空快照，调用者按需处理
            return s;
        }

        for (DatabaseType db : DatabaseType.syncDbs()) {
            BaseMapper<?> mapper = mapperFn.getMapper(entityType, db);
            Object row = (mapper == null ? null : mapper.selectById(entityId));

            int flag = (row == null ? 0 : 1);
            s.exists.put(db, flag);
            s.hash.put(db, flag == 0 ? null : sha256Hex(buildFingerprint(entityType, row)));
        }

        s.anyMissing = s.exists.values().stream().anyMatch(v -> v == 0);
        s.allExist = s.exists.values().stream().allMatch(v -> v == 1);
        s.allMissing = s.exists.values().stream().allMatch(v -> v == 0);

        s.mismatch = false;
        if (s.allExist) {
            String base = null;
            for (DatabaseType db : DatabaseType.syncDbs()) {
                base = s.hash.get(db);
                if (base != null) break;
            }
            for (DatabaseType db : DatabaseType.syncDbs()) {
                if (!Objects.equals(base, s.hash.get(db))) {
                    s.mismatch = true;
                    break;
                }
            }
        }

        return s;
    }

    /**
     * 根据 action + snapshot 判断冲突类型。
     *
     * <p>你当前策略：</p>
     * <ul>
     *   <li>DELETE：期望三库都不存在；否则 MISSING</li>
     *   <li>CREATE/UPDATE：anyMissing -> MISSING；mismatch -> MISMATCH；否则 null</li>
     * </ul>
     */
    public static ConflictType judgeConflictType(SyncAction action, Snapshot s) {
        if (action == null || s == null) return null;

        if (action == SyncAction.DELETE) {
            return s.allMissing ? null : ConflictType.MISSING;
        }

        if (s.anyMissing) return ConflictType.MISSING;
        if (s.mismatch) return ConflictType.MISMATCH;
        return null;
    }

    /**
     * 指纹：只拼业务字段，排除 createTime/updateTime 这类“自然会变”的字段。
     */
    public static String buildFingerprint(SyncEntityType type, Object row) {
        if (row == null) return "";

        return switch (type) {
            case USER -> {
                User u = (User) row;
                yield "username=" + normText(u.getUsername())
                        + "|roleId=" + nvo(u.getRoleId())
                        + "|deptId=" + nvo(u.getDeptId())
                        + "|nickname=" + normText(u.getNickname())
                        + "|email=" + normText(u.getEmail())
                        + "|phone=" + normText(u.getPhone())
                        + "|status=" + nvo(u.getStatus());
            }
            case ROLE -> {
                Role r = (Role) row;
                yield "name=" + normText(r.getName());
            }
            case DEPT -> {
                Dept d = (Dept) row;
                yield "name=" + normText(d.getName())
                        + "|parentId=" + nvo(d.getParentId())
                        + "|description=" + normText(d.getDescription())
                        + "|sortOrder=" + nvo(d.getSortOrder())
                        + "|status=" + nvo(d.getStatus());
            }
            case NOTICE -> {
                Notice n = (Notice) row;
                yield "title=" + normText(n.getTitle())
                        + "|content=" + normText(n.getContent())
                        + "|publisherId=" + nvo(n.getPublisherId())
                        + "|level=" + normText(n.getLevel())
                        + "|status=" + normText(n.getStatus())
                        + "|publishTime=" + normTime(n.getPublishTime())
                        + "|expireTime=" + normTime(n.getExpireTime());
            }
            case NOTICE_TARGET_DEPT -> {
                NoticeTargetDept x = (NoticeTargetDept) row;
                yield "noticeId=" + nvo(x.getNoticeId())
                        + "|deptId=" + nvo(x.getDeptId());
            }
            case NOTICE_READ -> {
                NoticeRead nr = (NoticeRead) row;
                yield "noticeId=" + nvo(nr.getNoticeId())
                        + "|userId=" + nvo(nr.getUserId())
                        + "|readTime=" + normTime(nr.getReadTime())
                        + "|deviceType=" + normText(nr.getDeviceType());
            }
            default -> "id=" + normText(extractIdGeneric(row));
        };
    }

    private static String extractIdGeneric(Object row) {
        try {
            var m = row.getClass().getMethod("getId");
            Object v = m.invoke(row);
            return v == null ? "" : String.valueOf(v);
        } catch (Exception e) {
            return "";
        }
    }

    /** null -> ""；不会产生 "null" 字符串 */
    private static String nvo(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        return s == null ? "" : s.trim();
    }

    /** 文本标准化：trim + 统一换行，避免 \r\n / \r 造成假 mismatch */
    private static String normText(String s) {
        if (s == null) return "";
        String t = s.trim();
        return t.replace("\r\n", "\n").replace("\r", "\n");
    }

    /** 时间标准化：truncate 到秒 + 固定格式输出，避免不同库精度差异 */
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String normTime(LocalDateTime t) {
        if (t == null) return "";
        LocalDateTime tt = t.truncatedTo(ChronoUnit.SECONDS);
        return DT_FMT.format(tt);
    }

    /** hash：出错就抛，让上层记录 ERROR/日志 */
    private static String sha256Hex(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("sha256 failed", e);
        }
    }

    /**
     * 给 SnapshotUtil 注入 “如何取 mapper” 的方式，避免 common 包直接依赖你的 registry/service 基类。
     */
    @FunctionalInterface
    public interface MapperProvider {
        BaseMapper<?> getMapper(SyncEntityType type, DatabaseType db);
    }
}

