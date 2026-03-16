package com.examsystem.service;

import com.examsystem.entity.Notification;
import com.examsystem.mapper.NotificationMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Resource
    private NotificationMapper notificationMapper;

    @Override
    public void createNotification(Long userId, String type, String title, String content, Long relatedId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setIsRead(false);
        notificationMapper.insert(notification);
    }

    @Override
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationMapper.findByUserId(userId);
    }

    @Override
    public Notification getNotificationDetail(Long userId, Long id) {
        return notificationMapper.findByIdAndUserId(id, userId);
    }

    @Override
    public List<Notification> getUnreadNotificationsByUserId(Long userId) {
        return notificationMapper.findUnreadByUserId(userId);
    }

    @Override
    public void markAsRead(Long id) {
        notificationMapper.markAsRead(id);
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
    }

    @Override
    public int countUnreadByUserId(Long userId) {
        return notificationMapper.countUnreadByUserId(userId);
    }

    @Override
    public void createExamPublishedNotification(Long examId, List<Long> studentIds) {
        // 这里需要获取考试信息和班级信息，由于没有注入相关服务，暂时使用占位符
        // 实际实现时需要注入ExamService和ClassService来获取详细信息
        String examName = "考试" + examId;
        String className = "班级";
        
        for (Long studentId : studentIds) {
            createNotification(
                    studentId,
                    "exam_published",
                    "考试发布通知",
                    className + "的" + examName + "已发布，请及时查看",
                    examId
            );
        }
    }

    @Override
    public void createClassApplyNotification(Long classId, Long studentId, Long teacherId) {
        createNotification(
                teacherId,
                "class_apply",
                "班级申请通知",
                "有学生申请加入班级，请审核",
                classId
        );
    }

    @Override
    public void createExamGradedNotification(Long examId, Long studentId) {
        createNotification(
                studentId,
                "exam_graded",
                "考试批改完成",
                "您的考试已批改完成，请查看成绩",
                examId
        );
    }

    @Override
    public void createClassApprovedNotification(Long classId, Long studentId) {
        createNotification(
                studentId,
                "class_approved",
                "班级申请批准",
                "您的班级申请已批准",
                classId
        );
    }

    @Override
    public void createBulkNotification(List<Long> userIds, String type, String title, String content, Long relatedId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        
        // 去重，确保每个学生只收到一条通知
        Set<Long> uniqueUserIds = new HashSet<>(userIds);
        
        for (Long userId : uniqueUserIds) {
            if (userId != null) {
                createNotification(userId, type, title, content, relatedId);
            }
        }
    }

    @Override
    public void deleteNotification(Long id) {
        notificationMapper.deleteById(id);
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notificationMapper.findAll();
    }

    @Override
    public List<Notification> getAllNotifications(int page, int size) {
        int offset = (page - 1) * size;
        return notificationMapper.findAllWithPagination(offset, size);
    }
}
