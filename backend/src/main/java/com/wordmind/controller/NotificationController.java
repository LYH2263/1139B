package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.NotificationDTO;
import com.wordmind.security.JwtTokenProvider;
import com.wordmind.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<NotificationDTO.PageResponse> getNotifications(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean read) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(notificationService.getNotifications(userId, page, size, read));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<NotificationDTO.Response>> getRecentNotifications(
            HttpServletRequest request,
            @RequestParam(defaultValue = "5") int limit) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(notificationService.getRecentNotifications(userId, limit));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<NotificationDTO.UnreadCountResponse> getUnreadCount(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        long count = notificationService.getUnreadCount(userId);
        return ApiResponse.success(NotificationDTO.UnreadCountResponse.builder().count(count).build());
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> markAsRead(
            HttpServletRequest request,
            @PathVariable Long id) {
        Long userId = getUserIdFromRequest(request);
        notificationService.markAsRead(userId, id);
        return ApiResponse.success();
    }

    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        notificationService.markAllAsRead(userId);
        return ApiResponse.success();
    }

    @PutMapping("/read-selected")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> markSelectedAsRead(
            HttpServletRequest request,
            @Valid @RequestBody NotificationDTO.MarkReadRequest markReadRequest) {
        Long userId = getUserIdFromRequest(request);
        notificationService.markSelectedAsRead(userId, markReadRequest.getIds());
        return ApiResponse.success();
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return tokenProvider.getUserIdFromToken(token);
    }
}
