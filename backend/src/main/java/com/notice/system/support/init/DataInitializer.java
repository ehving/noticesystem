package com.notice.system.support.init;

import com.notice.system.common.GlobalProperties;
import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.service.PasswordService;
import com.notice.system.service.RoleService;
import com.notice.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleService roleService;
    private final UserService userService;
    private final PasswordService passwordService;
    private final GlobalProperties globalProperties; // 可选：用来读 adminRoleName

    @Override
    public void run(String... args) {

        // 0) 约定角色名（避免硬编码）
        String adminRoleName = globalProperties.getSecurity().getAdminRoleName();
        if (adminRoleName == null || adminRoleName.isBlank()) {
            adminRoleName = "管理员";
        }

        // 1) 确保基础角色存在（幂等）
        Role adminRole = ensureRoleExists(adminRoleName);
        ensureRoleExists("普通用户");

        // 2) 确保默认管理员存在（幂等）
        ensureAdminUserExists(adminRole);
    }

    /**
     * 幂等创建角色：不存在则创建，存在则直接返回。
     * 注意：如果你给 role.name 加了唯一索引，这里并发下 save 可能抛异常 -> catch 后再查一次即可。
     */
    private Role ensureRoleExists(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return null;
        }

        Role exists = roleService.findByName(roleName);
        if (exists != null) {
            return exists;
        }

        Role role = new Role();
        role.setName(roleName);

        try {
            boolean ok = roleService.save(role); // ✅ 走服务层：默认库写入 + 自动同步
            if (!ok) {
                log.warn("[INIT] 创建角色失败：name={}", roleName);
            } else {
                log.info("[INIT] 已创建角色：name={}, id={}", roleName, role.getId());
            }
        } catch (Exception e) {
            // 并发/重复启动兜底：可能另一线程刚插入，重查一次
            log.warn("[INIT] 创建角色可能重复（将重查）：name={}, err={}", roleName, e.getMessage());
        }

        Role again = roleService.findByName(roleName);
        if (again == null) {
            log.warn("[INIT] 角色创建/查询失败：name={}", roleName);
        }
        return again;
    }

    /**
     * 幂等创建管理员：admin 不存在才创建。
     * 注意：如果 user.username 有唯一索引，并发下 save 可能抛异常 -> catch 后再查一次即可。
     */
    private void ensureAdminUserExists(Role adminRole) {
        User existingAdmin = userService.findByUsername("admin");
        if (existingAdmin != null) {
            log.info("[INIT] 已存在默认管理员用户：username=admin，跳过创建");
            return;
        }

        if (adminRole == null || adminRole.getId() == null) {
            log.warn("[INIT] 未找到管理员角色，无法创建默认管理员用户");
            return;
        }

        User admin = new User();
        admin.setUsername("admin");

        String defaultPwd = "Admin123456";
        if (passwordService.isWeak(defaultPwd)) {
            log.warn("[INIT] 默认管理员密码不符合规则，但仍将写入（建议修改 defaultPwd）");
        }
        admin.setPassword(passwordService.prepareForStore(defaultPwd));

        admin.setRoleId(adminRole.getId());
        admin.setNickname("系统管理员");
        admin.setEmail("1798864119@qq.com");
        admin.setStatus(1);

        // deptId/avatar/phone 等可为空，createTime/updateTime 交给 FieldFill
        admin.setDeptId(null);
        admin.setPhone(null);
        admin.setAvatar(null);

        try {
            boolean ok = userService.save(admin); // ✅ 走服务层：默认库写入 + 自动同步
            if (ok) {
                log.info("[INIT] 已创建默认管理员用户：username=admin, id={}", admin.getId());
                log.warn("[INIT] 默认管理员密码为初始化值，请尽快登录系统修改密码！");
            } else {
                log.warn("[INIT] 创建默认管理员用户失败：username=admin");
            }
        } catch (Exception e) {
            // 并发/重复启动兜底
            log.warn("[INIT] 创建 admin 可能重复（将重查）：err={}", e.getMessage());
            User again = userService.findByUsername("admin");
            if (again != null) {
                log.info("[INIT] admin 已存在（并发创建成功）：id={}", again.getId());
            } else {
                log.warn("[INIT] admin 创建失败且重查仍不存在");
            }
        }
    }
}


