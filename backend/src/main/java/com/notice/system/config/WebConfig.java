package com.notice.system.config;

import com.notice.system.security.JwtInterceptor;
import com.notice.system.security.SecurityPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;

    // 兼容前端传参：2025-12-22 10:30 / 2025-12-22T10:30:00 / 2025-12-22
    private static final DateTimeFormatter DT_SPACE_MIN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DT_SPACE_SEC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    //拦截所有API
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(SecurityPaths.PUBLIC_PATTERNS);
    }

    //时间适配兼容
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // queryParam/pathVariable String -> LocalDateTime
        registry.addConverter(String.class, LocalDateTime.class, source -> {
            String s = source.trim();
            if (s.isEmpty()) return null;

            // ISO: 2025-12-22T10:30:00
            if (s.contains("T")) {
                return LocalDateTime.parse(s);
            }
            // 兼容：2025-12-22 10:30:00
            if (s.contains(":") && s.contains(" ")) {
                if (s.length() == 16) return LocalDateTime.parse(s, DT_SPACE_MIN);
                return LocalDateTime.parse(s, DT_SPACE_SEC);
            }
            // 兼容：2025-12-22 -> 当天 00:00:00
            LocalDate d = LocalDate.parse(s, DATE_ONLY);
            return d.atStartOfDay();
        });

        // 如果你 Controller / VO 里也直接用 LocalDate，这个也顺手加上
        registry.addConverter(String.class, LocalDate.class, source -> {
            String s = source.trim();
            if (s.isEmpty()) return null;
            // ISO 日期 or 2025-12-22 都能 parse
            return LocalDate.parse(s);
        });
    }
}





