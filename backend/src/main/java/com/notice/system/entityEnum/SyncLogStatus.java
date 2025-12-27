package com.notice.system.entityEnum;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum SyncLogStatus {
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    CONFLICT("CONFLICT"),
    ERROR("ERROR");

    @EnumValue
    private final String code;

    SyncLogStatus(String code) { this.code = code; }

    @Override
    public String toString() {
        return code;
    }
}

