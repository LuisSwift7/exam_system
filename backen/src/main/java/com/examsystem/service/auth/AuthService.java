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

    String token = jwtService.issueToken(user.getId(), user.getUsername(), user.getRoleCode());
    return new LoginResponse(
        token,
        "Bearer",
        jwtProperties.getExpireSeconds(),
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
}

