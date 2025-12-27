package com.notice.system.entityEnum;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum SyncAction {
    CREATE("CREATE"),
    UPDATE("UPDATE"),
    DELETE("DELETE");

    @EnumValue
    private final String code;

    SyncAction(String code) { this.code = code; }
}




