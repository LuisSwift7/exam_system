package com.examsystem.service;

import com.examsystem.entity.Notification;
import java.util.List;

public interface NotificationService {
    void createNotification(Long userId, String type, String title, String content, Long relatedId);
    List<Notification> getNotificationsByUserId(Long userId);
    List<Notification> getUnreadNotificationsByUserId(Long userId);
    void markAsRead(Long id);
    void markAllAsRead(Long userId);
    int countUnreadByUserId(Long userId);
    void createExamPublishedNotification(Long examId, List<Long> studentIds);
    void createClassApplyNotification(Long classId, Long studentId, Long teacherId);
    void createExamGradedNotification(Long examId, Long studentId);
    void createClassApprovedNotification(Long classId, Long studentId);
}