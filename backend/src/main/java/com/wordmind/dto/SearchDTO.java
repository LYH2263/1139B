package com.wordmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class SearchDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchSuggestion {
        private Long id;
        private String word;
        private String meaning;
        private String matchType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchHistoryItem {
        private Long id;
        private String keyword;
        private LocalDateTime searchedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HotKeyword {
        private String keyword;
        private Long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResponse {
        private java.util.List<WordDTO.Response> list;
        private Long total;
        private Integer page;
        private Integer size;
    }
}
