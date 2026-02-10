package com.examsystem.controller.student;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.examsystem.common.ApiResponse;
import com.examsystem.entity.Exam;
import com.examsystem.service.exam.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/student/exams")
@RequiredArgsConstructor
public class StudentExamController {
    private final ExamService examService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        IPage<Exam> result = examService.getAvailableExams(page, size);
        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("list", result.getRecords());
        return ApiResponse.ok(map);
    }
}
