package com.wordmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class LevelDTO {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelResponse {
        private Long id;
        private String name;
        private String description;
        private String difficulty;
        private Integer passingScore;
        private Integer order;
        private Integer wordCount;
        private Boolean unlocked;
        private LevelProgressResponse progress;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelProgressResponse {
        private Long id;
        private Integer bestScore;
        private Integer stars;
        private Boolean completed;
        private Integer attempts;
        private LocalDateTime lastAttemptAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartResponse {
        private String sessionId;
        private List<QuizDTO.Question> questions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitRequest {
        @NotNull(message = "会话ID不能为空")
        private String sessionId;
        
        @NotNull(message = "答案不能为空")
        private List<QuizDTO.Answer> answers;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitResponse {
        private Long levelId;
        private Integer score;
        private Integer correctCount;
        private Integer totalCount;
        private Integer stars;
        private Boolean passed;
        private Boolean newlyCompleted;
        private Integer duration;
        private List<WordDTO.Response> wrongWords;
        private LevelProgressResponse progress;
    }
}
