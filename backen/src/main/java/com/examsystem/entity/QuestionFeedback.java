package com.examsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("question_feedback")
public class QuestionFeedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long questionId;
    private Long examId;
    private String content;
    private String replyContent;
    private Integer status; // 0: Pending, 1: Handled
    private LocalDateTime createTime;
    private LocalDateTime replyTime;

    @TableField(exist = false)
    private Question question;
}
