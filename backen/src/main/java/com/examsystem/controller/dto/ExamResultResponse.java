package com.examsystem.controller.dto;

import com.examsystem.entity.Exam;
import com.examsystem.entity.ExamRecord;
import com.examsystem.entity.Option;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ExamResultResponse {
    private ExamRecord record;
    private Exam exam;
    private Integer totalScore;
    private List<QuestionResult> questions;
    private List<CategoryStat> categoryStats;

    @Data
    public static class QuestionResult {
        private Long id;
        private String content;
        private Integer type;
        private Integer score;
        private List<Option> options;
        private String answer;
        private String analysis;
        private String studentAnswer;
        private Integer isCorrect;
        private String category;
    }

    @Data
    public static class CategoryStat {
        private String category;
        private Integer totalCount;
        private Integer correctCount;
        private Integer score;
        private Double accuracyRate;
    }
}
