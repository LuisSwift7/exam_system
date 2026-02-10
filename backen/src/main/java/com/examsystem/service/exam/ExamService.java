package com.examsystem.service.exam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examsystem.common.BizException;
import com.examsystem.entity.Exam;
import com.examsystem.entity.ExamQuestionRelation;
import com.examsystem.entity.Question;
import com.examsystem.mapper.ExamMapper;
import com.examsystem.mapper.ExamQuestionRelationMapper;
import com.examsystem.mapper.QuestionMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {
    private final ExamMapper examMapper;
    private final ExamQuestionRelationMapper relationMapper;
    private final QuestionMapper questionMapper;

    public IPage<Exam> getAvailableExams(int page, int size) {
        Page<Exam> p = new Page<>(page, size);
        return examMapper.selectPage(p, new LambdaQueryWrapper<Exam>()
                .orderByDesc(Exam::getStartTime));
    }

    public IPage<Exam> getExams(int page, int size, String keyword) {
        Page<Exam> p = new Page<>(page, size);
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .orderByDesc(Exam::getCreatedTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Exam::getTitle, keyword);
        }
        return examMapper.selectPage(p, wrapper);
    }

    public void createExam(Exam exam) {
        exam.setId(null);
        exam.setStatus(0); // Default Not Published
        examMapper.insert(exam);
    }

    public void updateExam(Exam exam) {
        if (exam.getId() == null) throw new BizException(400, "ID不能为空");
        examMapper.updateById(exam);
    }

    public void deleteExam(Long id) {
        examMapper.deleteById(id);
        // Also delete relations
        relationMapper.delete(new LambdaQueryWrapper<ExamQuestionRelation>().eq(ExamQuestionRelation::getExamId, id));
    }

    @Transactional
    public void manualCompose(Long examId, List<Long> questionIds, Integer score) {
        // Clear existing
        relationMapper.delete(new LambdaQueryWrapper<ExamQuestionRelation>().eq(ExamQuestionRelation::getExamId, examId));
        
        int order = 1;
        for (Long qId : questionIds) {
            ExamQuestionRelation r = new ExamQuestionRelation();
            r.setExamId(examId);
            r.setQuestionId(qId);
            r.setScore(score != null ? score : 2); // Default 2 points
            r.setSortOrder(order++);
            relationMapper.insert(r);
        }
    }

    @Transactional
    public void autoCompose(Long examId, AutoComposeStrategy strategy) {
        // Clear existing
        relationMapper.delete(new LambdaQueryWrapper<ExamQuestionRelation>().eq(ExamQuestionRelation::getExamId, examId));

        // Simple Random Strategy (Simplified NSGA-II placeholder)
        // In real world: Use strategy.difficulty, strategy.type to filter
        
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (strategy.getType() != null) wrapper.eq(Question::getType, strategy.getType());
        // If difficulty provided, we could use a range or exact match, here we ignore for randomness or simple match
        
        List<Question> candidates = questionMapper.selectList(wrapper);
        if (candidates.size() < strategy.getCount()) {
            throw new BizException(400, "题库数量不足，无法满足组卷要求");
        }

        Collections.shuffle(candidates);
        List<Question> selected = candidates.subList(0, strategy.getCount());

        int order = 1;
        for (Question q : selected) {
            ExamQuestionRelation r = new ExamQuestionRelation();
            r.setExamId(examId);
            r.setQuestionId(q.getId());
            r.setScore(strategy.getScore() != null ? strategy.getScore() : 2);
            r.setSortOrder(order++);
            relationMapper.insert(r);
        }
    }

    public List<Long> getExamQuestionIds(Long examId) {
        return relationMapper.selectQuestionIdsByExamId(examId);
    }

    @Data
    public static class AutoComposeStrategy {
        private Integer type; // 1: Single, 2: Multiple
        private Integer count;
        private Integer difficulty; // 1-5
        private Integer score;
    }
}
