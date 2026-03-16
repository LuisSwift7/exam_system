package com.examsystem.controller;

import com.examsystem.common.ApiResponse;
import com.examsystem.entity.Notification;
import com.examsystem.service.NotificationService;
import com.examsystem.security.UserPrincipal;
import com.examsystem.mapper.NotificationMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Resource
    private NotificationService notificationService;
    
    @Resource
    private NotificationMapper notificationMapper;

    @GetMapping
    public ApiResponse<List<Notification>> getNotifications(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        List<Notification> notifications = notificationService.getNotificationsByUserId(userPrincipal.getUserId());
        return ApiResponse.ok(notifications);
    }

    @GetMapping("/{id}")
    public ApiResponse<Notification> getNotificationDetail(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Notification notification = notificationService.getNotificationDetail(userPrincipal.getUserId(), id);
        return ApiResponse.ok(notification);
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

    @PostMapping("/create")
    public ApiResponse<Void> createNotification(@RequestBody NotificationRequest request) {
        notificationService.createBulkNotification(request.getUserIds(), request.getType(), request.getTitle(), request.getContent(), request.getRelatedId());
        return ApiResponse.ok();
    }
    
    // 通知请求DTO
    static class NotificationRequest {
        private List<Long> userIds;
        private String type;
        private String title;
        private String content;
        private Long relatedId;
        
        // Getters and setters
        public List<Long> getUserIds() {
            return userIds;
        }
        public void setUserIds(List<Long> userIds) {
            this.userIds = userIds;
        }
        public String getType() {
            return type;
        }
        public void setType(String type) {
            this.type = type;
        }
        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }
        public String getContent() {
            return content;
        }
        public void setContent(String content) {
            this.content = content;
        }
        public Long getRelatedId() {
            return relatedId;
        }
        public void setRelatedId(Long relatedId) {
            this.relatedId = relatedId;
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ApiResponse.ok();
    }

    @GetMapping("/all")
    public ApiResponse<Map<String, Object>> getAllNotifications(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        List<Notification> notifications = notificationService.getAllNotifications(page, size);
        int total = notificationMapper.countAll();
        Map<String, Object> result = new HashMap<>();
        result.put("list", notifications);
        result.put("total", total);
        return ApiResponse.ok(result);
    }
}
