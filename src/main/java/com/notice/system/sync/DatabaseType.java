package com.notice.system.sync;

/**
 * 数据源类型
 */
public enum DatabaseType {
    MYSQL,
    PG,
    SQLSERVER;

    public String value() {
        return name();
    }
}


