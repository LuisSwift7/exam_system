package com.examsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wrong_book")
public class WrongBook {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long studentId;
    private Long questionId;
    private Long examId; // The exam where this mistake was first found or most recently found

    private Integer wrongCount; // Total times answered wrong in exams
    private Integer practiceCount; // Times practiced in wrong book
    
    // We can calculate accuracy dynamically or store it. Let's just store practice stats.
    // For simplicity, maybe just track how many times practiced. 
    // The requirement says "record practice times and correct rate". 
    // So maybe add "practiceCorrectCount".
    private Integer practiceCorrectCount;
    private String note;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
