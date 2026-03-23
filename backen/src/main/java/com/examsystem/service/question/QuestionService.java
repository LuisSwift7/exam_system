package com.examsystem.service.question;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examsystem.common.BizException;
import com.examsystem.entity.Question;
import com.examsystem.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {
    private final QuestionMapper questionMapper;

    public IPage<Question> getQuestions(int page, int size, String keyword, Integer type, String category) {
        Page<Question> p = new Page<>(page, size);
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Question::getCreatedTime);
        
        if (StringUtils.hasText(keyword)) {
            String trimmedKeyword = keyword.trim();
            wrapper.and(w -> {
                w.like(Question::getContent, trimmedKeyword)
                        .or()
                        .like(Question::getAnalysis, trimmedKeyword)
                        .or()
                        .like(Question::getCategory, trimmedKeyword);
                if (trimmedKeyword.matches("\\d+")) {
                    w.or().eq(Question::getId, Long.parseLong(trimmedKeyword));
                }
            });
        }
        if (type != null) {
            wrapper.eq(Question::getType, type);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Question::getCategory, category);
        }
        return questionMapper.selectPage(p, wrapper);
    }

    public void addQuestion(Question question) {
        question.setId(null);
        questionMapper.insert(question);
    }

    public void updateQuestion(Question question) {
        if (question.getId() == null) throw new BizException(400, "ID不能为空");
        questionMapper.updateById(question);
    }

    public void deleteQuestion(Long id) {
        questionMapper.deleteById(id);
    }

    public void deleteQuestions(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        questionMapper.deleteBatchIds(ids);
    }
}
