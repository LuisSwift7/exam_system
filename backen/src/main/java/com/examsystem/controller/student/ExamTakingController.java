package com.examsystem.controller.student;

import com.examsystem.common.ApiResponse;
import com.examsystem.controller.dto.ExamResultResponse;
import com.examsystem.entity.Exam;
import com.examsystem.entity.ExamRecord;
import com.examsystem.entity.Question;
import com.examsystem.security.UserPrincipal;
import com.examsystem.service.exam.ExamTakingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/exam-taking")
@RequiredArgsConstructor
public class ExamTakingController {
    private final ExamTakingService takingService;

    @GetMapping("/{examId}/detail")
    public ApiResponse<Exam> getDetail(@PathVariable Long examId) {
        return ApiResponse.ok(takingService.getExam(examId));
    }

    @PostMapping("/{examId}/start")
    public ApiResponse<ExamRecord> start(@PathVariable Long examId, @AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.ok(takingService.startExam(examId, user.getUserId()));
    }

    @GetMapping("/{examId}/questions")
    public ApiResponse<List<Question>> getQuestions(@PathVariable Long examId) {
        // In real world, answers should be hidden
        List<Question> qs = takingService.getExamQuestions(examId);
        qs.forEach(q -> {
            q.setAnswer(null); 
            q.setAnalysis(null);
        });
        return ApiResponse.ok(qs);
    }

    @GetMapping("/{recordId}/result")
    public ApiResponse<ExamResultResponse> getResult(@PathVariable Long recordId) {
        return ApiResponse.ok(takingService.getExamResult(recordId));
    }

    @PostMapping("/submit-answer")
    public ApiResponse<Void> submitAnswer(@RequestBody AnswerRequest req) {
        takingService.submitAnswer(req.getRecordId(), req.getQuestionId(), req.getAnswer(), req.getIsMarked());
        return ApiResponse.ok(null);
    }

    @PostMapping("/submit-exam")
    public ApiResponse<Void> submitExam(@RequestBody Map<String, Long> body) {
        takingService.submitExam(body.get("recordId"));
        return ApiResponse.ok(null);
    }

    @Data
    public static class AnswerRequest {
        private Long recordId;
        private Long questionId;
        private String answer;
        private Integer isMarked;
    }
}
