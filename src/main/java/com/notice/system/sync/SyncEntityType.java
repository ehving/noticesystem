package com.notice.system.sync;

/**
 * 支持多库同步的实体类型枚举
 */
public enum SyncEntityType {

    /**
     * 用户表
     */
    USER,

    /**
     * 角色表
     */
    ROLE,

    /**
     * 部门表
     */
    DEPT,

    /**
     * 公告表
     */
    NOTICE,

    /**
     * 公告-部门关联表
     */
    NOTICE_TARGET_DEPT,

    /**
     * 公告阅读记录表
     */
    NOTICE_READ,

    /**
     * 多库同步日志
     */
    SYNC_LOG;
}




