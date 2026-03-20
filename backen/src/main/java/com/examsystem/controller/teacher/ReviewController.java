package com.examsystem.controller.teacher;

import com.examsystem.common.ApiResponse;
import com.examsystem.entity.Review;
import com.examsystem.service.ReviewService;
import com.examsystem.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/reviews")
public class ReviewController {

    @Resource
    private ReviewService reviewService;

    @PostMapping
    public ApiResponse<Void> createReview(@RequestBody Review review, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        review.setTeacherId(userPrincipal.getUserId());
        reviewService.createReview(review);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateReview(@PathVariable Long id, @RequestBody Review review) {
        review.setId(id);
        reviewService.updateReview(review);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}")
    public ApiResponse<Review> getReview(@PathVariable Long id) {
        Review review = reviewService.getReviewById(id);
        return ApiResponse.ok(review);
    }

    @GetMapping("/exam/{examId}")
    public ApiResponse<Review> getReviewByExamId(@PathVariable Long examId) {
        Review review = reviewService.getReviewByExamId(examId);
        return ApiResponse.ok(review);
    }

    @GetMapping
    public ApiResponse<List<Review>> getReviews(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<Review> reviews = reviewService.getReviewsByTeacherId(userPrincipal.getUserId());
        return ApiResponse.ok(reviews);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<Void> publishReview(@PathVariable Long id) {
        reviewService.publishReview(id);
        return ApiResponse.ok();
    }
}
