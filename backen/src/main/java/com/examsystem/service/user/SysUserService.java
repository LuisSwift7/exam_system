package com.examsystem.service.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examsystem.common.BizException;
import com.examsystem.entity.SysUser;
import com.examsystem.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SysUserService {
  private final SysUserMapper sysUserMapper;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public SysUser findByUsername(String username) {
    return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
        .eq(SysUser::getUsername, username)
        .last("limit 1"));
  }

  public SysUser findById(Long id) {
    return sysUserMapper.selectById(id);
  }

  public void updateLastLoginTime(Long userId) {
    SysUser user = new SysUser();
    user.setId(userId);
    user.setLastLoginTime(LocalDateTime.now());
    sysUserMapper.updateById(user);
  }

  public boolean existsByUsername(String username) {
    Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    return count != null && count > 0;
  }

  public void createStudentUser(String username, String passwordHash, String realName) {
    SysUser user = new SysUser();
    user.setUsername(username);
    user.setPasswordHash(passwordHash);
    user.setRealName(realName);
    user.setRoleCode("STUDENT");
    user.setStatus(1);
    sysUserMapper.insert(user);
  }

  // Teacher Management Methods

  public IPage<SysUser> getStudents(int page, int size, String keyword) {
    Page<SysUser> p = new Page<>(page, size);
    LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getRoleCode, "STUDENT")
            .orderByDesc(SysUser::getCreatedTime);
    
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
    }
    return sysUserMapper.selectPage(p, wrapper);
  }

  public void addStudent(String username, String realName, String password) {
    if (existsByUsername(username)) {
      throw new BizException(2004, "账号已存在");
    }
    createStudentUser(username, passwordEncoder.encode(password), realName);
  }

  public void updateStudent(Long id, String realName, String password, Integer status) {
    SysUser user = sysUserMapper.selectById(id);
    if (user == null || !"STUDENT".equals(user.getRoleCode())) {
      throw new BizException(2005, "用户不存在或非学生");
    }

    SysUser update = new SysUser();
    update.setId(id);
    if (StringUtils.hasText(realName)) update.setRealName(realName);
    if (StringUtils.hasText(password)) update.setPasswordHash(passwordEncoder.encode(password));
    if (status != null) update.setStatus(status);
    
    sysUserMapper.updateById(update);
  }

  public void deleteStudent(Long id) {
    SysUser user = sysUserMapper.selectById(id);
    if (user == null || !"STUDENT".equals(user.getRoleCode())) {
      throw new BizException(2005, "用户不存在或非学生");
    }
    sysUserMapper.deleteById(id);
  }

  // Admin Management Methods (Teachers)

  public IPage<SysUser> getTeachers(int page, int size, String keyword) {
    Page<SysUser> p = new Page<>(page, size);
    LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getRoleCode, "TEACHER")
            .orderByDesc(SysUser::getCreatedTime);
    
    if (StringUtils.hasText(keyword)) {
      wrapper.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword));
    }
    return sysUserMapper.selectPage(p, wrapper);
  }

  public void addTeacher(String username, String realName, String password) {
    if (existsByUsername(username)) {
      throw new BizException(2004, "账号已存在");
    }
    SysUser user = new SysUser();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setRealName(realName);
    user.setRoleCode("TEACHER");
    user.setStatus(1);
    sysUserMapper.insert(user);
  }

  public void updateTeacher(Long id, String realName, String password, Integer status) {
    SysUser user = sysUserMapper.selectById(id);
    if (user == null || !"TEACHER".equals(user.getRoleCode())) {
      throw new BizException(2005, "用户不存在或非教师");
    }

    SysUser update = new SysUser();
    update.setId(id);
    if (StringUtils.hasText(realName)) update.setRealName(realName);
    if (StringUtils.hasText(password)) update.setPasswordHash(passwordEncoder.encode(password));
    if (status != null) update.setStatus(status);
    
    sysUserMapper.updateById(update);
  }

  public void deleteTeacher(Long id) {
    SysUser user = sysUserMapper.selectById(id);
    if (user == null || !"TEACHER".equals(user.getRoleCode())) {
      throw new BizException(2005, "用户不存在或非教师");
    }
    sysUserMapper.deleteById(id);
  }
}
