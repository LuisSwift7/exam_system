package com.examsystem.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
  private String accessToken;
  private String tokenType;
  private long expiresIn;
  private UserView user;

  @Data
  @AllArgsConstructor
  public static class UserView {
    private Long id;
    private String username;
    private String realName;
    private String roleCode;
  }
}

