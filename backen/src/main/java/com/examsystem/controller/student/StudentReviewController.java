package com.examsystem.controller.student;

import com.examsystem.common.ApiResponse;
import com.examsystem.entity.Review;
import com.examsystem.security.UserPrincipal;
import com.examsystem.service.ReviewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/student/reviews")
public class StudentReviewController {

    @Resource
    private ReviewService reviewService;

    @GetMapping
    public ApiResponse<List<Review>> listPublishedReviews(@AuthenticationPrincipal UserPrincipal user) {
        return ApiResponse.ok(reviewService.getPublishedReviewsByStudentId(user.getUserId()));
    }

    @GetMapping("/exam/{examId}")
    public ApiResponse<Review> getReviewByExamId(@PathVariable Long examId,
                                                 @AuthenticationPrincipal UserPrincipal user) {
        Review review = reviewService.getPublishedReviewsByStudentId(user.getUserId()).stream()
                .filter(item -> examId.equals(item.getExamId()))
                .findFirst()
                .orElse(null);
        return ApiResponse.ok(review);
    }
}
