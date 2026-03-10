package com.examsystem.controller.teacher;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.examsystem.common.ApiResponse;
import com.examsystem.entity.Exam;
import com.examsystem.entity.Image;
import com.examsystem.service.exam.ExamService;
import com.examsystem.dto.AutoGenerateRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/exams")
@RequiredArgsConstructor
public class TeacherExamController {
    private final ExamService examService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        IPage<Exam> result = examService.getExams(page, size, keyword);
        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("list", result.getRecords());
        return ApiResponse.ok(map);
    }

    @PostMapping
    public ApiResponse<Void> add(@RequestBody Exam exam) {
        examService.createExam(exam);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Exam exam) {
        exam.setId(id);
        examService.updateExam(exam);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        examService.deleteExam(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publish(@PathVariable Long id) {
        examService.publishExam(id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/question-ids")
    public ApiResponse<List<Long>> getQuestionIds(@PathVariable Long id) {
        return ApiResponse.ok(examService.getExamQuestionIds(id));
    }

    @GetMapping("/{id}/preview")
    public ApiResponse<ExamService.ExamPreviewVo> getPreview(@PathVariable Long id) {
        return ApiResponse.ok(examService.getExamPreview(id));
    }

    @PostMapping("/{id}/manual-compose")
    public ApiResponse<Void> manualCompose(@PathVariable Long id, @RequestBody ManualComposeRequest req) {
        examService.manualCompose(id, req.getQuestionIds(), req.getScore());
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/auto-compose")
    public ApiResponse<Void> autoCompose(@PathVariable Long id, @RequestBody ExamService.AutoComposeStrategy strategy) {
        examService.autoCompose(id, strategy);
        return ApiResponse.ok(null);
    }

    @PostMapping("/genetic-auto-compose")
    public ApiResponse<Void> geneticAutoCompose(@RequestBody AutoGenerateRequest request) {
        if (request.getExamId() == null) {
            throw new com.examsystem.common.BizException(400, "试卷ID不能为空");
        }
        examService.geneticAutoCompose(request.getExamId(), request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/classes")
    public ApiResponse<Void> setClasses(@PathVariable Long id, @RequestBody SetClassesRequest req) {
        examService.setExamClasses(id, req.getClassIds());
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/classes")
    public ApiResponse<List<Long>> getClasses(@PathVariable Long id) {
        return ApiResponse.ok(examService.getExamClassIds(id));
    }

    @GetMapping("/{id}/submissions")
    public ApiResponse<List<Map<String, Object>>> getSubmissions(@PathVariable Long id) {
        return ApiResponse.ok(examService.getExamSubmissions(id));
    }

    @PostMapping("/{id}/withdraw-submission/{recordId}")
    public ApiResponse<Void> withdrawSubmission(@PathVariable Long id, @PathVariable Long recordId) {
        examService.withdrawSubmission(recordId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/{id}/submissions/{recordId}/captures")
    public ApiResponse<List<Map<String, Object>>> getStudentCaptures(@PathVariable Long id, @PathVariable Long recordId) {
        return ApiResponse.ok(examService.getStudentCaptures(recordId));
    }

    @Data
    public static class ManualComposeRequest {
        private List<Long> questionIds;
        private Integer score;
    }

    @Data
    public static class SetClassesRequest {
        private List<Long> classIds;
    }
}
