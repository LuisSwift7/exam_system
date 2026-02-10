package com.examsystem.controller.student;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examsystem.common.ApiResponse;
import com.examsystem.entity.QuestionFeedback;
import com.examsystem.entity.QuestionReview;
import com.examsystem.security.UserPrincipal;
import com.examsystem.service.QuestionFeedbackService;
import com.examsystem.service.QuestionReviewService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/feedback")
public class StudentFeedbackController {

    @Autowired
    private QuestionFeedbackService feedbackService;
    
    @Autowired
    private QuestionReviewService reviewService;

    @PostMapping("/create")
    public ApiResponse<Void> createFeedback(@AuthenticationPrincipal UserPrincipal user,
                                            @RequestBody FeedbackRequest request) {
        feedbackService.createFeedback(user.getUserId(), request.getQuestionId(), request.getExamId(), request.getContent());
        return ApiResponse.ok(null);
    }

    @GetMapping("/list")
    public ApiResponse<Page<QuestionFeedback>> listMyFeedbacks(@AuthenticationPrincipal UserPrincipal user,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(feedbackService.getStudentFeedbacks(user.getUserId(), page, size));
    }
    
    @GetMapping("/review/{questionId}")
    public ApiResponse<QuestionReview> getReview(@PathVariable Long questionId) {
        return ApiResponse.ok(reviewService.getPublishedReviewByQuestionId(questionId));
    }

    @Data
    public static class FeedbackRequest {
        private Long questionId;
        private Long examId;
        private String content;
    }
}
