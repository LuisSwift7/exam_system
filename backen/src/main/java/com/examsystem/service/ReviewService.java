package com.examsystem.service;

import com.examsystem.entity.Review;
import java.util.List;

public interface ReviewService {
    void createReview(Review review);
    void updateReview(Review review);
    Review getReviewById(Long id);
    Review getReviewByExamId(Long examId);
    List<Review> getReviewsByTeacherId(Long teacherId);
    List<Review> getAllReviews();
    void publishReview(Long id);
    void notifyStudents(Long examId, Long reviewId);
}