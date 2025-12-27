package com.notice.system.service.impl;

import com.notice.system.common.GlobalProperties;
import com.notice.system.entity.*;
import com.notice.system.service.MailService;
import com.notice.system.service.RoleService;
import com.notice.system.service.UserService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final RoleService roleService;
    private final UserService userService;
    private final GlobalProperties globalProperties;

    @Override
    public void sendConflictAlert(SyncConflict conflict, List<SyncConflictItem> items) {
        if (conflict == null) return;

        GlobalProperties.Mail mailCfg = globalProperties.getMail();
        if (!mailCfg.isEnabled()) {
            log.debug("[MAIL] 邮件告警已关闭，跳过发送");
            return;
        }

        String[] adminEmails = resolveAdminEmails();
        if (adminEmails.length == 0) {
            log.warn("[MAIL] 没有可用的管理员邮箱，跳过发送冲突告警邮件");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            if (mailCfg.getFrom() != null && !mailCfg.getFrom().isBlank()) {
                helper.setFrom(mailCfg.getFrom());
            }
            helper.setTo(adminEmails);

            String subject = mailCfg.getSubjectPrefix()
                    + " 冲突工单 " + conflict.getEntityType() + " #" + conflict.getEntityId();
            helper.setSubject(subject);

            String url = null;
            if (mailCfg.getAdminUrlBase() != null && !mailCfg.getAdminUrlBase().isBlank()) {
                url = mailCfg.getAdminUrlBase().replaceAll("/$", "")
                        + "/admin/sync-conflicts?id=" + conflict.getId();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("检测到多库数据不一致，已生成冲突工单。\n\n")
                    .append("工单ID：").append(conflict.getId()).append("\n")
                    .append("实体类型：").append(conflict.getEntityType()).append("\n")
                    .append("实体ID：").append(conflict.getEntityId()).append("\n")
                    .append("冲突类型：").append(conflict.getConflictType()).append("\n")
                    .append("状态：").append(conflict.getStatus()).append("\n")
                    .append("首次发现：").append(conflict.getFirstSeenAt()).append("\n")
                    .append("最近发现：").append(conflict.getLastSeenAt()).append("\n")
                    .append("最近校验：").append(conflict.getLastCheckedAt()).append("\n");

            if (url != null) {
                sb.append("\n后台处理链接：").append(url).append("\n");
            }

            sb.append("\n各库快照：\n");
            if (items != null) {
                for (SyncConflictItem it : items) {
                    sb.append("- ").append(it.getDbType())
                            .append(" exists=").append(it.getExistsFlag())
                            .append(" hash=").append(it.getRowHash() == null ? "-" : it.getRowHash())
                            .append(" updateTime=").append(it.getRowUpdateTime() == null ? "-" : it.getRowUpdateTime())
                            .append("\n");
                }
            }

            helper.setText(sb.toString(), false);
            mailSender.send(message);

        } catch (Exception e) {
            log.warn("[MAIL] 发送冲突告警邮件失败，conflictId={}，错误={}",
                    conflict.getId(), e.getMessage(), e);
        }
    }


    private String[] resolveAdminEmails() {
        String adminRoleName = globalProperties.getSecurity().getAdminRoleName();
        Role adminRole = roleService.findByName(adminRoleName);
        if (adminRole == null) {
            log.warn("[MAIL] 未找到名称为 {} 的角色，无法发送冲突告警邮件", adminRoleName);
            return new String[0];
        }

        List<User> adminUsers = userService.listByRoleId(adminRole.getId());
        if (adminUsers == null || adminUsers.isEmpty()) {
            return new String[0];
        }

        return adminUsers.stream()
                .map(User::getEmail)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct().toArray(String[]::new);
    }
}

