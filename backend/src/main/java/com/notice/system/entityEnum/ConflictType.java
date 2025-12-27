package com.notice.system.entityEnum;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ConflictType {
    MISSING("MISSING"),   // 某库缺行
    MISMATCH("MISMATCH"); // 行内容不一致

    @EnumValue
    private final String code;

    ConflictType(String code) { this.code = code; }
}

