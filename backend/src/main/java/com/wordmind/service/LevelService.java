package com.wordmind.service;

import com.wordmind.dto.LevelDTO;
import com.wordmind.dto.QuizDTO;
import com.wordmind.dto.WordDTO;
import com.wordmind.entity.Level;
import com.wordmind.entity.LevelProgress;
import com.wordmind.entity.Word;
import com.wordmind.repository.LevelProgressRepository;
import com.wordmind.repository.LevelRepository;
import com.wordmind.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LevelService {
    
    @Autowired
    private LevelRepository levelRepository;
    
    @Autowired
    private LevelProgressRepository levelProgressRepository;
    
    @Autowired
    private WordRepository wordRepository;
    
    private final Map<String, LevelSession> levelSessions = new HashMap<>();
    
    public List<LevelDTO.LevelResponse> getLevelsWithProgress(Long userId) {
        List<Level> levels = levelRepository.findAllByOrderByOrderAsc();
        List<LevelProgress> progresses = levelProgressRepository.findByUserId(userId);
        
        Map<Long, LevelProgress> progressMap = progresses.stream()
                .collect(Collectors.toMap(LevelProgress::getLevelId, p -> p));
        
        Set<Long> completedLevelIds = progresses.stream()
                .filter(LevelProgress::getCompleted)
                .map(LevelProgress::getLevelId)
                .collect(Collectors.toSet());
        
        return levels.stream()
                .map(level -> {
                    LevelProgress progress = progressMap.get(level.getId());
                    boolean unlocked = isLevelUnlocked(level, completedLevelIds);
                    
                    return LevelDTO.LevelResponse.builder()
                            .id(level.getId())
                            .name(level.getName())
                            .description(level.getDescription())
                            .difficulty(level.getDifficulty())
                            .passingScore(level.getPassingScore())
                            .order(level.getOrder())
                            .wordCount(getWordIds(level).size())
                            .unlocked(unlocked)
                            .progress(progress != null ? convertToProgressDTO(progress) : null)
                            .build();
                })
                .collect(Collectors.toList());
    }
    
    private boolean isLevelUnlocked(Level level, Set<Long> completedLevelIds) {
        if (level.getOrder() == 1) {
            return true;
        }
        
        Optional<Level> previousLevel = levelRepository.findByOrder(level.getOrder() - 1);
        return previousLevel.map(value -> completedLevelIds.contains(value.getId())).orElse(false);
    }
    
    public LevelDTO.StartResponse startLevel(Long userId, Long levelId) {
        Level level = levelRepository.findById(levelId)
                .orElseThrow(() -> new RuntimeException("关卡不存在"));
        
        List<LevelProgress> progresses = levelProgressRepository.findByUserId(userId);
        Set<Long> completedLevelIds = progresses.stream()
                .filter(LevelProgress::getCompleted)
                .map(LevelProgress::getLevelId)
                .collect(Collectors.toSet());
        
        if (!isLevelUnlocked(level, completedLevelIds)) {
            throw new RuntimeException("该关卡尚未解锁");
        }
        
        List<Long> wordIds = getWordIds(level);
        if (wordIds.isEmpty()) {
            throw new RuntimeException("该关卡没有配置单词");
        }
        
        List<Word> words = wordRepository.findAllById(wordIds);
        List<Word> allWords = wordRepository.findAll();
        
        String sessionId = UUID.randomUUID().toString();
        LocalDateTime startTime = LocalDateTime.now();
        
        List<QuizDTO.Question> questions = words.stream()
                .map(word -> generateQuestion(word, allWords))
                .collect(Collectors.toList());
        
        levelSessions.put(sessionId, new LevelSession(userId, levelId, startTime, words));
        
        return LevelDTO.StartResponse.builder()
                .sessionId(sessionId)
                .questions(questions)
                .build();
    }
    
    @Transactional
    public LevelDTO.SubmitResponse submitLevel(Long userId, LevelDTO.SubmitRequest request) {
        LevelSession session = levelSessions.get(request.getSessionId());
        if (session == null) {
            throw new RuntimeException("会话不存在或已过期");
        }
        
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该会话");
        }
        
        Level level = levelRepository.findById(session.getLevelId())
                .orElseThrow(() -> new RuntimeException("关卡不存在"));
        
        List<Word> words = session.getWords();
        int correctCount = 0;
        List<Word> wrongWords = new ArrayList<>();
        
        for (int i = 0; i < words.size() && i < request.getAnswers().size(); i++) {
            Word word = words.get(i);
            QuizDTO.Answer answer = request.getAnswers().get(i);
            
            if (isCorrect(word, answer)) {
                correctCount++;
            } else {
                wrongWords.add(word);
            }
        }
        
        int totalCount = words.size();
        int score = totalCount > 0 ? (correctCount * 100 / totalCount) : 0;
        int stars = calculateStars(score);
        boolean passed = score >= level.getPassingScore();
        int duration = (int) java.time.Duration.between(session.getStartTime(), LocalDateTime.now()).getSeconds();
        
        LevelProgress progress = levelProgressRepository.findByUserIdAndLevelId(userId, level.getId())
                .orElse(null);
        
        boolean newlyCompleted = false;
        if (progress == null) {
            progress = new LevelProgress();
            progress.setUserId(userId);
            progress.setLevelId(level.getId());
            progress.setBestScore(score);
            progress.setStars(stars);
            progress.setCompleted(passed);
            progress.setAttempts(1);
            progress.setLastAttemptAt(LocalDateTime.now());
            newlyCompleted = passed;
        } else {
            progress.setAttempts(progress.getAttempts() + 1);
            progress.setLastAttemptAt(LocalDateTime.now());
            
            if (score > progress.getBestScore()) {
                progress.setBestScore(score);
            }
            
            if (stars > (progress.getStars() != null ? progress.getStars() : 0)) {
                progress.setStars(stars);
            }
            
            if (!progress.getCompleted() && passed) {
                progress.setCompleted(true);
                newlyCompleted = true;
            }
        }
        
        levelProgressRepository.save(progress);
        levelSessions.remove(request.getSessionId());
        
        return LevelDTO.SubmitResponse.builder()
                .levelId(level.getId())
                .score(score)
                .correctCount(correctCount)
                .totalCount(totalCount)
                .stars(stars)
                .passed(passed)
                .newlyCompleted(newlyCompleted)
                .duration(duration)
                .wrongWords(wrongWords.stream()
                        .map(this::convertToDTO)
                        .collect(Collectors.toList()))
                .progress(convertToProgressDTO(progress))
                .build();
    }
    
    private int calculateStars(int score) {
        if (score >= 100) {
            return 3;
        } else if (score >= 90) {
            return 2;
        } else if (score >= 80) {
            return 1;
        }
        return 0;
    }
    
    private List<Long> getWordIds(Level level) {
        if (level.getWordIds() == null || level.getWordIds().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(level.getWordIds().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }
    
    private QuizDTO.Question generateQuestion(Word word, List<Word> allWords) {
        List<String> options = new ArrayList<>();
        options.add(word.getMeaning());
        
        List<Word> otherWords = allWords.stream()
                .filter(w -> !w.getId().equals(word.getId()))
                .collect(Collectors.toList());
        Collections.shuffle(otherWords);
        
        for (int i = 0; i < Math.min(3, otherWords.size()); i++) {
            options.add(otherWords.get(i).getMeaning());
        }
        
        Collections.shuffle(options);
        
        return QuizDTO.Question.builder()
                .wordId(word.getId())
                .word(word.getWord())
                .type("CHOICE")
                .question("请选择 \"" + word.getWord() + "\" 的正确释义")
                .options(options)
                .correctAnswer(word.getMeaning())
                .build();
    }
    
    private boolean isCorrect(Word word, QuizDTO.Answer answer) {
        return word.getMeaning().equals(answer.getAnswer());
    }
    
    private WordDTO.Response convertToDTO(Word word) {
        return WordDTO.Response.builder()
                .id(word.getId())
                .word(word.getWord())
                .phonetic(word.getPhonetic())
                .pos(word.getPos())
                .meaning(word.getMeaning())
                .example(word.getExample())
                .memoryTip(word.getMemoryTip())
                .build();
    }
    
    private LevelDTO.LevelProgressResponse convertToProgressDTO(LevelProgress progress) {
        return LevelDTO.LevelProgressResponse.builder()
                .id(progress.getId())
                .bestScore(progress.getBestScore())
                .stars(progress.getStars())
                .completed(progress.getCompleted())
                .attempts(progress.getAttempts())
                .lastAttemptAt(progress.getLastAttemptAt())
                .build();
    }
    
    private static class LevelSession {
        private final Long userId;
        private final Long levelId;
        private final LocalDateTime startTime;
        private final List<Word> words;
        
        public LevelSession(Long userId, Long levelId, LocalDateTime startTime, List<Word> words) {
            this.userId = userId;
            this.levelId = levelId;
            this.startTime = startTime;
            this.words = words;
        }
        
        public Long getUserId() { return userId; }
        public Long getLevelId() { return levelId; }
        public LocalDateTime getStartTime() { return startTime; }
        public List<Word> getWords() { return words; }
    }
}
