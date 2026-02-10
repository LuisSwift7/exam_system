package com.examsystem.controller.student;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.examsystem.common.ApiResponse;
import com.examsystem.service.exam.WrongBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student/wrong-book")
@RequiredArgsConstructor
public class StudentWrongBookController {
    private final WrongBookService wrongBookService;

    @GetMapping
    public ApiResponse<IPage<WrongBookService.WrongBookVo>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer type) {
        return ApiResponse.ok(wrongBookService.getWrongQuestions(page, size, keyword, type));
    }

    @PostMapping("/{id}/practice")
    public ApiResponse<Void> practice(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean correct = body.get("correct");
        if (correct == null) correct = false;
        wrongBookService.practice(id, correct);
        return ApiResponse.ok(null);
    }

    @GetMapping("/stats")
    public ApiResponse<WrongBookService.WrongBookStats> getStats() {
        return ApiResponse.ok(wrongBookService.getStats());
    }
}
