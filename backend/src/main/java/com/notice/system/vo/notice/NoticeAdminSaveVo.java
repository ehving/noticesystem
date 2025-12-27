package com.notice.system.vo.notice;

import com.notice.system.entityEnum.DatabaseType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collection;

@Data
public class NoticeAdminSaveVo {

    private String id;              // 新增时可空，编辑时必传
    private String title;
    private String content;
    private String level;
    private String status;          // DRAFT / PUBLISHED / RECALLED（新增时一般用不到 RECALLED）
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private Collection<String> targetDeptIds;

    // 选库用（可选）
    private DatabaseType sourceDb;
}



