package com.wordmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public class AssociationDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull(message = "单词ID不能为空")
        private Long wordId;

        @NotBlank(message = "联想类型不能为空")
        @Size(max = 50, message = "联想类型不能超过50个字符")
        private String type;

        @NotBlank(message = "联想内容不能为空")
        @Size(max = 1000, message = "联想内容不能超过1000个字符")
        private String content;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long wordId;
        private String type;
        private String content;
        private Integer upvotes;
        private String createdBy;
        private Boolean isSystemGenerated;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<Response> list;
        private Long total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpvoteResponse {
        private Long id;
        private Integer upvotes;
    }
}
