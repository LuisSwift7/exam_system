package com.examsystem.service.exam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.examsystem.common.BizException;
import com.examsystem.entity.*;
import com.examsystem.mapper.*;
import com.examsystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public Exam getExam(Long examId) {
        return examMapper.selectById(examId);
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
        if (record != null) return record;

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

        return record;
    }

    public List<Question> getExamQuestions(Long examId) {
        List<Long> qIds = relationMapper.selectQuestionIdsByExamId(examId);
        if (qIds.isEmpty()) return new ArrayList<>();
        return questionMapper.selectBatchIds(qIds);
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
                }
                examAnswerMapper.updateById(ans);
            }
        }
        
        record.setScore(totalScore);
        examRecordMapper.updateById(record);
    }
}
