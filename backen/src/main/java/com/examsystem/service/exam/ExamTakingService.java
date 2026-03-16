package com.examsystem.service.exam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.examsystem.common.BizException;
import com.examsystem.controller.dto.ExamResultResponse;
import com.examsystem.entity.Class;
import com.examsystem.entity.ClassStudent;
import com.examsystem.entity.Exam;
import com.examsystem.entity.ExamAnswer;
import com.examsystem.entity.ExamClass;
import com.examsystem.entity.ExamQuestionRelation;
import com.examsystem.entity.ExamRecord;
import com.examsystem.entity.Image;
import com.examsystem.entity.Question;
import com.examsystem.entity.SysUser;
import com.examsystem.entity.WrongBook;
import com.examsystem.mapper.ClassMapper;
import com.examsystem.mapper.ClassStudentMapper;
import com.examsystem.mapper.ExamAnswerMapper;
import com.examsystem.mapper.ExamClassMapper;
import com.examsystem.mapper.ExamMapper;
import com.examsystem.mapper.ExamQuestionRelationMapper;
import com.examsystem.mapper.ExamRecordMapper;
import com.examsystem.mapper.ImageMapper;
import com.examsystem.mapper.QuestionMapper;
import com.examsystem.mapper.SysUserMapper;
import com.examsystem.mapper.WrongBookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamTakingService {
    private final ExamMapper examMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final ExamQuestionRelationMapper relationMapper;
    private final QuestionMapper questionMapper;
    private final WrongBookMapper wrongBookMapper;
    private final ImageMapper imageMapper;
    private final ExamClassMapper examClassMapper;
    private final ClassStudentMapper classStudentMapper;
    private final SysUserMapper sysUserMapper;
    private final ClassMapper classMapper;

    public Exam getExam(Long examId) {
        return examMapper.selectById(examId);
    }

    public ExamResultResponse getExamResult(Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) throw new BizException(4004, "记录不存在");
        if (record.getStatus() == 0) throw new BizException(4003, "考试未完成，无法查看结果");

        Exam exam = examMapper.selectById(record.getExamId());

        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId));
        Map<Long, ExamAnswer> ansMap = answers.stream().collect(Collectors.toMap(ExamAnswer::getQuestionId, Function.identity()));

        List<ExamQuestionRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<ExamQuestionRelation>()
                .eq(ExamQuestionRelation::getExamId, record.getExamId()));
        Map<Long, Integer> scoreMap = relations.stream()
                .collect(Collectors.toMap(ExamQuestionRelation::getQuestionId, ExamQuestionRelation::getScore));
        Map<Long, String> categoryMap = relations.stream()
                .collect(Collectors.toMap(ExamQuestionRelation::getQuestionId, ExamQuestionRelation::getCategory, (a, b) -> a));
        int totalScore = relations.stream().mapToInt(ExamQuestionRelation::getScore).sum();

        List<Question> questions = getExamQuestions(record.getExamId());

        List<ExamResultResponse.QuestionResult> qResults = questions.stream().map(q -> {
            ExamResultResponse.QuestionResult qr = new ExamResultResponse.QuestionResult();
            qr.setId(q.getId());
            qr.setContent(q.getContent());
            qr.setContentImageUrl(q.getContentImageUrl());
            qr.setType(q.getType());
            qr.setScore(scoreMap.getOrDefault(q.getId(), 0));
            qr.setOptions(q.getOptions());
            qr.setAnswer(q.getAnswer());
            qr.setAnalysis(q.getAnalysis());
            qr.setCategory(categoryMap.getOrDefault(q.getId(), q.getCategory()));
            qr.setStemId(q.getStemId());

            ExamAnswer a = ansMap.get(q.getId());
            if (a != null) {
                qr.setStudentAnswer(a.getStudentAnswer());
                qr.setIsCorrect(a.getIsCorrect());
            }
            return qr;
        }).collect(Collectors.toList());

        Map<String, ExamResultResponse.CategoryStat> categoryStatMap = new HashMap<>();
        for (ExamResultResponse.QuestionResult qr : qResults) {
            String cat = qr.getCategory();
            if (cat == null || cat.isEmpty()) cat = "未分类";

            ExamResultResponse.CategoryStat stat = categoryStatMap.computeIfAbsent(cat, k -> {
                ExamResultResponse.CategoryStat s = new ExamResultResponse.CategoryStat();
                s.setCategory(k);
                s.setTotalCount(0);
                s.setCorrectCount(0);
                s.setScore(0);
                return s;
            });

            stat.setTotalCount(stat.getTotalCount() + 1);
            stat.setScore(stat.getScore() + (qr.getScore() != null ? qr.getScore() : 0));
            if (qr.getIsCorrect() != null && qr.getIsCorrect() == 1) {
                stat.setCorrectCount(stat.getCorrectCount() + 1);
            }
        }

        List<ExamResultResponse.CategoryStat> categoryStats = new ArrayList<>(categoryStatMap.values());
        for (ExamResultResponse.CategoryStat stat : categoryStats) {
            if (stat.getTotalCount() > 0) {
                stat.setAccuracyRate(Math.round(stat.getCorrectCount() * 100.0 / stat.getTotalCount()) / 100.0);
            }
        }

        ExamResultResponse res = new ExamResultResponse();
        res.setRecord(record);
        res.setExam(exam);
        res.setTotalScore(totalScore);
        res.setQuestions(qResults);
        res.setCategoryStats(categoryStats);
        res.setRanking(buildRankingInfo(record));
        return res;
    }

    private ExamResultResponse.RankingInfo buildRankingInfo(ExamRecord currentRecord) {
        List<ExamClass> examClasses = examClassMapper.selectList(
                new LambdaQueryWrapper<ExamClass>().eq(ExamClass::getExamId, currentRecord.getExamId())
        );
        if (examClasses.isEmpty()) {
            return null;
        }

        List<Long> examClassIds = examClasses.stream()
                .map(ExamClass::getClassId)
                .collect(Collectors.toList());

        List<ClassStudent> joinedClasses = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getStudentId, currentRecord.getStudentId())
                        .eq(ClassStudent::getStatus, 1)
                        .in(ClassStudent::getClassId, examClassIds)
                        .orderByAsc(ClassStudent::getClassId)
        );
        if (joinedClasses.isEmpty()) {
            return null;
        }

        Long classId = joinedClasses.get(0).getClassId();
        List<ClassStudent> classStudents = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, classId)
                        .eq(ClassStudent::getStatus, 1)
        );
        if (classStudents.isEmpty()) {
            return null;
        }

        List<Long> studentIds = classStudents.stream()
                .map(ClassStudent::getStudentId)
                .collect(Collectors.toList());

        List<ExamRecord> submittedRecords = examRecordMapper.selectList(
                new LambdaQueryWrapper<ExamRecord>()
                        .eq(ExamRecord::getExamId, currentRecord.getExamId())
                        .eq(ExamRecord::getStatus, 1)
                        .in(ExamRecord::getStudentId, studentIds)
        );
        if (submittedRecords.isEmpty()) {
            return null;
        }

        submittedRecords.sort(
                Comparator.comparing(ExamRecord::getScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ExamRecord::getSubmitTime, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ExamRecord::getStudentId)
        );

        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));

        ExamResultResponse.RankingInfo rankingInfo = new ExamResultResponse.RankingInfo();
        rankingInfo.setClassId(classId);
        Class clazz = classMapper.selectById(classId);
        rankingInfo.setClassName(clazz != null ? clazz.getName() : "班级");
        rankingInfo.setParticipantCount(submittedRecords.size());
        rankingInfo.setClassStudentCount(submittedRecords.size());

        List<ExamResultResponse.RankingItem> leaderboard = new ArrayList<>();
        for (int i = 0; i < submittedRecords.size(); i++) {
            ExamRecord record = submittedRecords.get(i);
            SysUser user = userMap.get(record.getStudentId());

            ExamResultResponse.RankingItem item = new ExamResultResponse.RankingItem();
            item.setRank(i + 1);
            item.setStudentId(record.getStudentId());
            item.setStudentName(user != null && user.getRealName() != null ? user.getRealName() : String.valueOf(record.getStudentId()));
            item.setScore(record.getScore());
            item.setSubmitTime(record.getSubmitTime() != null ? record.getSubmitTime().toString() : null);
            item.setCurrentStudent(record.getStudentId().equals(currentRecord.getStudentId()));
            leaderboard.add(item);

            if (record.getId().equals(currentRecord.getId())) {
                rankingInfo.setMyRank(i + 1);
            }
        }

        rankingInfo.setLeaderboard(leaderboard);
        return rankingInfo;
    }

    @Transactional
    public void updateRecordStatus(Long recordId, Integer status) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record != null) {
            record.setStatus(status);
            if (status == 2) {
                record.setSubmitTime(LocalDateTime.now());
            }
            examRecordMapper.updateById(record);
        }
    }

    public ExamRecord getRecord(Long examId, Long studentId) {
        return examRecordMapper.selectOne(new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStudentId, studentId)
                .last("limit 1"));
    }

    @Transactional
    public ExamRecord startExam(Long examId, Long studentId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new BizException(4001, "考试不存在");

        ExamRecord record = getRecord(examId, studentId);
        if (record != null) {
            if (record.getStatus() == 1) {
                LocalDateTime now = LocalDateTime.now();
                record.setStartTime(now);
                record.setStatus(0);
                record.setSubmitTime(null);
                record.setScore(null);
                examRecordMapper.updateById(record);

                List<ExamAnswer> answers = examAnswerMapper.selectList(
                        new LambdaQueryWrapper<ExamAnswer>()
                                .eq(ExamAnswer::getRecordId, record.getId())
                );
                for (ExamAnswer answer : answers) {
                    answer.setIsCorrect(null);
                    examAnswerMapper.updateById(answer);
                }
            }

            long duration = exam.getDuration() * 60L;
            long elapsed = java.time.Duration.between(record.getStartTime(), LocalDateTime.now()).getSeconds();
            record.setRemainingSeconds(Math.max(0, duration - elapsed));
            return record;
        }

        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new BizException(4002, "考试未开始");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BizException(4003, "考试已结束");
        }

        record = new ExamRecord();
        record.setExamId(examId);
        record.setStudentId(studentId);
        record.setStatus(0);
        record.setStartTime(now);
        examRecordMapper.insert(record);

        record.setRemainingSeconds((long) exam.getDuration() * 60L);
        return record;
    }

    public List<Question> getExamQuestions(Long examId) {
        List<ExamQuestionRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<ExamQuestionRelation>()
                .eq(ExamQuestionRelation::getExamId, examId)
                .orderByAsc(ExamQuestionRelation::getSortOrder));
        if (relations.isEmpty()) return new ArrayList<>();

        List<Long> qIds = relations.stream().map(ExamQuestionRelation::getQuestionId).collect(Collectors.toList());
        Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        LinkedHashMap<String, List<ExamQuestionRelation>> relationsByCategory = new LinkedHashMap<>();
        for (ExamQuestionRelation relation : relations) {
            Question question = qMap.get(relation.getQuestionId());
            if (question == null) {
                continue;
            }
            String category = resolveQuestionCategory(relation, question);
            relationsByCategory.computeIfAbsent(category, key -> new ArrayList<>()).add(relation);
        }

        List<Question> orderedQuestions = new ArrayList<>();
        Set<Long> processedStemIds = new HashSet<>();
        for (List<ExamQuestionRelation> categoryRelations : relationsByCategory.values()) {
            for (ExamQuestionRelation relation : categoryRelations) {
                Question question = qMap.get(relation.getQuestionId());
                if (question == null) {
                    continue;
                }

                question.setCategory(resolveQuestionCategory(relation, question));
                if (question.getStemId() != null) {
                    if (processedStemIds.contains(question.getStemId())) {
                        continue;
                    }

                    for (ExamQuestionRelation stemRelation : categoryRelations) {
                        Question stemQuestion = qMap.get(stemRelation.getQuestionId());
                        if (stemQuestion != null && question.getStemId().equals(stemQuestion.getStemId())) {
                            stemQuestion.setCategory(resolveQuestionCategory(stemRelation, stemQuestion));
                            orderedQuestions.add(stemQuestion);
                        }
                    }
                    processedStemIds.add(question.getStemId());
                } else {
                    orderedQuestions.add(question);
                }
            }
        }

        return orderedQuestions;
    }

    private String resolveQuestionCategory(ExamQuestionRelation relation, Question question) {
        if (relation.getCategory() != null && !relation.getCategory().isEmpty()) {
            return relation.getCategory();
        }
        if (question.getCategory() != null && !question.getCategory().isEmpty()) {
            return question.getCategory();
        }
        return "未分类";
    }

    @Transactional
    public void submitAnswer(Long recordId, Long questionId, String answer, Integer isMarked) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null || record.getStatus() != 0) {
            throw new BizException(4004, "考试已结束或记录不存在");
        }

        ExamAnswer ea = examAnswerMapper.selectOne(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId)
                .eq(ExamAnswer::getQuestionId, questionId));

        if (ea == null) {
            ea = new ExamAnswer();
            ea.setRecordId(recordId);
            ea.setQuestionId(questionId);
            ea.setStudentAnswer(answer);
            ea.setIsMarked(isMarked);
            examAnswerMapper.insert(ea);
        } else {
            ea.setStudentAnswer(answer);
            ea.setIsMarked(isMarked);
            examAnswerMapper.updateById(ea);
        }
    }

    @Transactional
    public void submitExam(Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) throw new BizException(4004, "记录不存在");
        if (record.getStatus() == 1) return;

        record.setStatus(1);
        record.setSubmitTime(LocalDateTime.now());

        List<ExamAnswer> answers = examAnswerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId));

        List<ExamQuestionRelation> relations = relationMapper.selectList(new LambdaQueryWrapper<ExamQuestionRelation>()
                .eq(ExamQuestionRelation::getExamId, record.getExamId()));
        Map<Long, Integer> scoreMap = relations.stream()
                .collect(Collectors.toMap(ExamQuestionRelation::getQuestionId, ExamQuestionRelation::getScore));

        int totalScore = 0;
        List<Long> qIds = answers.stream().map(ExamAnswer::getQuestionId).collect(Collectors.toList());
        if (!qIds.isEmpty()) {
            Map<Long, Question> qMap = questionMapper.selectBatchIds(qIds).stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity()));

            for (ExamAnswer ans : answers) {
                Question q = qMap.get(ans.getQuestionId());
                if (q != null && ans.getStudentAnswer() != null && ans.getStudentAnswer().equalsIgnoreCase(q.getAnswer())) {
                    ans.setIsCorrect(1);
                    totalScore += scoreMap.getOrDefault(q.getId(), 0);
                } else if (q != null) {
                    ans.setIsCorrect(0);
                    saveWrongQuestion(record.getStudentId(), q.getId(), record.getExamId());
                }
                examAnswerMapper.updateById(ans);
            }
        }

        record.setScore(totalScore);
        examRecordMapper.updateById(record);
    }

    @Transactional
    public void saveCapture(MultipartFile image, Long recordId) throws IOException {
        String originalFilename = image.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf('.')) : ".png";
        String filename = "capture_" + System.currentTimeMillis() + extension;

        String projectPath = System.getProperty("user.dir");
        String uploadPath = projectPath + File.separator + "uploads" + File.separator + "captures" + File.separator;
        File directory = new File(uploadPath);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                throw new IOException("Failed to create directory: " + uploadPath);
            }
        }
        File dest = new File(uploadPath + filename);
        image.transferTo(dest);

        Image img = new Image();
        img.setName(filename);
        img.setPath("/uploads/captures/" + filename);
        img.setSize(image.getSize());
        img.setType(image.getContentType());
        img.setExamRecordId(recordId);
        img.setCreatedTime(LocalDateTime.now());
        imageMapper.insert(img);
    }

    public List<Image> getCaptures(Long recordId) {
        return imageMapper.selectList(new LambdaQueryWrapper<Image>()
                .eq(Image::getExamRecordId, recordId)
                .orderByAsc(Image::getCreatedTime));
    }

    private void saveWrongQuestion(Long studentId, Long questionId, Long examId) {
        WrongBook wb = wrongBookMapper.selectOne(new LambdaQueryWrapper<WrongBook>()
                .eq(WrongBook::getStudentId, studentId)
                .eq(WrongBook::getQuestionId, questionId));

        if (wb == null) {
            wb = new WrongBook();
            wb.setStudentId(studentId);
            wb.setQuestionId(questionId);
            wb.setExamId(examId);
            wb.setWrongCount(1);
            wb.setPracticeCount(0);
            wb.setPracticeCorrectCount(0);
            wb.setCreateTime(LocalDateTime.now());
            wb.setUpdateTime(LocalDateTime.now());
            wrongBookMapper.insert(wb);
        } else {
            wb.setWrongCount(wb.getWrongCount() + 1);
            wb.setExamId(examId);
            wb.setUpdateTime(LocalDateTime.now());
            wrongBookMapper.updateById(wb);
        }
    }
}
