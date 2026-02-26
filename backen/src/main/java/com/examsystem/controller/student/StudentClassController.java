package com.examsystem.controller.student;

import com.examsystem.common.ApiResponse;
import com.examsystem.entity.Class;
import com.examsystem.entity.ClassStudent;
import com.examsystem.mapper.ClassStudentMapper;
import com.examsystem.security.UserPrincipal;
import com.examsystem.service.classroom.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/classes")
@RequiredArgsConstructor
public class StudentClassController {

    private final ClassService classService;
    private final ClassStudentMapper classStudentMapper;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<Class> classes = classService.getStudentClasses(user.getUserId());
        
        List<Map<String, Object>> result = classes.stream().map(clazz -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", clazz.getId());
            map.put("name", clazz.getName());
            map.put("code", clazz.getCode());
            map.put("teacherId", clazz.getTeacherId());
            map.put("status", clazz.getStatus());
            map.put("createdTime", clazz.getCreatedTime());
            map.put("updatedTime", clazz.getUpdatedTime());
            
            ClassStudent cs = classStudentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, clazz.getId())
                    .eq(ClassStudent::getStudentId, user.getUserId())
                    .eq(ClassStudent::getStatus, 1)
            );
            if (cs != null) {
                LocalDateTime joinTime = cs.getApproveTime() != null ? cs.getApproveTime() : cs.getCreatedTime();
                map.put("joinTime", joinTime != null ? joinTime.toString().replace("T", " ") : null);
            } else {
                map.put("joinTime", null);
            }
            
            return map;
        }).collect(Collectors.toList());
        
        return ApiResponse.ok(result);
    }

    @PostMapping("/apply")
    public ApiResponse<Void> apply(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        String code = request.get("code");
        classService.applyToClass(code, user.getUserId());
        return ApiResponse.ok();
    }

    @GetMapping("/check-code/{code}")
    public ApiResponse<Class> checkCode(
            @PathVariable String code
    ) {
        Class clazz = classService.getClassByCode(code);
        if (clazz != null) {
            return ApiResponse.ok(clazz);
        } else {
            return ApiResponse.fail(404, "班级不存在");
        }
    }
}
