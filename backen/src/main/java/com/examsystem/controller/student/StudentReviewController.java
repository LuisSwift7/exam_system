package com.examsystem.controller.student;

import com.examsystem.common.ApiResponse;
import com.examsystem.entity.Review;
import com.examsystem.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/student/reviews")
public class StudentReviewController {

    @Resource
    private ReviewService reviewService;

    @GetMapping("/exam/{examId}")
    public ApiResponse<Review> getReviewByExamId(@PathVariable Long examId) {
        Review review = reviewService.getReviewByExamId(examId);
        // 只返回已发布的讲评
        if (review != null && review.getStatus() == 1) {
            return ApiResponse.ok(review);
        }
        return ApiResponse.ok(null);
    }
}
