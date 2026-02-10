package com.examsystem.controller.dto;

import com.examsystem.entity.Exam;
import com.examsystem.entity.ExamRecord;
import lombok.Data;
import java.util.List;

@Data
public class ExamResultResponse {
    private ExamRecord record;
    private Exam exam;
    private Integer totalScore;
    private List<QuestionResult> questions;

    @Data
    public static class QuestionResult {
        private Long id;
        private String content;
        private Integer type;
        private Integer score;
        private List<String> options;
        private String answer;
        private String analysis;
        private String studentAnswer;
        private Integer isCorrect;
    }
}
