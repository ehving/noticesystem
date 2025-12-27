package com.notice.system.common;

import com.notice.system.entityEnum.DatabaseType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 全局配置项：对应 application.yml 里的 notice.* 节点
 */
@Data
@ConfigurationProperties(prefix = "notice")
public class GlobalProperties {

    private Mail mail = new Mail();
    private Sync sync = new Sync();
    private Security security = new Security();

    @Data
    public static class Mail {
        /** 是否开启邮件告警 */
        private boolean enabled = true;
        /** 发件人邮箱地址 */
        private String from;
        /** 邮件标题前缀 */
        private String subjectPrefix = "[同步冲突告警]";
        /** 前端地址*/
        private String adminUrlBase =  "http://localhost:5173";
    }

    @Data
    public static class Sync {

        private Retry retry = new Retry();
        private Full full = new Full();
        private Conflict conflict = new Conflict();

        @Data
        public static class Retry {
            /** 是否启用失败自动重试 */
            private boolean enabled = true;
            /** 最大自动重试次数 */
            private int maxRetryCount = 3;
            /** 重试任务执行间隔（毫秒） */
            private long fixedDelayMs = 60000;
        }

        @Data
        public static class Full {
            /** 是否启用定时全量同步 */
            private boolean enabled = true;
            /** 定时任务 cron 表达式 */
            private String cron = "0 0 3 * * ?";
            /** 全量同步的源库 */
            private DatabaseType sourceDb = DatabaseType.MYSQL;
        }

        @Data
        public static class Conflict {
            /** 是否启用冲突定时任务 */
            private boolean enabled = true;

            /** 定时重检 OPEN 冲突的间隔（毫秒） */
            private long recheckFixedDelayMs = 300000;

            /** 单轮重检数量上限 */
            private int recheckLimit = 50;

            /** 定时发送待通知冲突的间隔（毫秒） */
            private long notifyFixedDelayMs = 300000;

            /** 单轮通知数量上限 */
            private int notifyLimit = 50;

            /** 邮件冷却时间（分钟）：你 service/SQL 用得到的话放这里 */
            private int notifyCooldownMinutes = 30;
        }
    }

    @Data
    public static class Security {
        /** 管理员角色名称 */
        private String adminRoleName = "管理员";
    }
}


