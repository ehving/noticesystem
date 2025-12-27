package com.notice.system.entityEnum;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ConflictStatus {
    OPEN("OPEN"),
    RESOLVED("RESOLVED"),
    IGNORED("IGNORED");

    @EnumValue
    private final String code;

    ConflictStatus(String code) { this.code = code; }
}
