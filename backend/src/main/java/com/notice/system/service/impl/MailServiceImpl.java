package com.notice.system.service.impl;

import com.notice.system.common.GlobalProperties;
import com.notice.system.entity.Role;
import com.notice.system.entity.SyncLog;
import com.notice.system.entity.User;
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
    public void sendSyncConflictAlert(SyncLog record) {
        if (record == null) {
            return;
        }

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
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, true, StandardCharsets.UTF_8.name());

            if (mailCfg.getFrom() != null && !mailCfg.getFrom().isBlank()) {
                helper.setFrom(mailCfg.getFrom());
            }

            helper.setTo(adminEmails);

            String subjectPrefix = mailCfg.getSubjectPrefix();
            helper.setSubject(subjectPrefix + " " + record.getEntityType() + " #" + record.getEntityId());

            StringBuilder sb = new StringBuilder();
            sb.append("多库同步出现冲突/失败，请及时处理。\n\n")
                    .append("实体类型：").append(record.getEntityType()).append("\n")
                    .append("实体ID：").append(record.getEntityId()).append("\n")
                    .append("动作：").append(record.getAction()).append("\n")
                    .append("源库：").append(record.getSourceDb()).append("\n")
                    .append("目标库：").append(record.getTargetDb()).append("\n")
                    .append("状态：").append(record.getStatus()).append("\n")
                    .append("错误信息：").append(record.getErrorMsg() == null ? "无" : record.getErrorMsg()).append("\n")
                    .append("创建时间：").append(record.getCreateTime()).append("\n")
                    .append("更新时间：").append(record.getUpdateTime()).append("\n");

            helper.setText(sb.toString(), false);

            mailSender.send(message);

        } catch (Exception e) {
            log.warn("[MAIL] 发送同步冲突告警邮件失败，logId={}，错误={}",
                    record.getId(), e.getMessage(), e);
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

