package com.examsystem.controller;

import com.examsystem.common.ApiResponse;
import com.examsystem.controller.dto.LoginRequest;
import com.examsystem.controller.dto.LoginResponse;
import com.examsystem.controller.dto.RegisterRequest;
import com.examsystem.service.auth.AuthService;
import com.examsystem.service.auth.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @GetMapping("/captcha")
  public ApiResponse<CaptchaService.CaptchaResult> captcha() {
    return ApiResponse.ok(authService.captcha());
  }

  @PostMapping("/login")
  public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
    return ApiResponse.ok(authService.login(req));
  }

  @PostMapping("/register")
  public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest req) {
    authService.registerStudent(req);
    return ApiResponse.ok(null);
  }

  @PostMapping("/refresh")
  public ApiResponse<LoginResponse> refresh(@RequestBody RefreshTokenRequest req) {
    return ApiResponse.ok(authService.refreshToken(req.getRefreshToken()));
  }

  @Data
  public static class RefreshTokenRequest {
    private String refreshToken;
  }
}

