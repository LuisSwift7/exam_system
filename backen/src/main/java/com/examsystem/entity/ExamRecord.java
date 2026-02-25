package com.examsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_record")
public class ExamRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long examId;
    private Long studentId;
    private Integer status;
    private Integer score;
    private LocalDateTime startTime;
    private LocalDateTime submitTime;
    private LocalDateTime createdTime;

    @TableField(exist = false)
    private Long remainingSeconds;
}
