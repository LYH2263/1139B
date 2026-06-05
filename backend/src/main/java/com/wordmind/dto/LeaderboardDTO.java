package com.wordmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class LeaderboardDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        private Integer rank;
        private Long userId;
        private String username;
        private Integer value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionResponse {
        private String dimension;
        private List<Entry> topList;
        private Entry currentUser;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private DimensionResponse masteredWords;
        private DimensionResponse quizScore;
        private DimensionResponse streakDays;
    }

    public enum Dimension {
        MASTERED_WORDS("masteredWords"),
        QUIZ_SCORE("quizScore"),
        STREAK_DAYS("streakDays");

        private final String value;

        Dimension(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Dimension fromValue(String value) {
            for (Dimension d : values()) {
                if (d.value.equalsIgnoreCase(value)) {
                    return d;
                }
            }
            throw new IllegalArgumentException("Invalid dimension: " + value);
        }
    }
}
