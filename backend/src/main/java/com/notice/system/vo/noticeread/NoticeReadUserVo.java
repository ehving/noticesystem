package com.notice.system.vo.noticeread;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeReadUserVo {

    private String userId;
    private String username;
    private String nickname;

    private String deptId;
    private String deptName;

    private LocalDateTime readTime;
    private String deviceType;
}

