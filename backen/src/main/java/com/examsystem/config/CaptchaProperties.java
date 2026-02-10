package com.examsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.captcha")
public class CaptchaProperties {
  private int length;
  private long expireSeconds;
}

