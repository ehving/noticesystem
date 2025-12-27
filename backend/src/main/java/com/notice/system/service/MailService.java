package com.notice.system.service;

import com.notice.system.entity.SyncConflict;
import com.notice.system.entity.SyncConflictItem;
import com.notice.system.entity.SyncLog;

import java.util.List;

/**
 * 邮件服务：目前只关心同步冲突告警
 */
public interface MailService {

    /**发送同步冲突告警邮件*/
    void sendConflictAlert(SyncConflict conflict, List<SyncConflictItem> items);
}
