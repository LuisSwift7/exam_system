package com.examsystem.service.exam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examsystem.common.BizException;
import com.examsystem.entity.Question;
import com.examsystem.entity.WrongBook;
import com.examsystem.mapper.QuestionMapper;
import com.examsystem.mapper.WrongBookMapper;
import com.examsystem.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WrongBookService {
    private final WrongBookMapper wrongBookMapper;
    private final QuestionMapper questionMapper;

    public IPage<WrongBookVo> getWrongQuestions(int page, int size, String keyword, Integer type) {
        Long userId = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        
        Page<WrongBook> p = new Page<>(page, size);
        
        // Complex query: find questions first if keyword/type provided, then find wrong book entries?
        // Or find wrong book entries then filter?
        // Since we are using MyBatis Plus without XML, we might need to do some manual joining or filtering in memory if dataset is small, or use nested queries.
        // For simplicity and performance with large datasets, usually we want to filter by question attributes.
        // But WrongBook table only has questionId.
        // Approach: 
        // 1. If keyword/type is present, find matching Question IDs first.
        // 2. Query WrongBook with those Question IDs and userId.
        
        List<Long> questionIds = null;
        if (StringUtils.hasText(keyword) || type != null) {
            LambdaQueryWrapper<Question> qWrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(keyword)) qWrapper.like(Question::getContent, keyword);
            if (type != null) qWrapper.eq(Question::getType, type);
            qWrapper.select(Question::getId);
            List<Object> ids = questionMapper.selectObjs(qWrapper);
            if (ids.isEmpty()) {
                return new Page<>(page, size); // No matching questions
            }
            questionIds = ids.stream().map(o -> (Long)o).collect(Collectors.toList());
        }

        LambdaQueryWrapper<WrongBook> wbWrapper = new LambdaQueryWrapper<>();
        wbWrapper.eq(WrongBook::getStudentId, userId);
        if (questionIds != null) {
            wbWrapper.in(WrongBook::getQuestionId, questionIds);
        }
        wbWrapper.orderByDesc(WrongBook::getUpdateTime);

        Page<WrongBook> result = wrongBookMapper.selectPage(p, wbWrapper);
        
        // Convert to VO
        List<WrongBookVo> vos = result.getRecords().stream().map(wb -> {
            WrongBookVo vo = new WrongBookVo();
            BeanUtils.copyProperties(wb, vo);
            Question q = questionMapper.selectById(wb.getQuestionId());
            if (q != null) {
                vo.setQuestionContent(q.getContent());
                vo.setQuestionType(q.getType());
                vo.setQuestionOptions(q.getOptions());
                vo.setQuestionAnswer(q.getAnswer());
                vo.setQuestionAnalysis(q.getAnalysis());
            }
            return vo;
        }).collect(Collectors.toList());

        Page<WrongBookVo> voPage = new Page<>();
        BeanUtils.copyProperties(result, voPage);
        voPage.setRecords(vos);
        
        return voPage;
    }

    public void practice(Long id, boolean correct) {
        WrongBook wb = wrongBookMapper.selectById(id);
        if (wb == null) throw new BizException(404, "记录不存在");
        
        wb.setPracticeCount(wb.getPracticeCount() + 1);
        if (correct) {
            wb.setPracticeCorrectCount(wb.getPracticeCorrectCount() + 1);
        }
        wb.setUpdateTime(LocalDateTime.now());
        wrongBookMapper.updateById(wb);
    }
    
    public WrongBookStats getStats() {
        Long userId = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        
        List<WrongBook> list = wrongBookMapper.selectList(new LambdaQueryWrapper<WrongBook>().eq(WrongBook::getStudentId, userId));
        
        WrongBookStats stats = new WrongBookStats();
        stats.setTotalCount(list.size());
        
        // Calculate accuracy
        long totalPractice = list.stream().mapToLong(WrongBook::getPracticeCount).sum();
        long totalCorrect = list.stream().mapToLong(WrongBook::getPracticeCorrectCount).sum();
        
        stats.setPracticeCount((int)totalPractice);
        stats.setPracticeAccuracy(totalPractice == 0 ? 0.0 : (double)totalCorrect / totalPractice);
        
        // Count by type (need to fetch questions)
        if (!list.isEmpty()) {
            List<Long> qIds = list.stream().map(WrongBook::getQuestionId).collect(Collectors.toList());
            List<Question> questions = questionMapper.selectBatchIds(qIds);
            Map<Integer, Long> typeCount = questions.stream().collect(Collectors.groupingBy(Question::getType, Collectors.counting()));
            stats.setTypeDistribution(typeCount);
        }
        
        return stats;
    }

    @Data
    public static class WrongBookVo extends WrongBook {
        private String questionContent;
        private Integer questionType;
        private List<String> questionOptions;
        private String questionAnswer;
        private String questionAnalysis;
    }
    
    @Data
    public static class WrongBookStats {
        private Integer totalCount;
        private Integer practiceCount;
        private Double practiceAccuracy;
        private Map<Integer, Long> typeDistribution;
    }
}
