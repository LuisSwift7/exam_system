package com.examsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "question", autoResultMap = true)
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String content;
    private Integer type;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> options;

    private String answer;
    private String analysis;
    private Integer difficulty;
    private LocalDateTime createdTime;
    private Long createBy;
    private LocalDateTime updatedTime;
    private Long updateBy;
}
