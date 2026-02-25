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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    public void publishExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new BizException(400, "试卷不存在");
        
        // Check if exam has questions
        Long count = relationMapper.selectCount(new LambdaQueryWrapper<ExamQuestionRelation>().eq(ExamQuestionRelation::getExamId, id));
        if (count == 0) {
            throw new BizException(400, "试卷未添加题目，无法发布");
        }
        
        exam.setStatus(1); // 1: Published
        examMapper.updateById(exam);
    }

    @Transactional
    public void manualCompose(Long examId, List<Long> questionIds, Integer score) {
        relationMapper.delete(new LambdaQueryWrapper<ExamQuestionRelation>().eq(ExamQuestionRelation::getExamId, examId));
        
        if (questionIds == null || questionIds.isEmpty()) return;
        
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> qMap = questions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        
        int order = 1;
        for (Long qId : questionIds) {
            ExamQuestionRelation r = new ExamQuestionRelation();
            r.setExamId(examId);
            r.setQuestionId(qId);
            r.setScore(score != null ? score : 2);
            r.setSortOrder(order++);
            
            Question q = qMap.get(qId);
            if (q != null) {
                r.setCategory(q.getCategory());
            }
            
            relationMapper.insert(r);
        }
    }

    @Transactional
    public void autoCompose(Long examId, AutoComposeStrategy strategy) {
        relationMapper.delete(new LambdaQueryWrapper<ExamQuestionRelation>().eq(ExamQuestionRelation::getExamId, examId));

        if (strategy.getCategoryCounts() != null && !strategy.getCategoryCounts().isEmpty()) {
            int totalRequired = 0;
            for (CategoryCount cc : strategy.getCategoryCounts()) {
                totalRequired += cc.getCount();
            }
            
            int order = 1;
            for (CategoryCount cc : strategy.getCategoryCounts()) {
                LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Question::getCategory, cc.getCategory());
                if (strategy.getType() != null) {
                    wrapper.eq(Question::getType, strategy.getType());
                }
                
                List<Question> candidates = questionMapper.selectList(wrapper);
                if (candidates.size() < cc.getCount()) {
                    throw new BizException(400, "分类【" + cc.getCategory() + "】题库数量不足，需要" + cc.getCount() + "道，仅有" + candidates.size() + "道");
                }
                
                Collections.shuffle(candidates);
                List<Question> selected = candidates.subList(0, cc.getCount());
                
                for (Question q : selected) {
                    ExamQuestionRelation r = new ExamQuestionRelation();
                    r.setExamId(examId);
                    r.setQuestionId(q.getId());
                    r.setScore(cc.getScore() != null ? cc.getScore() : 2);
                    r.setSortOrder(order++);
                    r.setCategory(q.getCategory());
                    relationMapper.insert(r);
                }
            }
        } else {
            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
            if (strategy.getType() != null) wrapper.eq(Question::getType, strategy.getType());
            if (strategy.getCategory() != null) wrapper.eq(Question::getCategory, strategy.getCategory());
            
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
                r.setCategory(q.getCategory());
                relationMapper.insert(r);
            }
        }
    }

    public List<Long> getExamQuestionIds(Long examId) {
        return relationMapper.selectQuestionIdsByExamId(examId);
    }

    public ExamPreviewVo getExamPreview(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new BizException(400, "试卷不存在");

        List<ExamQuestionRelation> relations = relationMapper.selectList(
                new LambdaQueryWrapper<ExamQuestionRelation>()
                        .eq(ExamQuestionRelation::getExamId, id)
                        .orderByAsc(ExamQuestionRelation::getSortOrder)
        );

        if (relations.isEmpty()) {
            return new ExamPreviewVo(exam, Collections.emptyList());
        }

        List<Long> qIds = relations.stream().map(ExamQuestionRelation::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIds(qIds);
        Map<Long, Question> qMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        List<QuestionPreviewVo> questionVos = new ArrayList<>();
        for (ExamQuestionRelation r : relations) {
            Question q = qMap.get(r.getQuestionId());
            if (q != null) {
                QuestionPreviewVo vo = new QuestionPreviewVo();
                BeanUtils.copyProperties(q, vo);
                vo.setScore(r.getScore());
                questionVos.add(vo);
            }
        }

        return new ExamPreviewVo(exam, questionVos);
    }

    @Data
    public static class AutoComposeStrategy {
        private Integer type;
        private Integer count;
        private Integer difficulty;
        private Integer score;
        private String category;
        private List<CategoryCount> categoryCounts;
    }

    @Data
    public static class CategoryCount {
        private String category;
        private Integer count;
        private Integer score;
    }

    @Data
    public static class ExamPreviewVo {
        private Exam exam;
        private List<QuestionPreviewVo> questions;

        public ExamPreviewVo(Exam exam, List<QuestionPreviewVo> questions) {
            this.exam = exam;
            this.questions = questions;
        }
    }

    @Data
    public static class QuestionPreviewVo extends Question {
        private Integer score;
    }
}
