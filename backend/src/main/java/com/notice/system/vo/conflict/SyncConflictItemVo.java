package com.notice.system.vo.conflict;

import com.notice.system.entityEnum.DatabaseType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SyncConflictItemVo {
    private String id;
    private DatabaseType dbType;
    private Integer existsFlag;      // 0/1
    private String rowHash;
    private String rowVersion;
    private LocalDateTime rowUpdateTime;
    private LocalDateTime lastCheckedAt;

    private String rowJson;
}



