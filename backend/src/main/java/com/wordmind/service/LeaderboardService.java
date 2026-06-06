package com.wordmind.service;

import com.wordmind.dto.LeaderboardDTO;
import com.wordmind.entity.User;
import com.wordmind.repository.QuizRecordRepository;
import com.wordmind.repository.ReviewRecordRepository;
import com.wordmind.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    @Autowired
    private ReviewRecordRepository reviewRecordRepository;

    @Autowired
    private QuizRecordRepository quizRecordRepository;

    @Autowired
    private UserRepository userRepository;

    private static final int TOP_LIMIT = 20;

    public LeaderboardDTO.Response getAllLeaderboards(Long currentUserId) {
        return LeaderboardDTO.Response.builder()
                .masteredWords(getMasteredWordsLeaderboard(currentUserId))
                .quizScore(getQuizScoreLeaderboard(currentUserId))
                .streakDays(getStreakDaysLeaderboard(currentUserId))
                .build();
    }

    public LeaderboardDTO.DimensionResponse getLeaderboardByDimension(String dimension, Long currentUserId) {
        LeaderboardDTO.Dimension dim = LeaderboardDTO.Dimension.fromValue(dimension);
        switch (dim) {
            case MASTERED_WORDS:
                return getMasteredWordsLeaderboard(currentUserId);
            case QUIZ_SCORE:
                return getQuizScoreLeaderboard(currentUserId);
            case STREAK_DAYS:
                return getStreakDaysLeaderboard(currentUserId);
            default:
                throw new IllegalArgumentException("Invalid dimension: " + dimension);
        }
    }

    private LeaderboardDTO.DimensionResponse getMasteredWordsLeaderboard(Long currentUserId) {
        List<Object[]> rankingData = reviewRecordRepository.findMasteredWordsRanking();
        List<LeaderboardDTO.Entry> entries = buildEntriesWithRank(rankingData, 0, 1);
        List<LeaderboardDTO.Entry> topList = entries.stream().limit(TOP_LIMIT).collect(Collectors.toList());
        LeaderboardDTO.Entry currentUserEntry = findCurrentUserEntry(entries, currentUserId);

        return LeaderboardDTO.DimensionResponse.builder()
                .dimension(LeaderboardDTO.Dimension.MASTERED_WORDS.getValue())
                .topList(topList)
                .currentUser(currentUserEntry)
                .build();
    }

    private LeaderboardDTO.DimensionResponse getQuizScoreLeaderboard(Long currentUserId) {
        List<Object[]> rankingData = quizRecordRepository.findMaxScoreRanking();
        List<LeaderboardDTO.Entry> entries = buildEntriesWithRank(rankingData, 0, 1);
        List<LeaderboardDTO.Entry> topList = entries.stream().limit(TOP_LIMIT).collect(Collectors.toList());
        LeaderboardDTO.Entry currentUserEntry = findCurrentUserEntry(entries, currentUserId);

        return LeaderboardDTO.DimensionResponse.builder()
                .dimension(LeaderboardDTO.Dimension.QUIZ_SCORE.getValue())
                .topList(topList)
                .currentUser(currentUserEntry)
                .build();
    }

    private LeaderboardDTO.DimensionResponse getStreakDaysLeaderboard(Long currentUserId) {
        List<Long> userIds = reviewRecordRepository.findAllUserIdsWithRecords();
        List<Map.Entry<Long, Integer>> streakList = new ArrayList<>();

        for (Long userId : userIds) {
            int streak = calculateMaxStreakDays(userId);
            if (streak > 0) {
                streakList.add(new AbstractMap.SimpleEntry<>(userId, streak));
            }
        }

        streakList.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<LeaderboardDTO.Entry> entries = buildStreakEntriesWithRank(streakList);
        List<LeaderboardDTO.Entry> topList = entries.stream().limit(TOP_LIMIT).collect(Collectors.toList());
        LeaderboardDTO.Entry currentUserEntry = findCurrentUserEntry(entries, currentUserId);

        return LeaderboardDTO.DimensionResponse.builder()
                .dimension(LeaderboardDTO.Dimension.STREAK_DAYS.getValue())
                .topList(topList)
                .currentUser(currentUserEntry)
                .build();
    }

    private List<LeaderboardDTO.Entry> buildEntriesWithRank(List<Object[]> rankingData, int userIdIndex, int valueIndex) {
        List<LeaderboardDTO.Entry> entries = new ArrayList<>();
        int rank = 1;
        int prevValue = -1;
        int sameRankCount = 0;

        for (int i = 0; i < rankingData.size(); i++) {
            Object[] row = rankingData.get(i);
            Long userId = ((Number) row[userIdIndex]).longValue();
            int value = ((Number) row[valueIndex]).intValue();

            if (value == prevValue) {
                sameRankCount++;
            } else {
                rank = i + 1;
                sameRankCount = 0;
                prevValue = value;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                entries.add(LeaderboardDTO.Entry.builder()
                        .rank(rank)
                        .userId(userId)
                        .username(user.getUsername())
                        .value(value)
                        .build());
            }
        }

        return entries;
    }

    private List<LeaderboardDTO.Entry> buildStreakEntriesWithRank(List<Map.Entry<Long, Integer>> streakList) {
        List<LeaderboardDTO.Entry> entries = new ArrayList<>();
        int rank = 1;
        int prevValue = -1;

        for (int i = 0; i < streakList.size(); i++) {
            Map.Entry<Long, Integer> entry = streakList.get(i);
            Long userId = entry.getKey();
            int value = entry.getValue();

            if (value != prevValue) {
                rank = i + 1;
                prevValue = value;
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                entries.add(LeaderboardDTO.Entry.builder()
                        .rank(rank)
                        .userId(userId)
                        .username(user.getUsername())
                        .value(value)
                        .build());
            }
        }

        return entries;
    }

    private LeaderboardDTO.Entry findCurrentUserEntry(List<LeaderboardDTO.Entry> entries, Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        return entries.stream()
                .filter(entry -> entry.getUserId().equals(currentUserId))
                .findFirst()
                .orElseGet(() -> {
                    User user = userRepository.findById(currentUserId).orElse(null);
                    if (user == null) {
                        return null;
                    }
                    return LeaderboardDTO.Entry.builder()
                            .rank(entries.size() + 1)
                            .userId(currentUserId)
                            .username(user.getUsername())
                            .value(0)
                            .build();
                });
    }

    public int calculateMaxStreakDays(Long userId) {
        List<Object> rawDates = reviewRecordRepository.findDistinctReviewDatesByUserId(userId);
        if (rawDates == null || rawDates.isEmpty()) {
            return 0;
        }

        List<LocalDate> reviewDates = new ArrayList<>();
        for (Object obj : rawDates) {
            LocalDate date = convertToLocalDate(obj);
            if (date != null) {
                reviewDates.add(date);
            }
        }

        if (reviewDates.isEmpty()) {
            return 0;
        }

        Collections.sort(reviewDates);

        int maxStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < reviewDates.size(); i++) {
            LocalDate prev = reviewDates.get(i - 1);
            LocalDate curr = reviewDates.get(i);

            if (prev.plusDays(1).equals(curr)) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else if (!prev.equals(curr)) {
                currentStreak = 1;
            }
        }

        return maxStreak;
    }

    private LocalDate convertToLocalDate(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof LocalDate) {
            return (LocalDate) obj;
        }
        if (obj instanceof java.sql.Date) {
            return ((java.sql.Date) obj).toLocalDate();
        }
        if (obj instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) obj).toLocalDateTime().toLocalDate();
        }
        if (obj instanceof java.util.Date) {
            return ((java.util.Date) obj).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }
}
