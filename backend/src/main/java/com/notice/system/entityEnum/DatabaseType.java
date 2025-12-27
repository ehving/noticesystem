package com.notice.system.entityEnum;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据源类型
 */
@Getter
public enum DatabaseType {

    MYSQL("MYSQL", true),
    PG("PG", true),
    SQLSERVER("SQLSERVER", true);

    @EnumValue
    private final String code;

    /**
     * 是否参与“业务同步 / 冲突快照”
     */
    private final boolean syncEnabled;

    DatabaseType(String code, boolean syncEnabled) {
        this.code = code;
        this.syncEnabled = syncEnabled;
    }

    /** 兼容你旧的 value() 习惯 */
    public String value() {
        return code;
    }

    public static DatabaseType of(String code) {
        if (code == null) return null;
        for (DatabaseType t : values()) {
            if (t.code.equalsIgnoreCase(code) || t.name().equalsIgnoreCase(code)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown DatabaseType: " + code);
    }

    public static List<DatabaseType> all() {
        return List.of(values());
    }

    /** 参与业务同步/冲突快照的数据库类型（推荐业务用这个） */
    public static List<DatabaseType> syncDbs() {
        return Arrays.stream(values())
                .filter(DatabaseType::isSyncEnabled)
                .toList();
    }

    public static Set<DatabaseType> syncDbSet() {
        return Arrays.stream(values())
                .filter(DatabaseType::isSyncEnabled)
                .collect(Collectors.toUnmodifiableSet());
    }
}




