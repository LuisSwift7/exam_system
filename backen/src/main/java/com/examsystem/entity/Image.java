package com.examsystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("image")
public class Image {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    private String path;
    private String type;
    private Long size;
    private Long examRecordId;
    
    private LocalDateTime createdTime;
}
