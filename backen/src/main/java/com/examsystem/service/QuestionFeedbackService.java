package com.examsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examsystem.entity.Question;
import com.examsystem.entity.QuestionFeedback;
import com.examsystem.mapper.QuestionMapper;
import com.examsystem.mapper.QuestionFeedbackMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class QuestionFeedbackService {

    @Autowired
    private QuestionFeedbackMapper feedbackMapper;

    @Autowired
    private QuestionMapper questionMapper;

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
        Page<QuestionFeedback> feedbackPage = feedbackMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<QuestionFeedback>()
                        .eq(QuestionFeedback::getStudentId, studentId)
                        .orderByDesc(QuestionFeedback::getCreateTime));
        attachQuestions(feedbackPage.getRecords(), false);
        return feedbackPage;
    }

    // Teacher: List all feedbacks (with filters)
    public Page<QuestionFeedback> getFeedbacks(int page, int size, Integer status) {
        LambdaQueryWrapper<QuestionFeedback> query = new LambdaQueryWrapper<>();
        if (status != null) {
            query.eq(QuestionFeedback::getStatus, status);
        }
        query.orderByDesc(QuestionFeedback::getCreateTime);
        Page<QuestionFeedback> feedbackPage = feedbackMapper.selectPage(new Page<>(page, size), query);
        attachQuestions(feedbackPage.getRecords(), true);
        return feedbackPage;
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

    private void attachQuestions(List<QuestionFeedback> feedbacks, boolean includeAnswer) {
        if (feedbacks == null || feedbacks.isEmpty()) {
            return;
        }

        List<Long> questionIds = feedbacks.stream()
                .map(QuestionFeedback::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (questionIds.isEmpty()) {
            return;
        }

        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, question -> question));

        for (QuestionFeedback feedback : feedbacks) {
            Question question = questionMap.get(feedback.getQuestionId());
            if (question != null) {
                feedback.setQuestion(includeAnswer ? question : buildStudentQuestion(question));
            }
        }
    }

    private Question buildStudentQuestion(Question source) {
        Question question = new Question();
        question.setId(source.getId());
        question.setContent(source.getContent());
        question.setContentImageUrl(source.getContentImageUrl());
        question.setType(source.getType());
        question.setOptions(source.getOptions());
        question.setDifficulty(source.getDifficulty());
        question.setCategory(source.getCategory());
        question.setStemId(source.getStemId());
        return question;
    }
}
