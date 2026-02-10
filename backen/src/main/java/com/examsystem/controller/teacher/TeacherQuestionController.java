package com.examsystem.controller.teacher;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.examsystem.common.ApiResponse;
import com.examsystem.entity.Question;
import com.examsystem.service.question.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/questions")
@RequiredArgsConstructor
public class TeacherQuestionController {
    private final QuestionService questionService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type
    ) {
        IPage<Question> result = questionService.getQuestions(page, size, keyword, type);
        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("list", result.getRecords());
        return ApiResponse.ok(map);
    }

    @PostMapping
    public ApiResponse<Void> add(@RequestBody Question question) {
        questionService.addQuestion(question);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody Question question) {
        question.setId(id);
        questionService.updateQuestion(question);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ApiResponse.ok(null);
    }
}
