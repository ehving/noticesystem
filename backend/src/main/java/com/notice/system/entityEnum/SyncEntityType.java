package com.notice.system.entityEnum;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum SyncEntityType {
    USER("USER"),
    ROLE("ROLE"),
    DEPT("DEPT"),
    NOTICE("NOTICE"),
    NOTICE_TARGET_DEPT("NOTICE_TARGET_DEPT"),
    NOTICE_READ("NOTICE_READ"),
    SYNC_LOG("SYNC_LOG"),
    SYNC_CONFLICT("SYNC_CONFLICT"),
    SYNC_CONFLICT_ITEM("SYNC_CONFLICT_ITEM");

    @EnumValue
    private final String code;

    SyncEntityType(String code) { this.code = code; }
}




