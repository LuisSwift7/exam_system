package com.examsystem.mapper;

import com.examsystem.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {
    void insert(Review review);
    void update(Review review);
    Review findById(@Param("id") Long id);
    Review findByExamId(@Param("examId") Long examId);
    List<Review> findByTeacherId(@Param("teacherId") Long teacherId);
    List<Review> findAll();
}