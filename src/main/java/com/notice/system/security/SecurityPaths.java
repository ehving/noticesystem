package com.notice.system.security;

public final class SecurityPaths {

    private SecurityPaths() {}

    /**
     * 公开路径白名单（不需要登录）——给 WebConfig.excludePathPatterns 用
     *
     * 注意：这里我们只保留“完全不需要拦截”的接口：
     * - 登录 / 注册
     * 公告接口需要“能看到 token”，所以不再放到这里。
     */
    public static final String[] PUBLIC_PATTERNS = new String[] {
            "/api/user/login",
            "/api/user/register"
            // 不再配置 "/api/notices/**"
    };

    /**
     * 是否为公开访问（用于 interceptor 内做 method 精确控制）
     */
    public static boolean isPublic(String uri, String method) {
        if (uri == null) return false;
        // 1) 登录注册永远放行
        if ("/api/user/login".equals(uri) || "/api/user/register".equals(uri)) {
            return true;
        }

        // 2) 公告：只放行 GET
        if ("GET".equalsIgnoreCase(method) && uri.startsWith("/api/notices")) {
            return true;
        }

        return false;
    }
}

