package com.examsystem.controller.student;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.examsystem.common.ApiResponse;
import com.examsystem.controller.dto.StudentExamResponse;
import com.examsystem.entity.Exam;
import com.examsystem.entity.ExamRecord;
import com.examsystem.security.UserPrincipal;
import com.examsystem.service.exam.ExamService;
import com.examsystem.service.exam.ExamTakingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student/exams")
@RequiredArgsConstructor
public class StudentExamController {
    private final ExamService examService;
    private final ExamTakingService takingService;

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        IPage<Exam> result = examService.getAvailableExams(page, size);
        
        List<StudentExamResponse> list = result.getRecords().stream().map(e -> {
            StudentExamResponse res = new StudentExamResponse();
            BeanUtils.copyProperties(e, res);
            
            ExamRecord record = takingService.getRecord(e.getId(), user.getUserId());
            if (record != null) {
                // Check if exam has ended
                LocalDateTime now = LocalDateTime.now();
                if (e.getEndTime() != null && now.isAfter(e.getEndTime())) {
                    // Exam has ended, update record status if still in progress
                    if (record.getStatus() == 0) {
                        record.setStatus(2); // Set to ended
                        takingService.updateRecordStatus(record.getId(), 2);
                    }
                    res.setStudentStatus(2);
                } else {
                    res.setStudentStatus(record.getStatus());
                }
                res.setRecordId(record.getId());
            } else {
                // No record, check if exam has ended
                LocalDateTime now = LocalDateTime.now();
                if (e.getEndTime() != null && now.isAfter(e.getEndTime())) {
                    res.setStudentStatus(2); // Exam ended
                } else if (e.getStartTime() != null && now.isBefore(e.getStartTime())) {
                    res.setStudentStatus(-1); // Not started
                } else {
                    res.setStudentStatus(-1); // Not taken yet
                }
            }
            return res;
        }).collect(Collectors.toList());

        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("list", list);
        return ApiResponse.ok(map);
    }
}
