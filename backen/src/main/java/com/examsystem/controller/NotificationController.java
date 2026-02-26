package com.examsystem.controller;

import com.examsystem.common.ApiResponse;
import com.examsystem.entity.Notification;
import com.examsystem.service.NotificationService;
import com.examsystem.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Resource
    private NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<Notification>> getNotifications(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<Notification> notifications = notificationService.getNotificationsByUserId(userPrincipal.getUserId());
        return ApiResponse.ok(notifications);
    }

    @GetMapping("/unread")
    public ApiResponse<List<Notification>> getUnreadNotifications(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<Notification> notifications = notificationService.getUnreadNotificationsByUserId(userPrincipal.getUserId());
        return ApiResponse.ok(notifications);
    }

    @GetMapping("/unread/count")
    public ApiResponse<Integer> getUnreadCount(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        int count = notificationService.countUnreadByUserId(userPrincipal.getUserId());
        return ApiResponse.ok(count);
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponse.ok();
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        notificationService.markAllAsRead(userPrincipal.getUserId());
        return ApiResponse.ok();
    }
}