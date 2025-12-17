package com.notice.system.service;

import com.notice.system.entity.SyncLog;

/**
 * 邮件服务：目前只关心同步冲突告警
 */
public interface MailService {

    /**
     * 发送同步冲突告警邮件到所有管理员邮箱
     */
    void sendSyncConflictAlert(SyncLog logRecord);
}
