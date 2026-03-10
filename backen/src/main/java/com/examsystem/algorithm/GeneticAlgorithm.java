package com.examsystem.algorithm;

import com.examsystem.dto.AutoGenerateRequest;
import com.examsystem.entity.Question;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GeneticAlgorithm {

    private static final int POPULATION_SIZE = 100;
    private static final int MAX_GENERATIONS = 200;
    private static final double CROSSOVER_RATE = 0.85;
    private static final double MUTATION_RATE = 0.15;
    private static final int EARLY_STOP_GENERATIONS = 20;

    /**
     * 核心入口方法
     */
    public List<Question> generatePaper(List<Question> questionPool, AutoGenerateRequest request) {
        // 1. 数据预处理：将题目池按类型/知识点分组，便于快速检索
        Map<Integer, List<Question>> questionsByType = questionPool.stream()
                .collect(Collectors.groupingBy(Question::getType));
        
        // 2. 初始化种群
        List<Chromosome> population = initializePopulation(POPULATION_SIZE, request, questionsByType);
        
        List<Chromosome> bestHistory = new ArrayList<>();
        int noImprovementCount = 0;
        double bestFitness = -1;

        // 3. 进化循环
        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            // 计算适应度
            calculateFitness(population, request);
            
            // 非支配排序与拥挤距离计算 (简化版：直接使用加权适应度进行排序)
            // NSGA-II 的核心是多目标优化，但这里用户最终要求综合得分，所以我们可以用加权和作为主适应度，
            // 同时保留帕累托前沿的概念用于多样性保持。
            // 这里为了简化实现且满足用户 "输出最符合权重的一套" 的需求，我们主要优化加权适应度。
            
            population.sort(Comparator.comparingDouble(Chromosome::getFitness).reversed());
            
            Chromosome currentBest = population.get(0);
            if (currentBest.getFitness() > bestFitness) {
                bestFitness = currentBest.getFitness();
                noImprovementCount = 0;
            } else {
                noImprovementCount++;
            }

            // 早停策略
            if (noImprovementCount >= EARLY_STOP_GENERATIONS) {
                log.info("Early stopping at generation {}", gen);
                break;
            }

            // 选择、交叉、变异
            List<Chromosome> newPopulation = new ArrayList<>();
            
            // 精英保留 (Top 10%)
            int eliteCount = (int) (POPULATION_SIZE * 0.1);
            newPopulation.addAll(population.subList(0, eliteCount));
            
            while (newPopulation.size() < POPULATION_SIZE) {
                // 锦标赛选择
                Chromosome p1 = tournamentSelection(population);
                Chromosome p2 = tournamentSelection(population);
                
                // 交叉
                Chromosome child;
                if (Math.random() < CROSSOVER_RATE) {
                    child = crossover(p1, p2, request);
                } else {
                    child = p1.clone();
                }
                
                // 变异
                if (Math.random() < MUTATION_RATE) {
                    mutate(child, questionsByType, request);
                }
                
                // 可行性修复 (确保无重复，题型满足)
                repair(child, questionsByType, request);
                
                newPopulation.add(child);
            }
            
            population = newPopulation;
        }

        // 4. 返回最优解
        calculateFitness(population, request);
        population.sort(Comparator.comparingDouble(Chromosome::getFitness).reversed());
        return population.get(0).getQuestions();
    }

    private static final Map<Integer, String> TYPE_NAMES = new HashMap<>();
    static {
        TYPE_NAMES.put(1, "单选题");
        TYPE_NAMES.put(2, "多选题");
        TYPE_NAMES.put(3, "判断题");
        TYPE_NAMES.put(4, "简答题");
    }

    private List<Chromosome> initializePopulation(int size, AutoGenerateRequest request, Map<Integer, List<Question>> pool) {
        // 0. 预检查：检查知识点题目数量
        if (request.getKnowledgePointRequirements() != null) {
            Map<String, Integer> kpReqs = request.getKnowledgePointRequirements();
            // 计算当前题目池中各知识点的可用数量
            Map<String, Long> kpCounts = pool.values().stream()
                    .flatMap(List::stream)
                    .filter(q -> q.getCategory() != null)
                    .collect(Collectors.groupingBy(Question::getCategory, Collectors.counting()));
            
            for (Map.Entry<String, Integer> entry : kpReqs.entrySet()) {
                String kp = entry.getKey();
                int reqCount = entry.getValue();
                long actualCount = kpCounts.getOrDefault(kp, 0L);
                
                // 注意：这里的检查是基于“整个池子”的，但题目是按Type划分的。
                // 如果用户要求 5道言语理解，且要求10道单选。
                // 可能池子里有5道言语理解（全是多选），0道单选。
                // 那么这里通过，但下面Type检查会失败。
                // 如果池子里有0道言语理解。
                // 那么这里会失败。
                // 这是一个基本检查。
                if (actualCount < reqCount) {
                    throw new RuntimeException("题库中 [" + kp + "] 类的题目数量不足 (需要 " + reqCount + ", 实际 " + actualCount + ")");
                }
            }
        }

        List<Chromosome> population = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            List<Question> genes = new ArrayList<>();
            // 按题型配置随机抽取
            if (request.getTypeConfigs() != null) {
                for (AutoGenerateRequest.TypeConfig config : request.getTypeConfigs()) {
                    List<Question> candidates = pool.getOrDefault(config.getType(), Collections.emptyList());
                    if (candidates.size() < config.getCount()) {
                        String typeName = TYPE_NAMES.getOrDefault(config.getType(), "类型" + config.getType());
                        throw new RuntimeException("题库中 " + typeName + " 的题目数量不足 (需要 " + config.getCount() + ", 实际 " + candidates.size() + ")");
                    }
                    List<Question> shuffled = new ArrayList<>(candidates);
                    Collections.shuffle(shuffled);
                    genes.addAll(shuffled.subList(0, config.getCount()));
                }
            }
            population.add(new Chromosome(genes));
        }
        return population;
    }

    private void calculateFitness(List<Chromosome> population, AutoGenerateRequest request) {
        for (Chromosome chrom : population) {
            double f1 = calculateDifficultyDeviation(chrom, request);
            double f2 = calculateKnowledgeCoverage(chrom, request);
            double f3 = calculateTypeRatioError(chrom, request);
            double f4 = calculateScoreError(chrom, request);
            
            double penalty = calculatePenalty(chrom, request);
            
            // 综合适应度 = w1*f1 + ... - 罚分
            // 注意：f1, f3, f4 是误差（越小越好），f2 是覆盖率（越大越好，但目标是 1-覆盖率 越小越好? 用户公式是 1 - 覆盖率）
            // 用户公式：f2 = 1 - (已覆盖/总要求)。所以 f2 也是越小越好。
            // 适应度应该越大越好，所以取倒数或负数。
            // 用户公式：F = w1*f1 + ... + 罚分。这里 F 是误差总和，越小越好。
            // 遗传算法通常最大化适应度，所以我们取 Fitness = 1 / (F + epsilon) 或者 -F。
            // 这里使用 100 - weighted_error 作为适应度，或者直接用 -error。
            
            double weightedError = 
                    request.getWeights().getDifficultyWeight() * f1 +
                    request.getWeights().getCoverageWeight() * f2 +
                    request.getWeights().getTypeRatioWeight() * f3 +
                    request.getWeights().getScoreWeight() * f4 +
                    penalty;
            
            // 适应度函数：误差越小适应度越高
            chrom.setFitness(100.0 / (1.0 + weightedError));
            chrom.setError(weightedError);
        }
    }

    private double calculateDifficultyDeviation(Chromosome chrom, AutoGenerateRequest request) {
        if (request.getDifficulty() == null) return 0.0;
        double avgDiff = chrom.getQuestions().stream()
                .mapToInt(Question::getDifficulty)
                .average().orElse(0.0);
        return Math.abs(avgDiff - request.getDifficulty());
    }

    private double calculateKnowledgeCoverage(Chromosome chrom, AutoGenerateRequest request) {
        if (request.getKnowledgePointRequirements() == null || request.getKnowledgePointRequirements().isEmpty()) return 0.0;
        
        Set<String> coveredPoints = chrom.getQuestions().stream()
                .map(Question::getCategory) // 假设 Category 对应知识点
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        long requiredPointsCount = request.getKnowledgePointRequirements().size();
        long coveredCount = request.getKnowledgePointRequirements().keySet().stream()
                .filter(coveredPoints::contains)
                .count();
        
        return 1.0 - ((double) coveredCount / requiredPointsCount);
    }

    private double calculateTypeRatioError(Chromosome chrom, AutoGenerateRequest request) {
        // 简单实现：由于初始化和变异都尽量保持题型数量，这里误差通常为0
        // 如果变异破坏了题型比例，这里会计算误差
        Map<Integer, Long> currentCounts = chrom.getQuestions().stream()
                .collect(Collectors.groupingBy(Question::getType, Collectors.counting()));
        
        double error = 0.0;
        if (request.getTypeConfigs() != null) {
            for (AutoGenerateRequest.TypeConfig config : request.getTypeConfigs()) {
                long actual = currentCounts.getOrDefault(config.getType(), 0L);
                error += Math.abs(actual - config.getCount());
            }
        }
        return error;
    }

    private double calculateScoreError(Chromosome chrom, AutoGenerateRequest request) {
        if (request.getTotalScore() == null) return 0.0;
        // 假设每题分值固定或从 TypeConfig 获取，这里简化为从请求配置计算目标分值
        // 实际题目可能有自己的分值，这里需要确认 Question 是否有 score 字段。
        // Question 实体没有 score 字段，分值通常在 ExamQuestionRelation 中。
        // 我们假设 TypeConfig 定义了该类型题目的分值。
        
        int totalScore = 0;
        Map<Integer, Integer> typeScores = request.getTypeConfigs().stream()
                .collect(Collectors.toMap(AutoGenerateRequest.TypeConfig::getType, AutoGenerateRequest.TypeConfig::getScore));
        
        for (Question q : chrom.getQuestions()) {
            totalScore += typeScores.getOrDefault(q.getType(), 2); // 默认2分
        }
        
        return Math.abs(totalScore - request.getTotalScore());
    }

    private double calculatePenalty(Chromosome chrom, AutoGenerateRequest request) {
        double penalty = 0.0;
        
        // 1. 重复题目罚分
        Set<Long> uniqueIds = new HashSet<>();
        int duplicates = 0;
        for (Question q : chrom.getQuestions()) {
            if (!uniqueIds.add(q.getId())) {
                duplicates++;
            }
        }
        penalty += duplicates * 100;
        
        // 2. 必选题缺失罚分
        if (request.getConstraints() != null && request.getConstraints().getMandatoryQuestionIds() != null) {
            for (Long mid : request.getConstraints().getMandatoryQuestionIds()) {
                boolean found = chrom.getQuestions().stream().anyMatch(q -> q.getId().equals(mid));
                if (!found) penalty += 50;
            }
        }
        
        // 3. 超时风险 (暂不实现，需要题目预估时间)
        
        return penalty;
    }

    private Chromosome tournamentSelection(List<Chromosome> population) {
        int k = 3;
        Chromosome best = null;
        for (int i = 0; i < k; i++) {
            Chromosome random = population.get(new Random().nextInt(population.size()));
            if (best == null || random.getFitness() > best.getFitness()) {
                best = random;
            }
        }
        return best;
    }

    private Chromosome crossover(Chromosome p1, Chromosome p2, AutoGenerateRequest request) {
        // 分段交叉：这里简化为单点交叉
        List<Question> genes1 = p1.getQuestions();
        List<Question> genes2 = p2.getQuestions();
        int size = Math.min(genes1.size(), genes2.size());
        
        int point = new Random().nextInt(size);
        List<Question> newGenes = new ArrayList<>();
        newGenes.addAll(genes1.subList(0, point));
        newGenes.addAll(genes2.subList(point, size));
        
        return new Chromosome(newGenes);
    }

    private void mutate(Chromosome chrom, Map<Integer, List<Question>> pool, AutoGenerateRequest request) {
        // 随机选择一个基因位进行替换
        List<Question> genes = chrom.getQuestions();
        int index = new Random().nextInt(genes.size());
        Question original = genes.get(index);
        
        // 尝试用同类型的其他题目替换
        List<Question> candidates = pool.get(original.getType());
        if (candidates != null && !candidates.isEmpty()) {
            Question replacement = candidates.get(new Random().nextInt(candidates.size()));
            genes.set(index, replacement);
        }
    }

    private void repair(Chromosome chrom, Map<Integer, List<Question>> pool, AutoGenerateRequest request) {
        // 1. 去重
        Set<Long> existingIds = new HashSet<>();
        List<Question> genes = chrom.getQuestions();
        for (int i = 0; i < genes.size(); i++) {
            Question q = genes.get(i);
            if (existingIds.contains(q.getId())) {
                // 发现重复，尝试替换
                List<Question> candidates = pool.get(q.getType());
                if (candidates != null) {
                    for (Question c : candidates) {
                        if (!existingIds.contains(c.getId())) {
                            genes.set(i, c);
                            existingIds.add(c.getId());
                            break;
                        }
                    }
                }
            } else {
                existingIds.add(q.getId());
            }
        }
        
        // 2. 检查题型数量并修复 (略，假设交叉变异保持总数不变)
    }

    @Data
    public static class Chromosome implements Cloneable {
        private List<Question> questions;
        private double fitness;
        private double error;
        
        public Chromosome(List<Question> questions) {
            this.questions = new ArrayList<>(questions);
        }
        
        @Override
        public Chromosome clone() {
            try {
                Chromosome clone = (Chromosome) super.clone();
                clone.questions = new ArrayList<>(this.questions);
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }
}
