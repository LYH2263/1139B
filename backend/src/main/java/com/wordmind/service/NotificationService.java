package com.wordmind.service;

import com.wordmind.dto.NotificationDTO;
import com.wordmind.entity.Notification;
import com.wordmind.entity.User;
import com.wordmind.repository.NotificationRepository;
import com.wordmind.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    public NotificationDTO.PageResponse getNotifications(Long userId, Integer page, Integer size, Boolean read) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notificationPage;
        
        if (read != null) {
            notificationPage = notificationRepository.findByUserIdAndIsRead(userId, read, pageable);
        } else {
            notificationPage = notificationRepository.findByUserId(userId, pageable);
        }

        List<NotificationDTO.Response> list = notificationPage.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        long unreadCount = notificationRepository.countByUserIdAndIsRead(userId, false);

        return NotificationDTO.PageResponse.builder()
                .list(list)
                .total(notificationPage.getTotalElements())
                .unreadCount(unreadCount)
                .page(page)
                .size(size)
                .build();
    }

    public List<NotificationDTO.Response> getRecentNotifications(Long userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId, pageable).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Transactional
    public void markAsRead(Long userId, Long id) {
        int updated = notificationRepository.markAsRead(id, userId);
        if (updated == 0) {
            throw new RuntimeException("通知不存在或无权操作");
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void markSelectedAsRead(Long userId, List<Long> ids) {
        for (Long id : ids) {
            notificationRepository.markAsRead(id, userId);
        }
    }

    @Transactional
    public void sendNotification(Long userId, Notification.NotificationType type, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(false);
        notificationRepository.save(notification);
    }

    @Transactional
    public int broadcastSystemAnnouncement(String title, String content) {
        List<User> users = userRepository.findAll();
        int count = 0;
        for (User user : users) {
            Notification notification = new Notification();
            notification.setUserId(user.getId());
            notification.setType(Notification.NotificationType.SYSTEM_ANNOUNCEMENT);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setIsRead(false);
            notificationRepository.save(notification);
            count++;
        }
        return count;
    }

    private NotificationDTO.Response convertToDTO(Notification notification) {
        return NotificationDTO.Response.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .read(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
