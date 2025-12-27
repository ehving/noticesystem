package com.notice.system.vo.report;

import lombok.Data;

@Data
public class AggVo {
    private String key;     // entityType 或 conflictType
    private Long count;
}

