package com.examsystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
  private JwtProperties jwt;
  private CaptchaProperties captcha;
  private String baseUrl;
  
  @Data
  public static class JwtProperties {
    private String secret;
    private long expireSeconds;
  }
  
  @Data
  public static class CaptchaProperties {
    private int length;
    private int expireSeconds;
  }
}