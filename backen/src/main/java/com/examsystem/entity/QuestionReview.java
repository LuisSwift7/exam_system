package com.examsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question_review")
public class QuestionReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private Long teacherId;
    private String title;
    private String content;
    private Integer publishStatus; // 0: Draft, 1: Published
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private Question question;
}
