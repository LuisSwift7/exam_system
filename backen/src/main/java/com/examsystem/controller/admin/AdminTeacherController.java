package com.examsystem.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.examsystem.common.ApiResponse;
import com.examsystem.entity.SysUser;
import com.examsystem.service.user.SysUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
public class AdminTeacherController {
    private final SysUserService sysUserService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        IPage<SysUser> result = sysUserService.getTeachers(page, size, keyword);
        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("list", result.getRecords());
        return ApiResponse.ok(map);
    }

    @PostMapping
    public ApiResponse<Void> add(@RequestBody TeacherRequest req) {
        sysUserService.addTeacher(req.getUsername(), req.getRealName(), req.getPassword());
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody TeacherRequest req) {
        sysUserService.updateTeacher(id, req.getRealName(), req.getPassword(), req.getStatus());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        sysUserService.deleteTeacher(id);
        return ApiResponse.ok(null);
    }

    @Data
    public static class TeacherRequest {
        private String username;
        private String realName;
        private String password;
        private Integer status;
    }
}
