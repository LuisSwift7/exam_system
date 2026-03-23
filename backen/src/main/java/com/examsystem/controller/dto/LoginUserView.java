package com.examsystem.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginUserView {
  private Long id;
  private String username;
  private String realName;
  private String roleCode;
}
