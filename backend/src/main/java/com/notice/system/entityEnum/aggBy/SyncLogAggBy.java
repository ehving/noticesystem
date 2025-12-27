package com.notice.system.entityEnum.aggBy;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum SyncLogAggBy {
    STATUS("STATUS"),
    ENTITY_TYPE("ENTITY_TYPE"),
    ACTION("ACTION"),
    SOURCE_DB("SOURCE_DB"),
    TARGET_DB("TARGET_DB");

    @EnumValue
    private final String code;

    SyncLogAggBy(String code) { this.code = code; }
}
