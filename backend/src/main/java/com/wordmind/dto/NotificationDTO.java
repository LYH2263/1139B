package com.wordmind.dto;

import com.wordmind.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public class NotificationDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long userId;
        private Notification.NotificationType type;
        private String title;
        private String content;
        private Boolean read;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageResponse {
        private List<Response> list;
        private Long total;
        private Long unreadCount;
        private Integer page;
        private Integer size;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnreadCountResponse {
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BroadcastRequest {
        @NotBlank(message = "标题不能为空")
        @Size(max = 100, message = "标题长度不能超过100")
        private String title;

        @NotBlank(message = "内容不能为空")
        @Size(max = 500, message = "内容长度不能超过500")
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarkReadRequest {
        private List<Long> ids;
    }
}
