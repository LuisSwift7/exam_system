package com.examsystem.mapper;

import com.examsystem.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {
    void insert(Notification notification);
    List<Notification> findByUserId(@Param("userId") Long userId);
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);
    void markAsRead(@Param("id") Long id);
    void markAllAsRead(@Param("userId") Long userId);
    int countUnreadByUserId(@Param("userId") Long userId);
}