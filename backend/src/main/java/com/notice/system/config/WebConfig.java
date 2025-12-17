package com.notice.system.config;

import com.notice.system.security.JwtInterceptor;
import com.notice.system.security.SecurityPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                // 把公开路径从 interceptor 内部逻辑“前置到注册层”更清晰
                .excludePathPatterns(SecurityPaths.PUBLIC_PATTERNS);
    }
}




