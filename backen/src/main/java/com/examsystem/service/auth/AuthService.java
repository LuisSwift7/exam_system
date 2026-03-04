package com.examsystem.service.auth;

import com.examsystem.common.BizException;
import com.examsystem.config.JwtProperties;
import com.examsystem.controller.dto.LoginRequest;
import com.examsystem.controller.dto.LoginResponse;
import com.examsystem.controller.dto.RegisterRequest;
import com.examsystem.entity.SysUser;
import com.examsystem.service.user.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final SysUserService sysUserService;
  private final CaptchaService captchaService;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public CaptchaService.CaptchaResult captcha() {
    return captchaService.generate();
  }

  public LoginResponse login(LoginRequest req) {
    captchaService.verify(req.getCaptchaId(), req.getCaptchaCode());

    SysUser user = sysUserService.findByUsername(req.getUsername());
    if (user == null) {
      throw new BizException(2001, "用户名或密码错误");
    }
    if (user.getStatus() != null && user.getStatus() == 0) {
      throw new BizException(2003, "账号被禁用");
    }
    if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
      throw new BizException(2001, "用户名或密码错误");
    }

    sysUserService.updateLastLoginTime(user.getId());

    String accessToken = jwtService.issueAccessToken(user.getId(), user.getUsername(), user.getRoleCode());
    String refreshToken = jwtService.issueRefreshToken(user.getId(), user.getUsername(), user.getRoleCode());
    return new LoginResponse(
        accessToken,
        refreshToken,
        "Bearer",
        30 * 60, // 30 minutes in seconds
        15 * 24 * 60 * 60, // 15 days in seconds
        new LoginResponse.UserView(user.getId(), user.getUsername(), user.getRealName(), user.getRoleCode())
    );
  }

  public void registerStudent(RegisterRequest req) {
    String username = req.getUsername().trim();
    if (sysUserService.existsByUsername(username)) {
      throw new BizException(2004, "账号已存在");
    }
    String hash = passwordEncoder.encode(req.getPassword());
    sysUserService.createStudentUser(username, hash, req.getRealName().trim());
  }

  public LoginResponse refreshToken(String refreshToken) {
    try {
      Claims claims = jwtService.parse(refreshToken);
      String tokenType = (String) claims.get("tokenType");
      if (!"refresh".equals(tokenType)) {
        throw new BizException(3001, "无效的刷新令牌");
      }
      
      Long userId = Long.parseLong(claims.getSubject());
      String username = (String) claims.get("username");
      String roleCode = (String) claims.get("role");
      
      // Generate new tokens
      String newAccessToken = jwtService.issueAccessToken(userId, username, roleCode);
      String newRefreshToken = jwtService.issueRefreshToken(userId, username, roleCode);
      
      return new LoginResponse(
          newAccessToken,
          newRefreshToken,
          "Bearer",
          30 * 60, // 30 minutes in seconds
          15 * 24 * 60 * 60, // 15 days in seconds
          new LoginResponse.UserView(userId, username, sysUserService.findById(userId).getRealName(), roleCode)
      );
    } catch (Exception e) {
      throw new BizException(3001, "刷新令牌无效或已过期");
    }
  }
}

