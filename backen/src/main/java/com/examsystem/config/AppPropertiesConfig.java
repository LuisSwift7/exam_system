package com.examsystem.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CaptchaProperties.class, AppProperties.class})
public class AppPropertiesConfig {}

