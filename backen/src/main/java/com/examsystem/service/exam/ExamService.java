package com.examsystem.service.exam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examsystem.common.BizException;
import com.examsystem.entity.Exam;
import com.examsystem.entity.ExamClass;
import com.examsystem.entity.ExamQuestionRelation;
import com.examsystem.entity.Question;
import com.examsystem.entity.ClassStudent;
import com.examsystem.entity.ExamRecord;
import com.examsystem.entity.ExamAnswer;
import com.examsystem.entity.SysUser;
import com.examsystem.entity.Image;
import com.examsystem.mapper.ExamClassMapper;
import com.examsystem.mapper.ExamMapper;
import com.examsystem.mapper.ExamQuestionRelationMapper;
import com.examsystem.mapper.QuestionMapper;
import com.examsystem.mapper.ClassStudentMapper;
import com.examsystem.mapper.ExamRecordMapper;
import com.examsystem.mapper.ExamAnswerMapper;
import com.examsystem.mapper.SysUserMapper;
import com.examsystem.mapper.ImageMapper;
import com.examsystem.algorithm.GeneticAlgorithm;
import com.examsystem.dto.AutoGenerateRequest;
import java.util.HashMap;
import com.examsystem.service.NotificationService;
import java.util.Set;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class ExamService {
    private final ExamMapper examMapper;
    private final ExamQuestionRelationMapper relationMapper;
    private final QuestionMapper questionMapper;
    private final ExamClassMapper examClassMapper;
    private final ClassStudentMapper classStudentMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final SysUserMapper sysUserMapper;
    private final ImageMapper imageMapper;
    private final com.examsystem.service.classroom.ClassService classService;
    private final NotificationService notificationService;
    private final GeneticAlgorithm geneticAlgorithm;

    public IPage<Exam> getAvailableExams(int page, int size) {
        Page<Exam> p = new Page<>(page, size);
        return examMapper.selectPage(p, new LambdaQueryWrapper<Exam>()
                .eq(Exam::getStatus, 1)
                .orderByDesc(Exam::getStartTime));
    }

    public IPage<Exam> getStudentAvailableExams(int page, int size, Long studentId, List<Long> classIds) {
        Page<Exam> p = new Page<>(page, size);
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .eq(Exam::getStatus, 1);
        
        // 处理班级ID列表为空的情况
        if (classIds == null || classIds.isEmpty()) {
            // 学生没有加入任何班级，返回空结果
            wrapper.and(w -> w.eq(Exam::getId, -1)); // 确保返回空结果
        } else {
            // 学生加入了班级，通过exam_class表查询班级内的考试
            wrapper.inSql(Exam::getId, "SELECT exam_id FROM exam_class WHERE class_id IN (" + 
                classIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")");
        }
        
        return examMapper.selectPage(p, wrapper.orderByDesc(Exam::getStartTime));
    }

    public IPage<Exam> getExams(int page, int size, String keyword) {
        Page<Exam> p = new Page<>(page, size);
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<Exam>()
                .orderByDesc(Exam::getCreatedTime);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Exam::getTitle, keyword);
        }
        return examMapper.selectPage(p, wrapper);
    }

    public void createExam(Exam exam) {
        exam.setId(null);
        exam.setStatus(0); // Default Not Published
        exam.setCreatedTime(LocalDateTime.now());
        exam.setUpdatedTime(LocalDateTime.now());
        examMapper.insert(exam);
    }

    public void updateExam(Exam exam) {
        if (exam.getId() == null) throw new BizException(400, "ID不能为空");
        exam.setUpdatedTime(LocalDateTime.now());
        examMapper.updateById(exam);
    }

    public void deleteExam(Long id) {
        examMapper.deleteById(id);
        // Also delete relations
        relationMapper.delete(new LambdaQueryWrapper<ExamQuestionRelation>().eq(ExamQuestionRelation::getExamId, id));
    }

    public void publishExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new BizException(400, "试卷不存在");
        
        // Check if exam has questions
        Long count = relationMapper.selectCount(new LambdaQueryWrapper<ExamQuestionRelation>().eq(ExamQuestionRelation::getExamId, id));
        if (count == 0) {
            throw new BizException(400, "试卷未添加题目，无法发布");
        }
        
        exam.setStatus(1); // 1: Published
        examMapper.updateById(exam);
        
        // 发送考试发布通知
        List<Long> classIds = getExamClassIds(id);
        if (!classIds.isEmpty()) {
            List<Long> studentIds = new ArrayList<>();
            for (Long classId : classIds) {
                List<ClassStudent> classStudents = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getClassId, classId));
                for (ClassStudent cs : classStudents) {
                    studentIds.add(cs.getStudentId());
                }
            }
            if (!studentIds.isEmpty()) {
                // 获取考试名称
                String examName = exam.getTitle();
                // 获取班级名称
                StringBuilder classNames = new StringBuilder();
                for (Long classId : classIds) {
                    com.examsystem.entity.Class clazz = classService.getClassById(classId);
                    if (clazz != null) {
                        classNames.append(clazz.getName()).append("、");
                    }
                }
                if (classNames.length() > 0) {
                    classNames.setLength(classNames.length() - 1);
                }
                // 发送通知
                for (Long studentId : studentIds) {
                    notificationService.createNotification(
                            studentId,
                            "exam_published",
                            "考试发布通知",
                            classNames.toString() + "的" + examName + "已发布，请及时查看",
                            id
                    );
                }
            }
        }
    }

    @Transactional
    public void setExamClasses(Long examId, List<Long> classIds) {
        if (examId == null) throw new BizException(400, "试卷ID不能为空");
        
        // 删除旧的班级关联
        examClassMapper.delete(new LambdaQueryWrapper<ExamClass>().eq(ExamClass::getExamId, examId));
        
        // 添加新的班级关联
        if (classIds != null && !classIds.isEmpty()) {
            for (Long classId : classIds) {
                ExamClass examClass = new ExamClass();
                examClass.setExamId(examId);
                examClass.setClassId(classId);
                examClass.setCreatedTime(LocalDateTime.now());
                examClassMapper.insert(examClass);
            }
        }
    }

    public List<Long> getExamClassIds(Long examId) {
        return examClassMapper.selectList(
            new LambdaQueryWrapper<ExamClass>().eq(ExamClass::getExamId, examId)
        ).stream().map(ExamClass::getClassId).collect(Collectors.toList());
    }

    @Transactional
    public void manualCompose(Long examId, List<Long> questionIds, Integer score) {
        relationMapper.delete(new LambdaQueryWrapper<ExamQuestionRelation>().eq(ExamQuestionRelation::getExamId, examId));
        
        if (questionIds == null || questionIds.isEmpty()) return;
        
        List<Question> questions = questionMapper.selectBatchIds(questionIds);
        Map<Long, Question> qMap = questions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));
        
        int order = 1;
        for (Long qId : questionIds) {
            ExamQuestionRelation r = new ExamQuestionRelation();
            r.setExamId(examId);
            r.setQuestionId(qId);
            r.setScore(score != null ? score : 2);
            r.setSortOrder(order++);
            
            Question q = qMap.get(qId);
            if (q != null) {
                r.setCategory(q.getCategory());
            }
            
            relationMapper.insert(r);
        }
    }

    @Transactional
    public void autoCompose(Long examId, AutoComposeStrategy strategy) {
        relationMapper.delete(new LambdaQueryWrapper<ExamQuestionRelation>().eq(ExamQuestionRelation::getExamId, examId));

        if (strategy.getCategoryCounts() != null && !strategy.getCategoryCounts().isEmpty()) {
            int totalRequired = 0;
            for (CategoryCount cc : strategy.getCategoryCounts()) {
                totalRequired += cc.getCount();
            }
            
            int order = 1;
            for (CategoryCount cc : strategy.getCategoryCounts()) {
                LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Question::getCategory, cc.getCategory());
                if (strategy.getType() != null) {
                    wrapper.eq(Question::getType, strategy.getType());
                }
                
                List<Question> candidates = questionMapper.selectList(wrapper);
                if (candidates.size() < cc.getCount()) {
                    throw new BizException(400, "分类【" + cc.getCategory() + "】题库数量不足，需要" + cc.getCount() + "道，仅有" + candidates.size() + "道");
                }
                
                Collections.shuffle(candidates);
                List<Question> selected = candidates.subList(0, cc.getCount());
                
                for (Question q : selected) {
                    ExamQuestionRelation r = new ExamQuestionRelation();
                    r.setExamId(examId);
                    r.setQuestionId(q.getId());
                    r.setScore(cc.getScore() != null ? cc.getScore() : 2);
                    r.setSortOrder(order++);
                    r.setCategory(q.getCategory());
                    relationMapper.insert(r);
                }
            }
        } else {
            LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
            if (strategy.getType() != null) wrapper.eq(Question::getType, strategy.getType());
            if (strategy.getCategory() != null) wrapper.eq(Question::getCategory, strategy.getCategory());
            
            List<Question> candidates = questionMapper.selectList(wrapper);
            if (candidates.size() < strategy.getCount()) {
                throw new BizException(400, "题库数量不足，无法满足组卷要求");
            }

            Collections.shuffle(candidates);
            List<Question> selected = candidates.subList(0, strategy.getCount());

            int order = 1;
            for (Question q : selected) {
                ExamQuestionRelation r = new ExamQuestionRelation();
                r.setExamId(examId);
                r.setQuestionId(q.getId());
                r.setScore(strategy.getScore() != null ? strategy.getScore() : 2);
                r.setSortOrder(order++);
                r.setCategory(q.getCategory());
                relationMapper.insert(r);
            }
        }
    }

    @Transactional
    public void geneticAutoCompose(Long examId, AutoGenerateRequest request) {
        // 1. 清理旧题目
        relationMapper.delete(new LambdaQueryWrapper<ExamQuestionRelation>().eq(ExamQuestionRelation::getExamId, examId));

        // 2. 获取候选题目池
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        
        List<Integer> types = new ArrayList<>();
        if (request.getTypeConfigs() != null) {
            types = request.getTypeConfigs().stream().map(AutoGenerateRequest.TypeConfig::getType).collect(Collectors.toList());
        }
        
        if (!types.isEmpty()) {
            wrapper.in(Question::getType, types);
        }
        
        // 知识点筛选 (Category)
        if (request.getKnowledgePointRequirements() != null && !request.getKnowledgePointRequirements().isEmpty()) {
            wrapper.in(Question::getCategory, request.getKnowledgePointRequirements().keySet());
        }

        List<Question> candidatePool = questionMapper.selectList(wrapper);
        
        if (candidatePool.isEmpty()) {
            throw new BizException(400, "题库中没有符合条件的题目");
        }

        // 3. 执行遗传算法
        List<Question> selectedQuestions = geneticAlgorithm.generatePaper(candidatePool, request);

        // 4. 保存结果
        int order = 1;
        // 构建类型分值映射
        Map<Integer, Integer> typeScores = new HashMap<>();
        if (request.getTypeConfigs() != null) {
            for (AutoGenerateRequest.TypeConfig config : request.getTypeConfigs()) {
                typeScores.put(config.getType(), config.getScore());
            }
        }

        for (Question q : selectedQuestions) {
            ExamQuestionRelation r = new ExamQuestionRelation();
            r.setExamId(examId);
            r.setQuestionId(q.getId());
            r.setScore(typeScores.getOrDefault(q.getType(), 2)); // 默认2分
            r.setSortOrder(order++);
            r.setCategory(q.getCategory());
            relationMapper.insert(r);
        }
    }

    public List<Long> getExamQuestionIds(Long examId) {
        return relationMapper.selectQuestionIdsByExamId(examId);
    }

    public List<Map<String, Object>> getExamSubmissions(Long examId) {
        // 检查试卷是否存在
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BizException(400, "试卷不存在");
        }
        
        // 获取该试卷的所有考试记录
        List<ExamRecord> records = examRecordMapper.selectList(
            new LambdaQueryWrapper<ExamRecord>()
                .eq(ExamRecord::getExamId, examId)
                .eq(ExamRecord::getStatus, 1) // 只获取已提交的记录
                .orderByDesc(ExamRecord::getSubmitTime)
        );
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamRecord record : records) {
            Map<String, Object> submission = new HashMap<>();
            submission.put("recordId", record.getId());
            submission.put("studentId", record.getStudentId());
            submission.put("score", record.getScore());
            submission.put("submitTime", record.getSubmitTime());
            
            // 获取学生信息
            SysUser student = sysUserMapper.selectById(record.getStudentId());
            if (student != null) {
                submission.put("studentName", student.getRealName());
                submission.put("studentNumber", student.getUsername());
            }
            
            result.add(submission);
        }
        
        return result;
    }

    @Transactional
    public void withdrawSubmission(Long recordId) {
        // 检查记录是否存在
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException(400, "考试记录不存在");
        }
        
        // 检查记录是否已提交
        if (record.getStatus() != 1) {
            throw new BizException(400, "该记录尚未提交，无法撤回");
        }
        
        // 将记录状态改为未提交
        record.setStatus(0);
        record.setSubmitTime(null);
        record.setScore(null);
        record.setStartTime(null); // 重置开始时间，以便重新计算答题时间
        examRecordMapper.updateById(record);
        
        // 清除所有答案的正确性标记，因为考试状态已重置
        List<ExamAnswer> answers = examAnswerMapper.selectList(
            new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getRecordId, recordId)
        );
        for (ExamAnswer answer : answers) {
            answer.setIsCorrect(null);
            examAnswerMapper.updateById(answer);
        }
    }

    public List<Map<String, Object>> getStudentCaptures(Long recordId) {
        // 检查记录是否存在
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException(400, "考试记录不存在");
        }
        
        // 查询该考试记录的所有抓拍图片，按创建时间排序
        List<Image> images = imageMapper.selectList(new LambdaQueryWrapper<Image>()
                .eq(Image::getExamRecordId, recordId)
                .orderByAsc(Image::getCreatedTime));
        
        // 转换为包含base64编码的图片数据
        List<Map<String, Object>> result = new ArrayList<>();
        for (Image image : images) {
            Map<String, Object> imageData = new HashMap<>();
            imageData.put("id", image.getId());
            imageData.put("name", image.getName());
            imageData.put("path", image.getPath());
            imageData.put("type", image.getType());
            imageData.put("size", image.getSize());
            imageData.put("examRecordId", image.getExamRecordId());
            imageData.put("createdTime", image.getCreatedTime());
            
            try {
                String projectPath = System.getProperty("user.dir");
                String filePath = projectPath + image.getPath();
                File file = new File(filePath);
                if (file.exists()) {
                    byte[] fileContent = Files.readAllBytes(file.toPath());
                    String base64Image = "data:" + image.getType() + ";base64," + Base64.getEncoder().encodeToString(fileContent);
                    imageData.put("base64Image", base64Image);
                } else {
                    imageData.put("base64Image", null);
                }
            } catch (Exception e) {
                imageData.put("base64Image", null);
                e.printStackTrace();
            }
            
            result.add(imageData);
        }
        
        return result;
    }

    public List<Long> getExamStudentIds(Long examId) {
        // 获取考试关联的班级ID列表
        List<Long> classIds = getExamClassIds(examId);
        Set<Long> studentIds = new HashSet<>();
        
        // 遍历每个班级，获取学生ID
        for (Long classId : classIds) {
            List<ClassStudent> classStudents = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getClassId, classId)
            );
            for (ClassStudent cs : classStudents) {
                studentIds.add(cs.getStudentId());
            }
        }
        
        return new ArrayList<>(studentIds);
    }

    public ExamPreviewVo getExamPreview(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new BizException(400, "试卷不存在");

        List<ExamQuestionRelation> relations = relationMapper.selectList(
                new LambdaQueryWrapper<ExamQuestionRelation>()
                        .eq(ExamQuestionRelation::getExamId, id)
                        .orderByAsc(ExamQuestionRelation::getSortOrder)
        );

        if (relations.isEmpty()) {
            return new ExamPreviewVo(exam, Collections.emptyList());
        }

        List<Long> qIds = relations.stream().map(ExamQuestionRelation::getQuestionId).collect(Collectors.toList());
        List<Question> questions = questionMapper.selectBatchIds(qIds);
        Map<Long, Question> qMap = questions.stream().collect(Collectors.toMap(Question::getId, q -> q));

        List<QuestionPreviewVo> questionVos = new ArrayList<>();
        for (ExamQuestionRelation r : relations) {
            Question q = qMap.get(r.getQuestionId());
            if (q != null) {
                QuestionPreviewVo vo = new QuestionPreviewVo();
                BeanUtils.copyProperties(q, vo);
                vo.setScore(r.getScore());
                questionVos.add(vo);
            }
        }

        return new ExamPreviewVo(exam, questionVos);
    }

    @Data
    public static class AutoComposeStrategy {
        private Integer type;
        private Integer count;
        private Integer difficulty;
        private Integer score;
        private String category;
        private List<CategoryCount> categoryCounts;
    }

    @Data
    public static class CategoryCount {
        private String category;
        private Integer count;
        private Integer score;
    }

    @Data
    public static class ExamPreviewVo {
        private Exam exam;
        private List<QuestionPreviewVo> questions;

        public ExamPreviewVo(Exam exam, List<QuestionPreviewVo> questions) {
            this.exam = exam;
            this.questions = questions;
        }
    }

    @Data
    public static class QuestionPreviewVo extends Question {
        private Integer score;
    }
}
