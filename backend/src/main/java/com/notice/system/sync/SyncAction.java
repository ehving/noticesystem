package com.notice.system.sync;

/**
 * 同步动作类型：新增 / 更新 / 删除
 */
public enum SyncAction {
    CREATE,
    UPDATE,
    DELETE;

    public String value() {
        return name();
    }
}



