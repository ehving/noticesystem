package com.notice.system.entityEnum.aggBy;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum SyncConflictAggBy {
    STATUS("STATUS"),
    CONFLICT_TYPE("CONFLICT_TYPE"),
    ENTITY_TYPE("ENTITY_TYPE");

    @EnumValue
    private final String code;

    SyncConflictAggBy(String code) { this.code = code; }
}

