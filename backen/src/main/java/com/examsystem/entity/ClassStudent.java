package com.examsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("class_student")
public class ClassStudent {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long classId;
    private Long studentId;
    private Integer status; // 0: 待审核, 1: 已加入, 2: 已拒绝
    private LocalDateTime applyTime;
    private LocalDateTime approveTime;
    private Long approveBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}