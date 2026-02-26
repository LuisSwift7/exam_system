package com.examsystem.service.classroom;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.examsystem.entity.Class;
import com.examsystem.entity.ClassStudent;
import com.examsystem.entity.SysUser;
import com.examsystem.mapper.ClassMapper;
import com.examsystem.mapper.ClassStudentMapper;
import com.examsystem.mapper.SysUserMapper;
import com.examsystem.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassServiceImpl implements ClassService {

    private final ClassMapper classMapper;
    private final ClassStudentMapper classStudentMapper;
    private final SysUserMapper sysUserMapper;
    private final NotificationService notificationService;

    @Override
    public IPage<Class> getTeacherClasses(int page, int size, Long teacherId) {
        Page<Class> p = new Page<>(page, size);
        return classMapper.selectPage(p, new LambdaQueryWrapper<Class>()
                .eq(Class::getTeacherId, teacherId)
                .orderByDesc(Class::getCreatedTime));
    }

    @Override
    @Transactional
    public Class createClass(Class clazz) {
        // 生成班级代码
        String code = generateClassCode();
        clazz.setCode(code);
        clazz.setStatus(1);
        clazz.setCreatedTime(LocalDateTime.now());
        clazz.setUpdatedTime(LocalDateTime.now());
        classMapper.insert(clazz);
        return clazz;
    }

    @Override
    @Transactional
    public void updateClass(Class clazz) {
        clazz.setUpdatedTime(LocalDateTime.now());
        classMapper.updateById(clazz);
    }

    @Override
    @Transactional
    public void deleteClass(Long id) {
        // 删除班级
        classMapper.deleteById(id);
        // 删除班级学生关联
        classStudentMapper.delete(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, id));
    }

    @Override
    public Class getClassById(Long id) {
        return classMapper.selectById(id);
    }

    @Override
    public Class getClassByCode(String code) {
        return classMapper.selectOne(new LambdaQueryWrapper<Class>()
                .eq(Class::getCode, code)
                .eq(Class::getStatus, 1));
    }

    @Override
    public IPage<Map<String, Object>> getClassStudents(int page, int size, Long classId) {
        Page<ClassStudent> p = new Page<>(page, size);
        IPage<ClassStudent> result = classStudentMapper.selectPage(p, new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStatus, 1)
                .orderByDesc(ClassStudent::getCreatedTime));
        
        // 转换为包含学生信息的Map
        return result.convert(cs -> {
            Map<String, Object> map = new HashMap<>();
            map.put("studentId", cs.getStudentId());
            
            // 查询学生信息
            SysUser student = sysUserMapper.selectById(cs.getStudentId());
            if (student != null) {
                map.put("studentName", student.getRealName());
                map.put("studentUsername", student.getUsername());
                map.put("studentRealName", student.getRealName());
            } else {
                map.put("studentName", "");
                map.put("studentUsername", "");
                map.put("studentRealName", "");
            }
            
            // 添加加入时间
            LocalDateTime joinTime = cs.getApproveTime() != null ? cs.getApproveTime() : cs.getCreatedTime();
            map.put("joinTime", joinTime != null ? joinTime.toString().replace("T", " ") : null);
            
            return map;
        });
    }

    @Override
    @Transactional
    public void addStudent(Long classId, Long studentId) {
        // 检查是否已存在
        ClassStudent existing = classStudentMapper.selectOne(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStudentId, studentId));

        if (existing == null) {
            ClassStudent cs = new ClassStudent();
            cs.setClassId(classId);
            cs.setStudentId(studentId);
            cs.setStatus(1); // 直接通过
            cs.setApplyTime(LocalDateTime.now());
            cs.setApproveTime(LocalDateTime.now());
            cs.setCreatedTime(LocalDateTime.now());
            cs.setUpdatedTime(LocalDateTime.now());
            classStudentMapper.insert(cs);
        }
    }

    @Override
    @Transactional
    public void removeStudent(Long classId, Long studentId) {
        classStudentMapper.delete(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStudentId, studentId));
    }

    @Override
    @Transactional
    public void applyToClass(String code, Long studentId) {
        Class clazz = getClassByCode(code);
        if (clazz == null) {
            throw new RuntimeException("班级不存在");
        }

        // 检查是否已申请
        ClassStudent existing = classStudentMapper.selectOne(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, clazz.getId())
                .eq(ClassStudent::getStudentId, studentId));

        if (existing == null) {
            ClassStudent cs = new ClassStudent();
            cs.setClassId(clazz.getId());
            cs.setStudentId(studentId);
            cs.setStatus(0); // 待审核
            cs.setApplyTime(LocalDateTime.now());
            cs.setCreatedTime(LocalDateTime.now());
            cs.setUpdatedTime(LocalDateTime.now());
            classStudentMapper.insert(cs);
            
            // 发送班级申请通知给教师
            notificationService.createClassApplyNotification(clazz.getId(), studentId, clazz.getTeacherId());
        }
    }

    @Override
    public IPage<Map<String, Object>> getClassApplications(int page, int size, Long classId) {
        Page<ClassStudent> p = new Page<>(page, size);
        IPage<ClassStudent> result = classStudentMapper.selectPage(p, new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStatus, 0)
                .orderByDesc(ClassStudent::getApplyTime));
        
        return result.convert(cs -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", cs.getId());
            map.put("studentId", cs.getStudentId());
            map.put("classId", cs.getClassId());
            map.put("status", cs.getStatus());
            map.put("applyTime", cs.getApplyTime() != null ? cs.getApplyTime().toString().replace("T", " ") : null);
            
            SysUser student = sysUserMapper.selectById(cs.getStudentId());
            if (student != null) {
                map.put("studentName", student.getRealName());
                map.put("studentUsername", student.getUsername());
                map.put("studentRealName", student.getRealName());
            } else {
                map.put("studentName", "");
                map.put("studentUsername", "");
                map.put("studentRealName", "");
            }
            
            return map;
        });
    }

    @Override
    @Transactional
    public void approveApplication(Long id, Integer status) {
        ClassStudent cs = classStudentMapper.selectById(id);
        if (cs != null) {
            cs.setStatus(status);
            cs.setApproveTime(LocalDateTime.now());
            cs.setUpdatedTime(LocalDateTime.now());
            classStudentMapper.updateById(cs);
            
            // 发送班级申请批准通知给学生
            if (status == 1) { // 1: 批准
                notificationService.createClassApprovedNotification(cs.getClassId(), cs.getStudentId());
            }
        }
    }

    @Override
    public List<Class> getStudentClasses(Long studentId) {
        List<ClassStudent> csList = classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getStudentId, studentId)
                .eq(ClassStudent::getStatus, 1));

        return csList.stream()
                .map(cs -> classMapper.selectById(cs.getClassId()))
                .filter(clazz -> clazz != null && clazz.getStatus() == 1)
                .toList();
    }

    private String generateClassCode() {
        String code = "CLS" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // 检查是否已存在
        while (classMapper.selectOne(new LambdaQueryWrapper<Class>().eq(Class::getCode, code)) != null) {
            code = "CLS" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        return code;
    }
}
