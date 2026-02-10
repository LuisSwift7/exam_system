package com.examsystem.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {
  @NotBlank(message = "账号不能为空")
  @Size(min = 4, max = 64, message = "账号长度需为4-64")
  private String username;

  @NotBlank(message = "姓名不能为空")
  @Size(max = 64, message = "姓名长度不能超过64")
  private String realName;

  @NotBlank(message = "密码不能为空")
  @Size(min = 6, max = 32, message = "密码长度需为6-32")
  private String password;
}

