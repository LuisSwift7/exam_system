package com.examsystem.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AutoGenerateRequest {
    private Long examId;
    private Integer totalScore;
    private Double difficulty; // 目标平均难度
    private Integer questionCount; // 总题数
    
    // 题型分布配置
    private List<TypeConfig> typeConfigs;
    
    // 知识点覆盖要求 (知识点名称 -> 题目数量)
    private Map<String, Integer> knowledgePointRequirements;
    
    // 权重配置
    private Weights weights;
    
    // 约束配置
    private Constraints constraints;

    @Data
    public static class TypeConfig {
        private Integer type; // 题目类型
        private Integer count; // 题目数量
        private Integer score; // 每题分值
    }

    @Data
    public static class Weights {
        private Double difficultyWeight = 0.25; // w1: 难度偏差权重
        private Double coverageWeight = 0.25;   // w2: 知识点覆盖权重
        private Double typeRatioWeight = 0.25;  // w3: 题型比例权重
        private Double scoreWeight = 0.25;      // w4: 总分误差权重
    }
    
    @Data
    public static class Constraints {
        private Boolean avoidDuplicates = true; // 避免重复题目
        private Integer examDuration = 120;     // 考试时长(分钟)
        private List<Long> mandatoryQuestionIds; // 必选题ID
    }
}
