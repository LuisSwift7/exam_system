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
    private RankingInfo ranking;

    @Data
    public static class QuestionResult {
        private Long id;
        private String content;
        private String contentImageUrl;
        private Integer type;
        private Integer score;
        private List<Option> options;
        private String answer;
        private String analysis;
        private String studentAnswer;
        private Integer isCorrect;
        private String category;
        private Long stemId; // 资料分析题的共享题干ID
    }

    @Data
    public static class CategoryStat {
        private String category;
        private Integer totalCount;
        private Integer correctCount;
        private Integer score;
        private Double accuracyRate;
    }

    @Data
    public static class RankingInfo {
        private Long classId;
        private String className;
        private Integer myRank;
        private Integer participantCount;
        private Integer classStudentCount;
        private List<RankingItem> leaderboard;
    }

    @Data
    public static class RankingItem {
        private Integer rank;
        private Long studentId;
        private String studentName;
        private Integer score;
        private String submitTime;
        private Boolean currentStudent;
    }
}
