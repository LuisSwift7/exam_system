package com.examsystem.controller.dto;

import com.examsystem.entity.Exam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StudentExamResponse extends Exam {
    private Integer studentStatus; // -1: Not Started, 0: In Progress, 1: Completed
    private Long recordId;
}
