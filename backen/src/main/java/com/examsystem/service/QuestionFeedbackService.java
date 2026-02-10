package com.examsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examsystem.entity.QuestionFeedback;
import com.examsystem.mapper.QuestionFeedbackMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class QuestionFeedbackService {

    @Autowired
    private QuestionFeedbackMapper feedbackMapper;

    // Student: Create Feedback
    public void createFeedback(Long studentId, Long questionId, Long examId, String content) {
        QuestionFeedback feedback = new QuestionFeedback();
        feedback.setStudentId(studentId);
        feedback.setQuestionId(questionId);
        feedback.setExamId(examId);
        feedback.setContent(content);
        feedback.setStatus(0);
        feedback.setCreateTime(LocalDateTime.now());
        feedbackMapper.insert(feedback);
    }

    // Student: List my feedbacks
    public Page<QuestionFeedback> getStudentFeedbacks(Long studentId, int page, int size) {
        return feedbackMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<QuestionFeedback>()
                        .eq(QuestionFeedback::getStudentId, studentId)
                        .orderByDesc(QuestionFeedback::getCreateTime));
    }

    // Teacher: List all feedbacks (with filters)
    public Page<QuestionFeedback> getFeedbacks(int page, int size, Integer status) {
        LambdaQueryWrapper<QuestionFeedback> query = new LambdaQueryWrapper<>();
        if (status != null) {
            query.eq(QuestionFeedback::getStatus, status);
        }
        query.orderByDesc(QuestionFeedback::getCreateTime);
        return feedbackMapper.selectPage(new Page<>(page, size), query);
    }

    // Teacher: Reply
    public void replyFeedback(Long id, String replyContent) {
        QuestionFeedback feedback = feedbackMapper.selectById(id);
        if (feedback != null) {
            feedback.setReplyContent(replyContent);
            feedback.setReplyTime(LocalDateTime.now());
            feedback.setStatus(1);
            feedbackMapper.updateById(feedback);
        }
    }
}
