package com.examsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.examsystem.entity.Question;
import com.examsystem.entity.QuestionReview;
import com.examsystem.mapper.QuestionMapper;
import com.examsystem.mapper.QuestionReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class QuestionReviewService {

    @Autowired
    private QuestionReviewMapper reviewMapper;

    @Autowired
    private QuestionMapper questionMapper;

    // Teacher: Create/Update Review
    public void saveReview(QuestionReview review) {
        // Check if review exists for this question
        QuestionReview existing = reviewMapper.selectOne(new LambdaQueryWrapper<QuestionReview>()
                .eq(QuestionReview::getQuestionId, review.getQuestionId())
                .last("LIMIT 1"));

        if (existing != null) {
            review.setId(existing.getId());
            review.setCreateTime(existing.getCreateTime());
            review.setUpdateTime(LocalDateTime.now());
            reviewMapper.updateById(review);
        } else {
            review.setCreateTime(LocalDateTime.now());
            reviewMapper.insert(review);
        }
    }

    // Public/Student: Get Published Review for a Question
    public QuestionReview getPublishedReviewByQuestionId(Long questionId) {
        QuestionReview review = reviewMapper.selectOne(new LambdaQueryWrapper<QuestionReview>()
                .eq(QuestionReview::getQuestionId, questionId)
                .eq(QuestionReview::getPublishStatus, 1));
        attachQuestion(review);
        return review;
    }
    
    // Teacher: Get Review by QuestionId (Draft or Published)
    public QuestionReview getReviewByQuestionId(Long questionId) {
         // Assuming one review per question for simplicity, or get the latest
         QuestionReview review = reviewMapper.selectOne(new LambdaQueryWrapper<QuestionReview>()
                .eq(QuestionReview::getQuestionId, questionId)
                .last("LIMIT 1"));
         attachQuestion(review);
         return review;
    }

    private void attachQuestion(QuestionReview review) {
        if (review == null || review.getQuestionId() == null) {
            return;
        }

        Question question = questionMapper.selectById(review.getQuestionId());
        review.setQuestion(question);
    }
}
