package com.wordmind.service;

import com.wordmind.dto.ScheduleDTO;
import com.wordmind.entity.ScheduleProgress;
import com.wordmind.entity.StudySchedule;
import com.wordmind.entity.Word;
import com.wordmind.repository.ScheduleProgressRepository;
import com.wordmind.repository.StudyScheduleRepository;
import com.wordmind.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private static final int[] EBBINGHAUS_INTERVALS = {0, 1, 2, 4, 7, 15, 30};

    @Autowired
    private StudyScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleProgressRepository progressRepository;

    @Autowired
    private WordRepository wordRepository;

    public ScheduleDTO.ListResponse getSchedules(Long userId) {
        List<StudySchedule> schedules = scheduleRepository.findByUserId(userId);
        List<ScheduleDTO.ScheduleResponse> list = schedules.stream()
                .map(this::convertToScheduleResponse)
                .collect(Collectors.toList());
        return ScheduleDTO.ListResponse.builder()
                .list(list)
                .total((long) list.size())
                .build();
    }

    public ScheduleDTO.ScheduleResponse getSchedule(Long id, Long userId) {
        StudySchedule schedule = scheduleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("学习计划不存在"));
        return convertToScheduleResponse(schedule);
    }

    @Transactional
    public ScheduleDTO.ScheduleResponse createSchedule(Long userId, ScheduleDTO.CreateRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("结束日期不能早于开始日期");
        }

        StudySchedule schedule = new StudySchedule();
        schedule.setUserId(userId);
        schedule.setName(request.getName());
        schedule.setTargetWordIds(joinIds(request.getTargetWordIds()));
        schedule.setDailyCount(request.getDailyCount());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        schedule.setStatus(StudySchedule.ScheduleStatus.ACTIVE);

        StudySchedule saved = scheduleRepository.save(schedule);
        generateProgress(saved);

        return convertToScheduleResponse(saved);
    }

    @Transactional
    public ScheduleDTO.ScheduleResponse updateSchedule(Long id, Long userId, ScheduleDTO.UpdateRequest request) {
        StudySchedule schedule = scheduleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("学习计划不存在"));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException("结束日期不能早于开始日期");
        }

        schedule.setName(request.getName());
        schedule.setTargetWordIds(joinIds(request.getTargetWordIds()));
        schedule.setDailyCount(request.getDailyCount());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        if (request.getStatus() != null) {
            schedule.setStatus(StudySchedule.ScheduleStatus.valueOf(request.getStatus()));
        }

        progressRepository.deleteByScheduleId(schedule.getId());
        StudySchedule saved = scheduleRepository.save(schedule);
        generateProgress(saved);

        return convertToScheduleResponse(saved);
    }

    @Transactional
    public void deleteSchedule(Long id, Long userId) {
        if (!scheduleRepository.existsByIdAndUserId(id, userId)) {
            throw new RuntimeException("学习计划不存在");
        }
        progressRepository.deleteByScheduleId(id);
        scheduleRepository.deleteById(id);
    }

    public ScheduleDTO.TodayResponse getTodaySchedule(Long id, Long userId) {
        StudySchedule schedule = scheduleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("学习计划不存在"));

        LocalDate today = LocalDate.now();
        ScheduleProgress progress = progressRepository.findByScheduleIdAndDate(id, today)
                .orElseGet(() -> {
                    List<ScheduleProgress> allProgress = progressRepository.findByScheduleIdOrderByDateAsc(id);
                    for (ScheduleProgress p : allProgress) {
                        if (!p.getDate().isBefore(today)) {
                            return p;
                        }
                    }
                    return allProgress.isEmpty() ? null : allProgress.get(allProgress.size() - 1);
                });

        if (progress == null) {
            return ScheduleDTO.TodayResponse.builder()
                    .scheduleId(schedule.getId())
                    .scheduleName(schedule.getName())
                    .date(today)
                    .plannedWords(new ArrayList<>())
                    .completedWords(new ArrayList<>())
                    .newWords(new ArrayList<>())
                    .reviewWords(new ArrayList<>())
                    .totalCount(0)
                    .completedCount(0)
                    .isCompleted(true)
                    .build();
        }

        List<Long> plannedIds = parseIds(progress.getPlannedWordIds());
        List<Long> completedIds = parseIds(progress.getCompletedWordIds());
        Set<Long> completedSet = new HashSet<>(completedIds);

        Map<Long, Word> wordMap = getWordMap(plannedIds);

        List<Long> newWordIds = getNewWordIds(schedule, progress.getDate());
        Set<Long> newWordSet = new HashSet<>(newWordIds);

        List<ScheduleDTO.ScheduleWordItem> plannedWords = plannedIds.stream()
                .map(wordId -> buildWordItem(wordId, wordMap, newWordSet.contains(wordId) ? "NEW" : "REVIEW", completedSet.contains(wordId)))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<ScheduleDTO.ScheduleWordItem> completedWords = completedIds.stream()
                .map(wordId -> buildWordItem(wordId, wordMap, newWordSet.contains(wordId) ? "NEW" : "REVIEW", true))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<ScheduleDTO.ScheduleWordItem> newWords = plannedWords.stream()
                .filter(w -> "NEW".equals(w.getType()))
                .collect(Collectors.toList());

        List<ScheduleDTO.ScheduleWordItem> reviewWords = plannedWords.stream()
                .filter(w -> "REVIEW".equals(w.getType()))
                .collect(Collectors.toList());

        return ScheduleDTO.TodayResponse.builder()
                .scheduleId(schedule.getId())
                .scheduleName(schedule.getName())
                .date(progress.getDate())
                .plannedWords(plannedWords)
                .completedWords(completedWords)
                .newWords(newWords)
                .reviewWords(reviewWords)
                .totalCount(plannedWords.size())
                .completedCount(completedWords.size())
                .isCompleted(completedIds.containsAll(plannedIds) && !plannedIds.isEmpty())
                .build();
    }

    @Transactional
    public ScheduleDTO.TodayResponse completeToday(Long id, Long userId, ScheduleDTO.CompleteTodayRequest request) {
        StudySchedule schedule = scheduleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("学习计划不存在"));

        LocalDate today = LocalDate.now();
        ScheduleProgress progress = progressRepository.findByScheduleIdAndDate(id, today)
                .orElseGet(() -> {
                    ScheduleProgress p = new ScheduleProgress();
                    p.setScheduleId(id);
                    p.setDate(today);
                    p.setPlannedWordIds("");
                    p.setCompletedWordIds("");
                    return p;
                });

        List<Long> plannedIds = parseIds(progress.getPlannedWordIds());
        Set<Long> completedSet = new HashSet<>(parseIds(progress.getCompletedWordIds()));

        if (request.getCompletedWordIds() != null) {
            for (Long wordId : request.getCompletedWordIds()) {
                if (plannedIds.contains(wordId)) {
                    completedSet.add(wordId);
                }
            }
        } else {
            completedSet.addAll(plannedIds);
        }

        progress.setCompletedWordIds(joinIds(new ArrayList<>(completedSet)));
        progressRepository.save(progress);

        checkAndUpdateScheduleStatus(schedule);

        return getTodaySchedule(id, userId);
    }

    public ScheduleDTO.ScheduleDetailResponse getScheduleDetail(Long id, Long userId) {
        StudySchedule schedule = scheduleRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("学习计划不存在"));

        List<ScheduleProgress> progressList = progressRepository.findByScheduleIdOrderByDateAsc(id);

        List<ScheduleDTO.ProgressDetail> progressDetails = progressList.stream()
                .map(p -> {
                    List<Long> plannedIds = parseIds(p.getPlannedWordIds());
                    List<Long> completedIds = parseIds(p.getCompletedWordIds());
                    return ScheduleDTO.ProgressDetail.builder()
                            .date(p.getDate())
                            .plannedWordIds(plannedIds)
                            .completedWordIds(completedIds)
                            .plannedCount(plannedIds.size())
                            .completedCount(completedIds.size())
                            .isCompleted(completedIds.containsAll(plannedIds) && !plannedIds.isEmpty())
                            .build();
                })
                .collect(Collectors.toList());

        List<Long> targetIds = parseIds(schedule.getTargetWordIds());
        Set<Long> learnedSet = new HashSet<>();
        for (ScheduleProgress p : progressList) {
            learnedSet.addAll(parseIds(p.getCompletedWordIds()));
        }
        List<Long> learnedList = new ArrayList<>(learnedSet);
        learnedList.retainAll(targetIds);

        return ScheduleDTO.ScheduleDetailResponse.builder()
                .schedule(convertToScheduleResponse(schedule))
                .progressList(progressDetails)
                .ganttData(learnedList)
                .build();
    }

    private void generateProgress(StudySchedule schedule) {
        List<Long> targetWordIds = parseIds(schedule.getTargetWordIds());
        LocalDate startDate = schedule.getStartDate();
        LocalDate endDate = schedule.getEndDate();
        int dailyCount = schedule.getDailyCount();

        int totalDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        int wordsPerDay = Math.max(1, (int) Math.ceil((double) targetWordIds.size() / totalDays));
        int actualDailyNew = Math.min(dailyCount, wordsPerDay);

        Map<LocalDate, Set<Long>> dateToWords = new LinkedHashMap<>();
        for (int i = 0; i < targetWordIds.size(); i++) {
            int dayIndex = i / actualDailyNew;
            if (dayIndex >= totalDays) dayIndex = totalDays - 1;
            LocalDate learnDate = startDate.plusDays(dayIndex);
            Long wordId = targetWordIds.get(i);

            for (int interval : EBBINGHAUS_INTERVALS) {
                LocalDate reviewDate = learnDate.plusDays(interval);
                if (!reviewDate.isAfter(endDate) && !reviewDate.isBefore(LocalDate.now().minusDays(1))) {
                    dateToWords.computeIfAbsent(reviewDate, k -> new LinkedHashSet<>()).add(wordId);
                }
            }
        }

        List<Map.Entry<LocalDate, Set<Long>>> sortedEntries = new ArrayList<>(dateToWords.entrySet());
        sortedEntries.sort(Map.Entry.comparingByKey());

        for (Map.Entry<LocalDate, Set<Long>> entry : sortedEntries) {
            ScheduleProgress progress = new ScheduleProgress();
            progress.setScheduleId(schedule.getId());
            progress.setDate(entry.getKey());
            List<Long> wordList = new ArrayList<>(entry.getValue());
            wordList.sort((a, b) -> {
                int idxA = targetWordIds.indexOf(a);
                int idxB = targetWordIds.indexOf(b);
                return Integer.compare(idxA, idxB);
            });
            progress.setPlannedWordIds(joinIds(wordList));
            progress.setCompletedWordIds("");
            progressRepository.save(progress);
        }
    }

    private List<Long> getNewWordIds(StudySchedule schedule, LocalDate date) {
        List<Long> targetWordIds = parseIds(schedule.getTargetWordIds());
        LocalDate startDate = schedule.getStartDate();
        int dailyCount = schedule.getDailyCount();
        int totalDays = (int) ChronoUnit.DAYS.between(startDate, schedule.getEndDate()) + 1;
        int wordsPerDay = Math.max(1, (int) Math.ceil((double) targetWordIds.size() / totalDays));
        int actualDailyNew = Math.min(dailyCount, wordsPerDay);

        int dayIndex = (int) ChronoUnit.DAYS.between(startDate, date);
        if (dayIndex < 0) return new ArrayList<>();

        int startIdx = dayIndex * actualDailyNew;
        int endIdx = Math.min(startIdx + actualDailyNew, targetWordIds.size());
        if (startIdx >= targetWordIds.size()) return new ArrayList<>();

        return new ArrayList<>(targetWordIds.subList(startIdx, endIdx));
    }

    private void checkAndUpdateScheduleStatus(StudySchedule schedule) {
        if (schedule.getStatus() != StudySchedule.ScheduleStatus.ACTIVE) return;

        List<ScheduleProgress> allProgress = progressRepository.findByScheduleIdOrderByDateAsc(schedule.getId());
        if (allProgress.isEmpty()) return;

        boolean allCompleted = allProgress.stream()
                .allMatch(p -> {
                    List<Long> planned = parseIds(p.getPlannedWordIds());
                    List<Long> completed = parseIds(p.getCompletedWordIds());
                    return !p.getDate().isAfter(LocalDate.now()) && completed.containsAll(planned);
                });

        if (allCompleted && allProgress.get(allProgress.size() - 1).getDate().isBefore(LocalDate.now().plusDays(1))) {
            schedule.setStatus(StudySchedule.ScheduleStatus.COMPLETED);
            scheduleRepository.save(schedule);
        }
    }

    private ScheduleDTO.ScheduleResponse convertToScheduleResponse(StudySchedule schedule) {
        List<Long> targetIds = parseIds(schedule.getTargetWordIds());
        List<ScheduleProgress> progressList = progressRepository.findByScheduleIdOrderByDateAsc(schedule.getId());

        int totalDays = progressList.size();
        int completedDays = 0;
        Set<Long> learnedWords = new HashSet<>();

        for (ScheduleProgress p : progressList) {
            List<Long> planned = parseIds(p.getPlannedWordIds());
            List<Long> completed = parseIds(p.getCompletedWordIds());
            if (completed.containsAll(planned) && !planned.isEmpty()) {
                completedDays++;
            }
            learnedWords.addAll(completed);
        }

        Set<Long> targetSet = new HashSet<>(targetIds);
        learnedWords.retainAll(targetSet);

        int totalDaysSpan = (int) ChronoUnit.DAYS.between(schedule.getStartDate(), schedule.getEndDate()) + 1;
        double progressPercent = totalDaysSpan > 0 ? (completedDays * 100.0 / totalDaysSpan) : 0;

        return ScheduleDTO.ScheduleResponse.builder()
                .id(schedule.getId())
                .userId(schedule.getUserId())
                .name(schedule.getName())
                .targetWordIds(targetIds)
                .dailyCount(schedule.getDailyCount())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .status(schedule.getStatus().name())
                .createdAt(schedule.getCreatedAt() != null ?
                        schedule.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .totalDays(totalDays)
                .completedDays(completedDays)
                .totalWords(targetIds.size())
                .learnedWords(learnedWords.size())
                .progressPercent(Math.round(progressPercent * 100.0) / 100.0)
                .build();
    }

    private ScheduleDTO.ScheduleWordItem buildWordItem(Long wordId, Map<Long, Word> wordMap, String type, boolean isCompleted) {
        Word word = wordMap.get(wordId);
        if (word == null) return null;
        return ScheduleDTO.ScheduleWordItem.builder()
                .wordId(word.getId())
                .word(word.getWord())
                .phonetic(word.getPhonetic())
                .meaning(word.getMeaning())
                .example(word.getExample())
                .type(type)
                .isCompleted(isCompleted)
                .build();
    }

    private Map<Long, Word> getWordMap(List<Long> wordIds) {
        if (wordIds.isEmpty()) return new HashMap<>();
        List<Word> words = wordRepository.findAllById(wordIds);
        return words.stream().collect(Collectors.toMap(Word::getId, w -> w));
    }

    private String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return "";
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private List<Long> parseIds(String idsStr) {
        if (idsStr == null || idsStr.trim().isEmpty()) return new ArrayList<>();
        return Arrays.stream(idsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
}
