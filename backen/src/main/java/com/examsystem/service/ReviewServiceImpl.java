package com.examsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.examsystem.common.BizException;
import com.examsystem.entity.Exam;
import com.examsystem.entity.ExamClass;
import com.examsystem.entity.ExamQuestionRelation;
import com.examsystem.entity.Question;
import com.examsystem.entity.Review;
import com.examsystem.mapper.ExamClassMapper;
import com.examsystem.mapper.ExamMapper;
import com.examsystem.mapper.ExamQuestionRelationMapper;
import com.examsystem.mapper.QuestionMapper;
import com.examsystem.mapper.ReviewMapper;
import com.examsystem.service.classroom.ClassService;
import com.examsystem.service.exam.ExamService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Resource
    private ReviewMapper reviewMapper;

    @Resource
    private NotificationService notificationService;

    @Resource
    private ExamService examService;

    @Resource
    private ExamMapper examMapper;

    @Resource
    private ExamClassMapper examClassMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private ExamQuestionRelationMapper relationMapper;

    @Resource
    private ClassService classService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void createReview(Review review) {
        review.setStatus(0);
        review.setContent(serializeContent(review));
        reviewMapper.insert(review);
    }

    @Override
    public void updateReview(Review review) {
        Review existing = reviewMapper.findById(review.getId());
        if (existing == null) {
            throw new BizException(404, "讲评不存在");
        }

        review.setTeacherId(existing.getTeacherId());
        review.setStatus(review.getStatus() == null ? existing.getStatus() : review.getStatus());
        review.setExamId(review.getExamId() == null ? existing.getExamId() : review.getExamId());
        review.setContent(serializeContent(review));
        reviewMapper.update(review);
    }

    @Override
    public void deleteReview(Long id) {
        reviewMapper.deleteById(id);
    }

    @Override
    public Review getReviewById(Long id) {
        return enrichReview(reviewMapper.findById(id));
    }

    @Override
    public Review getReviewByExamId(Long examId) {
        return enrichReview(reviewMapper.findByExamId(examId));
    }

    @Override
    public List<Review> getPublishedReviewsByStudentId(Long studentId) {
        List<Long> classIds = classService.getStudentClasses(studentId).stream()
                .map(com.examsystem.entity.Class::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(classIds)) {
            return Collections.emptyList();
        }

        LinkedHashSet<Long> examIds = examClassMapper.selectList(
                new LambdaQueryWrapper<ExamClass>().in(ExamClass::getClassId, classIds)
        ).stream()
                .map(ExamClass::getExamId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (examIds.isEmpty()) {
            return Collections.emptyList();
        }

        return reviewMapper.findAll().stream()
                .filter(review -> review.getStatus() != null && review.getStatus() == 1)
                .filter(review -> review.getExamId() != null && examIds.contains(review.getExamId()))
                .map(this::enrichReview)
                .collect(Collectors.toList());
    }

    @Override
    public List<Review> getReviewsByTeacherId(Long teacherId) {
        return reviewMapper.findByTeacherId(teacherId).stream()
                .map(this::enrichReview)
                .collect(Collectors.toList());
    }

    @Override
    public List<Review> getAllReviews() {
        return reviewMapper.findAll().stream()
                .map(this::enrichReview)
                .collect(Collectors.toList());
    }

    @Override
    public void publishReview(Long id) {
        Review review = reviewMapper.findById(id);
        if (review != null) {
            review.setStatus(1);
            reviewMapper.update(review);
            notifyStudents(review.getExamId(), review.getId());
        }
    }

    @Override
    public void notifyStudents(Long examId, Long reviewId) {
        List<Long> studentIds = examService.getExamStudentIds(examId);
        if (studentIds != null && !studentIds.isEmpty()) {
            notificationService.createBulkNotification(
                    studentIds,
                    "review_published",
                    "考试讲评已发布",
                    "您参加的考试已发布讲评，请及时查看。",
                    reviewId
            );
        }
    }

    private String serializeContent(Review review) {
        if (review.getExamId() == null) {
            throw new BizException(400, "请选择关联试卷");
        }

        List<Review.QuestionReviewItem> questionReviews = sanitizeQuestionReviews(review.getQuestionReviews());
        validateQuestionReviews(review.getExamId(), questionReviews);

        ReviewContentPayload payload = new ReviewContentPayload();
        payload.setSummary(StringUtils.trimWhitespace(review.getSummary()));
        payload.setQuestionReviews(questionReviews);

        boolean hasSummary = StringUtils.hasText(payload.getSummary());
        boolean hasQuestionReviews = !CollectionUtils.isEmpty(payload.getQuestionReviews());

        if (!hasSummary && !hasQuestionReviews) {
            if (StringUtils.hasText(review.getContent())) {
                payload.setSummary(review.getContent().trim());
            } else {
                throw new BizException(400, "请填写讲评内容");
            }
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BizException(500, "讲评内容保存失败");
        }
    }

    private Review enrichReview(Review review) {
        if (review == null) {
            return null;
        }

        Exam exam = review.getExamId() == null ? null : examMapper.selectById(review.getExamId());
        review.setExamTitle(exam == null ? null : exam.getTitle());

        ReviewContentPayload payload = parseContent(review.getContent());
        review.setSummary(StringUtils.hasText(payload.getSummary()) ? payload.getSummary() : review.getContent());
        review.setQuestionReviews(enrichQuestionReviews(review.getExamId(), payload.getQuestionReviews()));

        return review;
    }

    private ReviewContentPayload parseContent(String content) {
        ReviewContentPayload payload = new ReviewContentPayload();
        if (!StringUtils.hasText(content)) {
            payload.setSummary("");
            payload.setQuestionReviews(Collections.emptyList());
            return payload;
        }

        try {
            ReviewContentPayload parsed = objectMapper.readValue(content, ReviewContentPayload.class);
            parsed.setQuestionReviews(parsed.getQuestionReviews() == null ? Collections.emptyList() : parsed.getQuestionReviews());
            return parsed;
        } catch (Exception ignored) {
            payload.setSummary(content);
            payload.setQuestionReviews(Collections.emptyList());
            return payload;
        }
    }

    private List<Review.QuestionReviewItem> sanitizeQuestionReviews(List<Review.QuestionReviewItem> questionReviews) {
        if (CollectionUtils.isEmpty(questionReviews)) {
            return Collections.emptyList();
        }

        List<Review.QuestionReviewItem> sanitized = new ArrayList<>();
        LinkedHashSet<Long> seenQuestionIds = new LinkedHashSet<>();

        for (Review.QuestionReviewItem item : questionReviews) {
            if (item == null || item.getQuestionId() == null || !StringUtils.hasText(item.getContent())) {
                continue;
            }
            if (!seenQuestionIds.add(item.getQuestionId())) {
                continue;
            }

            Review.QuestionReviewItem sanitizedItem = new Review.QuestionReviewItem();
            sanitizedItem.setQuestionId(item.getQuestionId());
            sanitizedItem.setContent(item.getContent().trim());
            sanitized.add(sanitizedItem);
        }

        return sanitized;
    }

    private void validateQuestionReviews(Long examId, List<Review.QuestionReviewItem> questionReviews) {
        if (CollectionUtils.isEmpty(questionReviews)) {
            return;
        }

        List<Long> allowedQuestionIds = relationMapper.selectQuestionIdsByExamId(examId);
        for (Review.QuestionReviewItem item : questionReviews) {
            if (!allowedQuestionIds.contains(item.getQuestionId())) {
                throw new BizException(400, "讲评题目不属于当前试卷");
            }
        }
    }

    private List<Review.QuestionReviewItem> enrichQuestionReviews(Long examId, List<Review.QuestionReviewItem> questionReviews) {
        if (CollectionUtils.isEmpty(questionReviews) || examId == null) {
            return Collections.emptyList();
        }

        List<ExamQuestionRelation> relations = relationMapper.selectList(
                new LambdaQueryWrapper<ExamQuestionRelation>()
                        .eq(ExamQuestionRelation::getExamId, examId)
                        .orderByAsc(ExamQuestionRelation::getSortOrder)
        );

        Map<Long, Integer> questionNoMap = new LinkedHashMap<>();
        for (int i = 0; i < relations.size(); i++) {
            questionNoMap.put(relations.get(i).getQuestionId(), i + 1);
        }

        List<Long> questionIds = questionReviews.stream()
                .map(Review.QuestionReviewItem::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Question> questionMap = questionMapper.selectBatchIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, question -> question));

        List<Review.QuestionReviewItem> enriched = new ArrayList<>();
        for (Review.QuestionReviewItem item : questionReviews) {
            Review.QuestionReviewItem enrichedItem = new Review.QuestionReviewItem();
            enrichedItem.setQuestionId(item.getQuestionId());
            enrichedItem.setContent(item.getContent());
            enrichedItem.setQuestionNo(questionNoMap.get(item.getQuestionId()));
            Question question = questionMap.get(item.getQuestionId());
            enrichedItem.setQuestionContent(question == null ? null : question.getContent());
            enrichedItem.setQuestionContentImageUrl(question == null ? null : question.getContentImageUrl());
            enrichedItem.setQuestionOptions(question == null ? Collections.emptyList() : question.getOptions());
            enriched.add(enrichedItem);
        }

        return enriched;
    }

    @Data
    private static class ReviewContentPayload {
        private String summary;
        private List<Review.QuestionReviewItem> questionReviews = Collections.emptyList();
    }
}
