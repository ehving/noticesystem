package com.notice.system.support.task;

import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoticePublishTask {

    private final NoticeService noticeService;

    @Scheduled(fixedDelay = 30_000)
    public void publishDueNotices() {
        DatabaseType db = noticeService.defaultDb(); // 建议只扫主库
        noticeService.publishDueDraftsInDb(db);
    }
}
