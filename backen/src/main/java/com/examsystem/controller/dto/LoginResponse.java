package com.examsystem.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
  private String accessToken;
  private String refreshToken;
  private String tokenType;
  private long expiresIn;
  private long refreshTokenExpiresIn;
  private LoginUserView user;
}

