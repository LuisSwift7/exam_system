package com.examsystem.service.exam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.examsystem.common.BizException;
import com.examsystem.controller.dto.ExamResultResponse;
import com.examsystem.entity.*;
import com.examsystem.mapper.*;
import com.examsystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamTakingService {
    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamQuestionRelationMapper relationMapper;
    private final QuestionMapper questionMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final WrongBookMapper wrongBookMapper;

    public Exam getExam(Long examId) {
        return examMapper.selectById(examId);
    }

    public ExamResultResponse getExamResult(Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) throw new BizException(4004, "记录不存在");
        if (record.getStatus() == 0) throw new BizException(4003, "考试未完成，无法查看结果");
        
        Exam exam = examMapper.selectById(record.getExamId());
        
        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId));
        Map<Long, ExamAnswer> ansMap = answers.stream().collect(Collectors.toMap(ExamAnswer::getQuestionId, Function.identity()));
        
        List<ExamQuestionRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<ExamQuestionRelation>()
                .eq(ExamQuestionRelation::getExamId, record.getExamId()));
        Map<Long, Integer> scoreMap = relations.stream()
                .collect(Collectors.toMap(ExamQuestionRelation::getQuestionId, ExamQuestionRelation::getScore));
        Map<Long, String> categoryMap = relations.stream()
                .collect(Collectors.toMap(ExamQuestionRelation::getQuestionId, ExamQuestionRelation::getCategory, (a, b) -> a));
        int totalScore = relations.stream().mapToInt(ExamQuestionRelation::getScore).sum();

        List<Question> questions = getExamQuestions(record.getExamId());
        
        List<ExamResultResponse.QuestionResult> qResults = questions.stream().map(q -> {
            ExamResultResponse.QuestionResult qr = new ExamResultResponse.QuestionResult();
            qr.setId(q.getId());
            qr.setContent(q.getContent());
            qr.setContentImageUrl(q.getContentImageUrl());
            qr.setType(q.getType());
            qr.setScore(scoreMap.getOrDefault(q.getId(), 0));
            qr.setOptions(q.getOptions());
            qr.setAnswer(q.getAnswer());
            qr.setAnalysis(q.getAnalysis());
            qr.setCategory(categoryMap.getOrDefault(q.getId(), q.getCategory()));
            qr.setStemId(q.getStemId());
            
            ExamAnswer a = ansMap.get(q.getId());
            if (a != null) {
                qr.setStudentAnswer(a.getStudentAnswer());
                qr.setIsCorrect(a.getIsCorrect());
            }
            return qr;
        }).collect(Collectors.toList());
        
        Map<String, ExamResultResponse.CategoryStat> categoryStatMap = new HashMap<>();
        for (ExamResultResponse.QuestionResult qr : qResults) {
            String cat = qr.getCategory();
            if (cat == null || cat.isEmpty()) cat = "未分类";
            
            ExamResultResponse.CategoryStat stat = categoryStatMap.computeIfAbsent(cat, k -> {
                ExamResultResponse.CategoryStat s = new ExamResultResponse.CategoryStat();
                s.setCategory(k);
                s.setTotalCount(0);
                s.setCorrectCount(0);
                s.setScore(0);
                return s;
            });
            
            stat.setTotalCount(stat.getTotalCount() + 1);
            stat.setScore(stat.getScore() + (qr.getScore() != null ? qr.getScore() : 0));
            if (qr.getIsCorrect() != null && qr.getIsCorrect() == 1) {
                stat.setCorrectCount(stat.getCorrectCount() + 1);
            }
        }
        
        List<ExamResultResponse.CategoryStat> categoryStats = new ArrayList<>(categoryStatMap.values());
        for (ExamResultResponse.CategoryStat stat : categoryStats) {
            if (stat.getTotalCount() > 0) {
                stat.setAccuracyRate(Math.round(stat.getCorrectCount() * 100.0 / stat.getTotalCount()) / 100.0);
            }
        }
        
        ExamResultResponse res = new ExamResultResponse();
        res.setRecord(record);
        res.setExam(exam);
        res.setTotalScore(totalScore);
        res.setQuestions(qResults);
        res.setCategoryStats(categoryStats);
        return res;
    }

    @Transactional
    public void updateRecordStatus(Long recordId, Integer status) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record != null) {
            record.setStatus(status);
            if (status == 2) { // Ended
                record.setSubmitTime(LocalDateTime.now());
            }
            examRecordMapper.updateById(record);
        }
    }

    public ExamRecord getRecord(Long examId, Long studentId) {
        return examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStudentId, studentId)
                .last("limit 1"));
    }

    @Transactional
    public ExamRecord startExam(Long examId, Long studentId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BizException(4001, "考试不存在");
        
        // Check if already started
        ExamRecord record = getRecord(examId, studentId);
        if (record != null) {
            long duration = exam.getDuration() * 60L;
            long elapsed = java.time.Duration.between(record.getStartTime(), LocalDateTime.now()).getSeconds();
            record.setRemainingSeconds(Math.max(0, duration - elapsed));
            return record;
        }

        // Check time
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BizException(4002, "考试未开始");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BizException(4003, "考试已结束");
        }

        record = new ExamRecord();
        record.setExamId(examId);
        record.setStudentId(studentId);
        record.setStatus(0); // In Progress
        record.setStartTime(now);
        examRecordMapper.insert(record);

        record.setRemainingSeconds((long) exam.getDuration() * 60L);
        return record;
    }

    public List<Question> getExamQuestions(Long examId) {
        List<ExamQuestionRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<ExamQuestionRelation>()
                .eq(ExamQuestionRelation::getExamId, examId)
                .orderByAsc(ExamQuestionRelation::getSortOrder));
        if (relations.isEmpty()) return new ArrayList<>();
        
        List<Long> qIds = relations.stream().map(ExamQuestionRelation::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, q -> q));
        
        List<Question> questions = new ArrayList<>();
        for (ExamQuestionRelation r : relations) {
            Question q = qMap.get(r.getQuestionId());
            if (q != null) {
                questions.add(q);
            }
        }
        return questions;
    }

    @Transactional
    public void submitAnswer(Long recordId, Long questionId, String answer, Integer isMarked) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || record.getStatus() != 0) {
            throw new BizException(4004, "考试已结束或记录不存在");
        }

        ExamAnswer ea = examAnswerMapper.selectOne(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId)
                .eq(ExamAnswer::getQuestionId, questionId));
        
        if (ea == null) {
            ea = new ExamAnswer();
            ea.setRecordId(recordId);
            ea.setQuestionId(questionId);
            ea.setStudentAnswer(answer);
            ea.setIsMarked(isMarked);
            examAnswerMapper.insert(ea);
        } else {
            ea.setStudentAnswer(answer);
            ea.setIsMarked(isMarked);
            examAnswerMapper.updateById(ea);
        }
    }

    @Transactional
    public void submitExam(Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) throw new BizException(4004, "记录不存在");
        if (record.getStatus() == 1) return; // Already submitted

        record.setStatus(1);
        record.setSubmitTime(LocalDateTime.now());
        
        // Calculate score (simple version)
        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId));
        
        List<ExamQuestionRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<ExamQuestionRelation>()
                .eq(ExamQuestionRelation::getExamId, record.getExamId()));
        Map<Long, Integer> scoreMap = relations.stream()
                .collect(Collectors.toMap(ExamQuestionRelation::getQuestionId, ExamQuestionRelation::getScore));
        
        int totalScore = 0;
        List<Long> qIds = answers.stream().map(ExamAnswer::getQuestionId).collect(Collectors.toList());
        if (!qIds.isEmpty()) {
            Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds).stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity()));
            
            for (ExamAnswer ans : answers) {
                Question q = qMap.get(ans.getQuestionId());
                if (q != null && ans.getStudentAnswer() != null && ans.getStudentAnswer().equalsIgnoreCase(q.getAnswer())) {
                    ans.setIsCorrect(1);
                    totalScore += scoreMap.getOrDefault(q.getId(), 0);
                } else {
                    ans.setIsCorrect(0);
                    // Add to Wrong Book
                    saveWrongQuestion(record.getStudentId(), q.getId(), record.getExamId());
                }
                examAnswerMapper.updateById(ans);
            }
        }
        
        record.setScore(totalScore);
        examRecordMapper.updateById(record);
    }

    private void saveWrongQuestion(Long studentId, Long questionId, Long examId) {
        WrongBook wb = wrongBookMapper.selectOne(new LambdaQueryWrapper<WrongBook>()
                .eq(WrongBook::getStudentId, studentId)
                .eq(WrongBook::getQuestionId, questionId));
        
        if (wb == null) {
            wb = new WrongBook();
            wb.setStudentId(studentId);
            wb.setQuestionId(questionId);
            wb.setExamId(examId);
            wb.setWrongCount(1);
            wb.setPracticeCount(0);
            wb.setPracticeCorrectCount(0);
            wb.setCreateTime(LocalDateTime.now());
            wb.setUpdateTime(LocalDateTime.now());
            wrongBookMapper.insert(wb);
        } else {
            wb.setWrongCount(wb.getWrongCount() + 1);
            wb.setExamId(examId); // Update to latest exam
            wb.setUpdateTime(LocalDateTime.now());
            wrongBookMapper.updateById(wb);
        }
    }
}
