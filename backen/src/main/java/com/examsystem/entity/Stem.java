package com.examsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("stem")
public class Stem {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String content; // 题干内容
    private String contentImageUrl; // 题干图片URL
    private String category; // 分类，如资料分析
    private LocalDateTime createdTime;
    private Long createBy;
    private LocalDateTime updatedTime;
    private Long updateBy;
}
