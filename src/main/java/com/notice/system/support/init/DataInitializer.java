package com.notice.system.support.init;

import com.notice.system.entity.Role;
import com.notice.system.entity.User;
import com.notice.system.service.PasswordService;
import com.notice.system.service.RoleService;
import com.notice.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统启动时初始化基础数据（角色 + 管理员用户）
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleService roleService;
    private final UserService userService;
    private final PasswordService passwordService;

    @Override
    public void run(String... args) {

        // 1. 先检查是否已经有角色数据
        List<Role> roles = roleService.listAll();
        Role adminRole;

        if (roles == null || roles.isEmpty()) {
            log.info("[INIT] 检测到是第一次启动，开始初始化角色和管理员用户");

            LocalDateTime now = LocalDateTime.now();

            // 1.1 创建基础角色：管理员
            adminRole = new Role();
            adminRole.setName("管理员");
            adminRole.setCreateTime(now);
            adminRole.setUpdateTime(now);
            roleService.save(adminRole);

            // 1.2 创建基础角色：普通用户
            Role userRole = new Role();
            userRole.setName("普通用户");
            userRole.setCreateTime(now);
            userRole.setUpdateTime(now);
            roleService.save(userRole);

            log.info("[INIT] 已创建基础角色：管理员({}) / 普通用户({})",
                    adminRole.getId(), userRole.getId());
        } else {
            log.info("[INIT] 角色表已有数据，跳过角色初始化");
            adminRole = roleService.findByName("管理员");
        }

        // 2. 初始化默认管理员账号 admin（仅当不存在时创建）
        User existingAdmin = userService.findByUsername("admin");
        if (existingAdmin != null) {
            log.info("[INIT] 已存在默认管理员用户：username=admin，跳过创建");
            return;
        }

        if (adminRole == null) {
            log.warn("[INIT] 未找到 '管理员' 角色，无法创建默认管理员用户");
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        User admin = new User();
        admin.setUsername("admin");

        // ====== 关键：统一走 PasswordService（当前明文）======
        String defaultPwd = "Admin123456"; // 你也可以换一个更强的
        if (passwordService.isWeak(defaultPwd)) {
            log.warn("[INIT] 默认管理员密码不符合规则，将仍然写入（你可以修改 defaultPwd 或放宽规则）");
        }
        admin.setPassword(passwordService.prepareForStore(defaultPwd));

        admin.setRoleId(adminRole.getId());

        admin.setNickname("系统管理员");
        admin.setEmail("admin@example.com");
        admin.setPhone(null);
        admin.setAvatar(null);
        admin.setStatus(1);

        admin.setDeptId(null);

        admin.setCreateTime(now);
        admin.setUpdateTime(now);

        userService.save(admin);

        log.info("[INIT] 已创建默认管理员用户：username=admin, id={}", admin.getId());
        log.warn("[INIT] 默认管理员密码为初始化值，请尽快登录系统修改密码！");
    }
}
