package com.examsystem.controller.teacher;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.examsystem.common.ApiResponse;
import com.examsystem.entity.Class;
import com.examsystem.entity.ClassStudent;
import com.examsystem.security.UserPrincipal;
import com.examsystem.service.classroom.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher/classes")
@RequiredArgsConstructor
public class TeacherClassController {

    private final ClassService classService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        IPage<Class> result = classService.getTeacherClasses(page, size, user.getUserId());
        return ApiResponse.ok(Map.of(
                "total", result.getTotal(),
                "list", result.getRecords()
        ));
    }

    @PostMapping
    public ApiResponse<Class> create(
            @RequestBody Class clazz,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        clazz.setTeacherId(user.getUserId());
        clazz.setCreateBy(user.getUserId());
        clazz.setUpdateBy(user.getUserId());
        Class created = classService.createClass(clazz);
        return ApiResponse.ok(created);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @PathVariable Long id,
            @RequestBody Class clazz,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        clazz.setId(id);
        clazz.setUpdateBy(user.getUserId());
        classService.updateClass(clazz);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        classService.deleteClass(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}")
    public ApiResponse<Class> get(
            @PathVariable Long id
    ) {
        Class clazz = classService.getClassById(id);
        return ApiResponse.ok(clazz);
    }

    @GetMapping("/{id}/students")
    public ApiResponse<Map<String, Object>> getStudents(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        IPage<Map<String, Object>> result = classService.getClassStudents(page, size, id);
        return ApiResponse.ok(Map.of(
                "total", result.getTotal(),
                "list", result.getRecords()
        ));
    }

    @PostMapping("/{id}/students")
    public ApiResponse<Void> addStudent(
            @PathVariable Long id,
            @RequestBody Map<String, Long> request
    ) {
        Long studentId = request.get("studentId");
        classService.addStudent(id, studentId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}/students/{studentId}")
    public ApiResponse<Void> removeStudent(
            @PathVariable Long id,
            @PathVariable Long studentId
    ) {
        classService.removeStudent(id, studentId);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/applications")
    public ApiResponse<Map<String, Object>> getApplications(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        IPage<Map<String, Object>> result = classService.getClassApplications(page, size, id);
        return ApiResponse.ok(Map.of(
                "total", result.getTotal(),
                "list", result.getRecords()
        ));
    }

    @PutMapping("/applications/{id}/approve")
    public ApiResponse<Void> approveApplication(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request
    ) {
        Integer status = request.get("status");
        classService.approveApplication(id, status);
        return ApiResponse.ok();
    }
}
