package com.examsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
  @TableId(type = IdType.AUTO)
  private Long id;

  private String username;
  private String passwordHash;
  private String realName;
  private String roleCode;
  private Integer status;
  private LocalDateTime lastLoginTime;
  private LocalDateTime createdTime;
  private Long createBy;
  private LocalDateTime updatedTime;
  private Long updateBy;
}

