package com.examsystem.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.examsystem.common.ApiResponse;
import com.examsystem.entity.SysLog;
import com.examsystem.entity.SysUser;
import com.examsystem.service.SysLogService;
import com.examsystem.mapper.SysUserMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysLogService sysLogService;

    // 用户管理
    @GetMapping("/users")
    public ApiResponse<IPage<SysUser>> getUsers(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        IPage<SysUser> userPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        return ApiResponse.ok(sysUserMapper.selectPage(userPage, null));
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        sysUserMapper.updateById(user);
        return ApiResponse.ok();
    }

    // 日志管理
    @GetMapping("/logs")
    public ApiResponse<IPage<SysLog>> getLogs(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size, @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(sysLogService.getLogs(page, size, keyword));
    }
}
