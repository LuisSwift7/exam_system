package com.examsystem.service;

import com.examsystem.entity.Review;
import com.examsystem.mapper.ReviewMapper;
import com.examsystem.service.exam.ExamService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Resource
    private ReviewMapper reviewMapper;

    @Resource
    private NotificationService notificationService;

    @Resource
    private ExamService examService;

    @Override
    public void createReview(Review review) {
        review.setStatus(0); // 初始状态为未发布
        reviewMapper.insert(review);
    }

    @Override
    public void updateReview(Review review) {
        reviewMapper.update(review);
    }

    @Override
    public Review getReviewById(Long id) {
        return reviewMapper.findById(id);
    }

    @Override
    public Review getReviewByExamId(Long examId) {
        return reviewMapper.findByExamId(examId);
    }

    @Override
    public List<Review> getReviewsByTeacherId(Long teacherId) {
        return reviewMapper.findByTeacherId(teacherId);
    }

    @Override
    public List<Review> getAllReviews() {
        return reviewMapper.findAll();
    }

    @Override
    public void publishReview(Long id) {
        Review review = reviewMapper.findById(id);
        if (review != null) {
            review.setStatus(1); // 设置为已发布
            reviewMapper.update(review);
            // 通知学生
            notifyStudents(review.getExamId(), review.getId());
        }
    }

    @Override
    public void notifyStudents(Long examId, Long reviewId) {
        // 获取参加该考试的学生ID列表
        List<Long> studentIds = examService.getExamStudentIds(examId);
        if (studentIds != null && !studentIds.isEmpty()) {
            // 发送通知
            notificationService.createBulkNotification(
                    studentIds,
                    "review_published",
                    "考试讲评已发布",
                    "您参加的考试已发布讲评，请及时查看。",
                    reviewId
            );
        }
    }
}