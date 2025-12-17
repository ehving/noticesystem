package com.notice.system.security;

public final class SecurityConstants {

    private SecurityConstants() {}

    /** 请求头：Authorization */
    public static final String AUTH_HEADER = "Authorization";

    /** 可选：如果你希望前端传 "Bearer xxx" */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** request attribute：给 AuthService 用的当前登录用户名 */
    public static final String LOGIN_USER_ATTR = "loginUser";

    /** 未登录统一提示 */
    public static final String MSG_UNAUTHENTICATED = "未登录或登录已失效";
}

