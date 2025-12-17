package com.notice.system.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.notice.system.mapper")
public class MybatisPlusConfig {
}

