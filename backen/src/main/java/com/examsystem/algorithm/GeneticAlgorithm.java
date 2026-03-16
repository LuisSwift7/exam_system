package com.examsystem.algorithm;

import com.examsystem.dto.AutoGenerateRequest;
import com.examsystem.entity.Question;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GeneticAlgorithm {

    private static final int POPULATION_SIZE = 100;
    private static final int MAX_GENERATIONS = 200;
    private static final double CROSSOVER_RATE = 0.85;
    private static final double MUTATION_RATE = 0.15;
    private static final int EARLY_STOP_GENERATIONS = 20;
    private static final Random RANDOM = new Random();

    private static final Map<Integer, String> TYPE_NAMES = new HashMap<>();

    static {
        TYPE_NAMES.put(1, "单选题");
        TYPE_NAMES.put(2, "多选题");
        TYPE_NAMES.put(3, "判断题");
        TYPE_NAMES.put(4, "简答题");
    }

    public List<Question> generatePaper(List<Question> questionPool, AutoGenerateRequest request) {
        Map<Integer, List<Question>> questionsByType = questionPool.stream()
                .filter(Objects::nonNull)
                .filter(question -> question.getId() != null)
                .collect(Collectors.groupingBy(Question::getType));

        validateKnowledgeCoverage(request, questionPool);

        Map<Integer, Integer> targetCounts = resolveTargetCounts(request, questionsByType);
        List<Chromosome> population = initializePopulation(POPULATION_SIZE, targetCounts, questionsByType);

        int noImprovementCount = 0;
        double bestFitness = -1;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            calculateFitness(population, request, targetCounts);
            population.sort(Comparator.comparingDouble(Chromosome::getFitness).reversed());

            Chromosome currentBest = population.get(0);
            if (currentBest.getFitness() > bestFitness) {
                bestFitness = currentBest.getFitness();
                noImprovementCount = 0;
            } else {
                noImprovementCount++;
            }

            if (noImprovementCount >= EARLY_STOP_GENERATIONS) {
                log.info("Early stopping at generation {}", gen);
                break;
            }

            List<Chromosome> newPopulation = new ArrayList<>();
            int eliteCount = Math.max(1, (int) (POPULATION_SIZE * 0.1));
            newPopulation.addAll(population.subList(0, eliteCount));

            while (newPopulation.size() < POPULATION_SIZE) {
                Chromosome parent1 = tournamentSelection(population);
                Chromosome parent2 = tournamentSelection(population);

                Chromosome child = Math.random() < CROSSOVER_RATE
                        ? crossover(parent1, parent2, targetCounts, questionsByType)
                        : parent1.clone();

                if (Math.random() < MUTATION_RATE) {
                    mutate(child, questionsByType);
                }

                repair(child, questionsByType, targetCounts);
                newPopulation.add(child);
            }

            population = newPopulation;
        }

        calculateFitness(population, request, targetCounts);
        population.sort(Comparator.comparingDouble(Chromosome::getFitness).reversed());

        Chromosome best = population.get(0).clone();
        repair(best, questionsByType, targetCounts);
        return best.getQuestions();
    }

    private List<Chromosome> initializePopulation(int size, Map<Integer, Integer> targetCounts, Map<Integer, List<Question>> pool) {
        validateTypeAvailability(targetCounts, pool);

        List<Chromosome> population = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            population.add(new Chromosome(buildChromosomeGenes(targetCounts, pool, null, null)));
        }
        return population;
    }

    private Map<Integer, Integer> resolveTargetCounts(AutoGenerateRequest request, Map<Integer, List<Question>> pool) {
        Map<Integer, Integer> minimumTypeCounts = normalizeTypeCounts(request);
        int minimumTotal = minimumTypeCounts.values().stream().mapToInt(Integer::intValue).sum();
        int requestedTotal = request.getQuestionCount() != null && request.getQuestionCount() > 0
                ? request.getQuestionCount()
                : minimumTotal;

        if (requestedTotal <= 0) {
            throw new RuntimeException("总题数必须大于 0");
        }

        if (minimumTotal > requestedTotal) {
            throw new RuntimeException("各题型最低数量之和超过了总题数，无法组卷");
        }

        int totalAvailable = pool.values().stream().mapToInt(List::size).sum();
        if (totalAvailable < requestedTotal) {
            throw new RuntimeException("题库可用题目不足，无法凑满 " + requestedTotal + " 道题");
        }

        Map<Integer, Integer> targetCounts = new LinkedHashMap<>(minimumTypeCounts);
        int remaining = requestedTotal - minimumTotal;
        if (remaining == 0) {
            return targetCounts;
        }

        List<Integer> typeOrder = buildTypeOrder(request, pool);
        if (typeOrder.isEmpty()) {
            throw new RuntimeException("未找到可用题型，无法组卷");
        }

        while (remaining > 0) {
            boolean allocated = false;
            for (Integer type : typeOrder) {
                int current = targetCounts.getOrDefault(type, 0);
                int available = pool.getOrDefault(type, Collections.emptyList()).size();
                if (available <= current) {
                    continue;
                }
                targetCounts.put(type, current + 1);
                remaining--;
                allocated = true;
                if (remaining == 0) {
                    break;
                }
            }

            if (!allocated) {
                throw new RuntimeException("题库可用题目不足，无法补齐到 " + requestedTotal + " 道题");
            }
        }

        return targetCounts;
    }

    private List<Integer> buildTypeOrder(AutoGenerateRequest request, Map<Integer, List<Question>> pool) {
        List<Integer> typeOrder = new ArrayList<>();
        Set<Integer> addedTypes = new HashSet<>();

        if (request.getTypeConfigs() != null) {
            for (AutoGenerateRequest.TypeConfig config : request.getTypeConfigs()) {
                if (config == null || config.getType() == null) {
                    continue;
                }
                if (addedTypes.add(config.getType())) {
                    typeOrder.add(config.getType());
                }
            }
        }

        for (Integer type : pool.keySet()) {
            if (addedTypes.add(type)) {
                typeOrder.add(type);
            }
        }

        return typeOrder;
    }

    private Map<Integer, Integer> normalizeTypeCounts(AutoGenerateRequest request) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        if (request.getTypeConfigs() == null) {
            return counts;
        }

        for (AutoGenerateRequest.TypeConfig config : request.getTypeConfigs()) {
            if (config == null || config.getType() == null || config.getCount() == null || config.getCount() <= 0) {
                continue;
            }
            counts.merge(config.getType(), config.getCount(), Integer::sum);
        }
        return counts;
    }

    private void validateKnowledgeCoverage(AutoGenerateRequest request, List<Question> pool) {
        if (request.getKnowledgePointRequirements() == null || request.getKnowledgePointRequirements().isEmpty()) {
            return;
        }

        Map<String, Long> knowledgePointCounts = pool.stream()
                .filter(Objects::nonNull)
                .filter(question -> question.getCategory() != null)
                .collect(Collectors.groupingBy(Question::getCategory, Collectors.counting()));

        for (Map.Entry<String, Integer> entry : request.getKnowledgePointRequirements().entrySet()) {
            long actualCount = knowledgePointCounts.getOrDefault(entry.getKey(), 0L);
            if (actualCount < entry.getValue()) {
                throw new RuntimeException("题库中[" + entry.getKey() + "]类的题目数量不足 (需要 " + entry.getValue() + ", 实际 " + actualCount + ")");
            }
        }
    }

    private void validateTypeAvailability(Map<Integer, Integer> targetCounts, Map<Integer, List<Question>> pool) {
        for (Map.Entry<Integer, Integer> entry : targetCounts.entrySet()) {
            int actualCount = pool.getOrDefault(entry.getKey(), Collections.emptyList()).size();
            if (actualCount < entry.getValue()) {
                throw new RuntimeException("题库中" + getTypeName(entry.getKey()) + "的题目数量不足 (需要 " + entry.getValue() + ", 实际 " + actualCount + ")");
            }
        }
    }

    private void calculateFitness(List<Chromosome> population, AutoGenerateRequest request, Map<Integer, Integer> targetCounts) {
        AutoGenerateRequest.Weights weights = request.getWeights() != null ? request.getWeights() : new AutoGenerateRequest.Weights();

        double difficultyWeight = weights.getDifficultyWeight() != null ? weights.getDifficultyWeight() : 0.25;
        double coverageWeight = weights.getCoverageWeight() != null ? weights.getCoverageWeight() : 0.25;
        double typeRatioWeight = weights.getTypeRatioWeight() != null ? weights.getTypeRatioWeight() : 0.25;
        double scoreWeight = weights.getScoreWeight() != null ? weights.getScoreWeight() : 0.25;

        for (Chromosome chromosome : population) {
            double difficultyError = calculateDifficultyDeviation(chromosome, request);
            double coverageError = calculateKnowledgeCoverage(chromosome, request);
            double typeRatioError = calculateTypeRatioError(chromosome, targetCounts);
            double scoreError = calculateScoreError(chromosome, request);
            double penalty = calculatePenalty(chromosome, request, targetCounts);

            double weightedError =
                    difficultyWeight * difficultyError +
                    coverageWeight * coverageError +
                    typeRatioWeight * typeRatioError +
                    scoreWeight * scoreError +
                    penalty;

            chromosome.setFitness(100.0 / (1.0 + weightedError));
            chromosome.setError(weightedError);
        }
    }

    private double calculateDifficultyDeviation(Chromosome chromosome, AutoGenerateRequest request) {
        if (request.getDifficulty() == null) {
            return 0.0;
        }
        double avgDifficulty = chromosome.getQuestions().stream()
                .map(Question::getDifficulty)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        return Math.abs(avgDifficulty - request.getDifficulty());
    }

    private double calculateKnowledgeCoverage(Chromosome chromosome, AutoGenerateRequest request) {
        if (request.getKnowledgePointRequirements() == null || request.getKnowledgePointRequirements().isEmpty()) {
            return 0.0;
        }

        Map<String, Long> actualCounts = chromosome.getQuestions().stream()
                .filter(question -> question.getCategory() != null)
                .collect(Collectors.groupingBy(Question::getCategory, Collectors.counting()));

        double error = 0.0;
        for (Map.Entry<String, Integer> entry : request.getKnowledgePointRequirements().entrySet()) {
            long actual = actualCounts.getOrDefault(entry.getKey(), 0L);
            if (actual < entry.getValue()) {
                error += (entry.getValue() - actual);
            }
        }
        return error;
    }

    private double calculateTypeRatioError(Chromosome chromosome, Map<Integer, Integer> targetCounts) {
        Map<Integer, Long> currentCounts = chromosome.getQuestions().stream()
                .collect(Collectors.groupingBy(Question::getType, Collectors.counting()));

        double error = 0.0;
        for (Map.Entry<Integer, Integer> entry : targetCounts.entrySet()) {
            long actual = currentCounts.getOrDefault(entry.getKey(), 0L);
            error += Math.abs(actual - entry.getValue());
        }
        return error;
    }

    private double calculateScoreError(Chromosome chromosome, AutoGenerateRequest request) {
        if (request.getTotalScore() == null || request.getTypeConfigs() == null || request.getTypeConfigs().isEmpty()) {
            return 0.0;
        }

        Map<Integer, Integer> typeScores = request.getTypeConfigs().stream()
                .filter(Objects::nonNull)
                .filter(config -> config.getType() != null && config.getScore() != null)
                .collect(Collectors.toMap(
                        AutoGenerateRequest.TypeConfig::getType,
                        AutoGenerateRequest.TypeConfig::getScore,
                        (first, second) -> second
                ));

        if (typeScores.isEmpty()) {
            return 0.0;
        }

        int totalScore = 0;
        for (Question question : chromosome.getQuestions()) {
            totalScore += typeScores.getOrDefault(question.getType(), 0);
        }
        return Math.abs(totalScore - request.getTotalScore());
    }

    private double calculatePenalty(Chromosome chromosome, AutoGenerateRequest request, Map<Integer, Integer> targetCounts) {
        double penalty = 0.0;

        Set<Long> uniqueIds = new HashSet<>();
        int duplicates = 0;
        for (Question question : chromosome.getQuestions()) {
            if (!uniqueIds.add(question.getId())) {
                duplicates++;
            }
        }
        penalty += duplicates * 100;

        int targetQuestionCount = targetCounts.values().stream().mapToInt(Integer::intValue).sum();
        penalty += Math.abs(chromosome.getQuestions().size() - targetQuestionCount) * 100;

        Map<Integer, Long> currentTypeCounts = chromosome.getQuestions().stream()
                .collect(Collectors.groupingBy(Question::getType, Collectors.counting()));
        for (Map.Entry<Integer, Integer> entry : targetCounts.entrySet()) {
            long actual = currentTypeCounts.getOrDefault(entry.getKey(), 0L);
            if (actual < entry.getValue()) {
                penalty += (entry.getValue() - actual) * 100;
            }
        }

        if (request.getConstraints() != null && request.getConstraints().getMandatoryQuestionIds() != null) {
            for (Long mandatoryId : request.getConstraints().getMandatoryQuestionIds()) {
                boolean found = chromosome.getQuestions().stream().anyMatch(question -> question.getId().equals(mandatoryId));
                if (!found) {
                    penalty += 50;
                }
            }
        }

        return penalty;
    }

    private Chromosome tournamentSelection(List<Chromosome> population) {
        int tournamentSize = 3;
        Chromosome best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Chromosome randomChromosome = population.get(RANDOM.nextInt(population.size()));
            if (best == null || randomChromosome.getFitness() > best.getFitness()) {
                best = randomChromosome;
            }
        }
        return best;
    }

    private Chromosome crossover(Chromosome parent1, Chromosome parent2, Map<Integer, Integer> targetCounts, Map<Integer, List<Question>> pool) {
        return new Chromosome(buildChromosomeGenes(targetCounts, pool, parent1.getQuestions(), parent2.getQuestions()));
    }

    private void mutate(Chromosome chromosome, Map<Integer, List<Question>> pool) {
        List<Question> genes = chromosome.getQuestions();
        if (genes.isEmpty()) {
            return;
        }

        int index = RANDOM.nextInt(genes.size());
        Question original = genes.get(index);
        List<Question> candidates = new ArrayList<>(pool.getOrDefault(original.getType(), Collections.emptyList()));
        Collections.shuffle(candidates);
        for (Question replacement : candidates) {
            if (!Objects.equals(replacement.getId(), original.getId())) {
                genes.set(index, replacement);
                return;
            }
        }
    }

    private void repair(Chromosome chromosome, Map<Integer, List<Question>> pool, Map<Integer, Integer> targetCounts) {
        chromosome.setQuestions(buildChromosomeGenes(targetCounts, pool, chromosome.getQuestions(), null));
    }

    private List<Question> buildChromosomeGenes(
            Map<Integer, Integer> targetCounts,
            Map<Integer, List<Question>> pool,
            List<Question> primarySource,
            List<Question> secondarySource
    ) {
        List<Question> genes = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>();

        for (Map.Entry<Integer, Integer> entry : targetCounts.entrySet()) {
            Integer type = entry.getKey();
            int targetCount = entry.getValue();
            if (targetCount <= 0) {
                continue;
            }

            List<Question> candidates = new ArrayList<>();
            appendQuestionsByType(candidates, primarySource, type);
            appendQuestionsByType(candidates, secondarySource, type);
            Collections.shuffle(candidates);

            int added = appendUniqueQuestions(genes, candidates, targetCount, usedIds);
            if (added < targetCount) {
                List<Question> poolCandidates = new ArrayList<>(pool.getOrDefault(type, Collections.emptyList()));
                Collections.shuffle(poolCandidates);
                added += appendUniqueQuestions(genes, poolCandidates, targetCount - added, usedIds);
            }

            if (added < targetCount) {
                throw new RuntimeException("题库中" + getTypeName(type) + "的题目数量不足，无法满足组卷要求");
            }
        }

        return genes;
    }

    private void appendQuestionsByType(List<Question> target, List<Question> source, Integer type) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Question question : source) {
            if (question != null && Objects.equals(question.getType(), type)) {
                target.add(question);
            }
        }
    }

    private int appendUniqueQuestions(List<Question> target, List<Question> candidates, int limit, Set<Long> usedIds) {
        int added = 0;
        for (Question candidate : candidates) {
            if (added >= limit) {
                break;
            }
            if (candidate == null || candidate.getId() == null || usedIds.contains(candidate.getId())) {
                continue;
            }
            target.add(candidate);
            usedIds.add(candidate.getId());
            added++;
        }
        return added;
    }

    private String getTypeName(Integer type) {
        return TYPE_NAMES.getOrDefault(type, "题型" + type);
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
