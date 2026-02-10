package com.examsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.examsystem.entity.ExamQuestionRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamQuestionRelationMapper extends BaseMapper<ExamQuestionRelation> {
    @Select("SELECT question_id FROM exam_question_relation WHERE exam_id = #{examId} ORDER BY sort_order ASC")
    List<Long> selectQuestionIdsByExamId(Long examId);
}
