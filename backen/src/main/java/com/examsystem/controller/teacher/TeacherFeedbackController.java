package com.examsystem.controller.teacher;

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
@RequestMapping("/api/teacher/feedback")
public class TeacherFeedbackController {

    @Autowired
    private QuestionFeedbackService feedbackService;
    
    @Autowired
    private QuestionReviewService reviewService;

    @GetMapping("/list")
    public ApiResponse<Page<QuestionFeedback>> listFeedbacks(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size,
                                                             @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(feedbackService.getFeedbacks(page, size, status));
    }

    @PostMapping("/reply")
    public ApiResponse<Void> replyFeedback(@RequestBody ReplyRequest request) {
        feedbackService.replyFeedback(request.getId(), request.getReplyContent());
        return ApiResponse.ok(null);
    }
    
    // Review Management
    @GetMapping("/review/{questionId}")
    public ApiResponse<QuestionReview> getReview(@PathVariable Long questionId) {
        return ApiResponse.ok(reviewService.getReviewByQuestionId(questionId));
    }
    
    @PostMapping("/review/save")
    public ApiResponse<Void> saveReview(@AuthenticationPrincipal UserPrincipal user,
                                        @RequestBody QuestionReview review) {
        review.setTeacherId(user.getUserId());
        reviewService.saveReview(review);
        return ApiResponse.ok(null);
    }

    @Data
    public static class ReplyRequest {
        private Long id;
        private String replyContent;
    }
}
