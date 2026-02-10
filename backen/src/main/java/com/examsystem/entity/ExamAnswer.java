package com.examsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_answer")
public class ExamAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;
    private Long questionId;
    private String studentAnswer;
    private Integer isCorrect;
    private Integer isMarked;
    private LocalDateTime createdTime;
}
