package com.notice.system.security;

import com.notice.system.exception.UnauthenticatedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String token = request.getHeader(SecurityConstants.AUTH_HEADER);

        log.debug("[JWT] {} {}", method, uri);

        // 先判断是不是“公共接口”
        boolean isPublic = SecurityPaths.isPublic(uri, method);

        // 1. 没带 token 的情况
        if (token == null || token.isBlank()) {
            if (isPublic) {
                // 公共接口 + 无 token：匿名访问，放行
                return true;
            }
            // 非公共接口必须登录
            throw new UnauthenticatedException(SecurityConstants.MSG_UNAUTHENTICATED);
        }

        // 2. 带了 token 的情况：尝试解析
        try {
            String username = JwtUtil.parseToken(token);
            request.setAttribute(SecurityConstants.LOGIN_USER_ATTR, username);
            return true;
        } catch (Exception e) {
            log.info("[JWT] token 无效：{}，{} {}", e.getMessage(), method, uri);

            if (isPublic) {
                // 公共接口 + token 无效：当匿名访问处理
                return true;
            }

            // 非公共接口 + token 无效：不允许
            throw new UnauthenticatedException(SecurityConstants.MSG_UNAUTHENTICATED);
        }
    }
}
