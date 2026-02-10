package com.examsystem.controller;

import com.examsystem.common.ApiResponse;
import com.examsystem.entity.SysUser;
import com.examsystem.security.UserPrincipal;
import com.examsystem.service.user.SysUserService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeController {
  private final SysUserService sysUserService;

  @GetMapping("/me")
  public ApiResponse<MeView> me(Authentication authentication) {
    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    SysUser user = sysUserService.findById(principal.getUserId());
    return ApiResponse.ok(new MeView(
        principal.getUserId(),
        principal.getUsername(),
        user == null ? null : user.getRealName(),
        principal.getRoleCode()
    ));
  }

  @Data
  @AllArgsConstructor
  public static class MeView {
    private Long id;
    private String username;
    private String realName;
    private String roleCode;
  }
}

