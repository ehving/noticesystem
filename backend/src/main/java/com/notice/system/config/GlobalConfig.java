package com.notice.system.config;

import com.notice.system.common.GlobalProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GlobalProperties.class)
public class GlobalConfig {
}


