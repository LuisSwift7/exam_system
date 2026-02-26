package com.examsystem.service.classroom;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.examsystem.entity.Class;
import com.examsystem.entity.ClassStudent;

import java.util.List;
import java.util.Map;

public interface ClassService {
    // 班级管理
    IPage<Class> getTeacherClasses(int page, int size, Long teacherId);
    Class createClass(Class clazz);
    void updateClass(Class clazz);
    void deleteClass(Long id);
    Class getClassById(Long id);
    Class getClassByCode(String code);

    // 学生管理
    IPage<Map<String, Object>> getClassStudents(int page, int size, Long classId);
    void addStudent(Long classId, Long studentId);
    void removeStudent(Long classId, Long studentId);

    // 申请管理
    void applyToClass(String code, Long studentId);
    IPage<Map<String, Object>> getClassApplications(int page, int size, Long classId);
    void approveApplication(Long id, Integer status);

    // 学生班级
    List<Class> getStudentClasses(Long studentId);
}
