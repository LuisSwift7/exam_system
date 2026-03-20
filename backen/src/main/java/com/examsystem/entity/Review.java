package com.examsystem.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class Review {
    private Long id;
    private Long examId;
    private Long teacherId;
    private String title;
    private String content;
    private Integer status; // 0: 未发布 1: 已发布
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String examTitle;
    private String summary;
    private List<QuestionReviewItem> questionReviews;

    @Data
    public static class QuestionReviewItem {
        private Long questionId;
        private Integer questionNo;
        private String questionContent;
        private String content;
    }
}
