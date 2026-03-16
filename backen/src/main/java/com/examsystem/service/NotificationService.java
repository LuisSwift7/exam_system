package com.examsystem.service;

import com.examsystem.entity.Notification;
import java.util.List;

public interface NotificationService {
    void createNotification(Long userId, String type, String title, String content, Long relatedId);
    List<Notification> getNotificationsByUserId(Long userId);
    Notification getNotificationDetail(Long userId, Long id);
    List<Notification> getUnreadNotificationsByUserId(Long userId);
    void markAsRead(Long id);
    void markAllAsRead(Long userId);
    int countUnreadByUserId(Long userId);
    void createExamPublishedNotification(Long examId, List<Long> studentIds);
    void createClassApplyNotification(Long classId, Long studentId, Long teacherId);
    void createExamGradedNotification(Long examId, Long studentId);
    void createClassApprovedNotification(Long classId, Long studentId);
    void createBulkNotification(List<Long> userIds, String type, String title, String content, Long relatedId);
    void deleteNotification(Long id);
    List<Notification> getAllNotifications();
    List<Notification> getAllNotifications(int page, int size);
}
