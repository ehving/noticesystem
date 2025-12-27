package com.notice.system.vo.conflict.req;

import com.notice.system.entityEnum.DatabaseType;
import lombok.Data;

@Data
public class SyncConflictResolveVo {
    private DatabaseType sourceDb;
    private String note;
}

