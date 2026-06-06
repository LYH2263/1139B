package com.wordmind.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public class ScheduleDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "计划名称不能为空")
        private String name;

        @NotEmpty(message = "请选择要学习的单词")
        private List<Long> targetWordIds;

        @NotNull(message = "每日学习量不能为空")
        @Min(value = 1, message = "每日学习量至少为1")
        private Integer dailyCount;

        @NotNull(message = "开始日期不能为空")
        private LocalDate startDate;

        @NotNull(message = "结束日期不能为空")
        private LocalDate endDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "计划名称不能为空")
        private String name;

        @NotEmpty(message = "请选择要学习的单词")
        private List<Long> targetWordIds;

        @NotNull(message = "每日学习量不能为空")
        @Min(value = 1, message = "每日学习量至少为1")
        private Integer dailyCount;

        @NotNull(message = "开始日期不能为空")
        private LocalDate startDate;

        @NotNull(message = "结束日期不能为空")
        private LocalDate endDate;

        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompleteTodayRequest {
        private List<Long> completedWordIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleResponse {
        private Long id;
        private Long userId;
        private String name;
        private List<Long> targetWordIds;
        private Integer dailyCount;
        private LocalDate startDate;
        private LocalDate endDate;
        private String status;
        private String createdAt;
        private Integer totalDays;
        private Integer completedDays;
        private Integer totalWords;
        private Integer learnedWords;
        private Double progressPercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayResponse {
        private Long scheduleId;
        private String scheduleName;
        private LocalDate date;
        private List<ScheduleWordItem> plannedWords;
        private List<ScheduleWordItem> completedWords;
        private List<ScheduleWordItem> newWords;
        private List<ScheduleWordItem> reviewWords;
        private Integer totalCount;
        private Integer completedCount;
        private Boolean isCompleted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleWordItem {
        private Long wordId;
        private String word;
        private String phonetic;
        private String meaning;
        private String example;
        private String type;
        private Boolean isCompleted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressDetail {
        private LocalDate date;
        private List<Long> plannedWordIds;
        private List<Long> completedWordIds;
        private Integer plannedCount;
        private Integer completedCount;
        private Boolean isCompleted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleDetailResponse {
        private ScheduleResponse schedule;
        private List<ProgressDetail> progressList;
        private List<Long> ganttData;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private List<ScheduleResponse> list;
        private Long total;
    }
}
