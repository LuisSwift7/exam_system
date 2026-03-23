package com.examsystem.controller.teacher;

import com.examsystem.common.ApiResponse;
import com.examsystem.entity.Question;
import com.examsystem.service.question.QuestionService;
import com.examsystem.service.tool.PdfImportService;
import com.examsystem.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/question")
@RequiredArgsConstructor
public class QuestionImportController {

    private final PdfImportService pdfImportService;
    private final QuestionService questionService;

    @PostMapping("/import/pdf/parse")
    public ApiResponse<List<Question>> parsePdf(@RequestParam("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(pdfImportService.parsePdf(file));
    }

    @PostMapping("/import/batch")
    public ApiResponse<Void> batchSave(@AuthenticationPrincipal UserPrincipal user, @RequestBody List<Question> questions) {
        Long userId = user.getUserId();
        for (Question q : questions) {
            q.setCreateBy(userId);
            questionService.addQuestion(q);
        }
        return ApiResponse.ok();
    }
}
