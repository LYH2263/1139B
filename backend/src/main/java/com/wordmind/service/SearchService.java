package com.wordmind.service;

import com.wordmind.dto.SearchDTO;
import com.wordmind.dto.WordDTO;
import com.wordmind.entity.SearchHistory;
import com.wordmind.entity.Word;
import com.wordmind.repository.SearchHistoryRepository;
import com.wordmind.repository.WordRepository;
import com.wordmind.util.PinyinUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final int MAX_HISTORY = 20;
    private static final int MAX_HOT_KEYWORDS = 10;
    private static final int MAX_SUGGESTIONS = 5;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired
    private WordService wordService;

    @Autowired
    private TagService tagService;

    public SearchDTO.SearchResponse searchWords(String keyword, String pos, List<Long> tagIds, int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page - 1, size);

        List<Long> tagFilteredWordIds = (tagIds != null && !tagIds.isEmpty() && userId != null)
                ? tagService.getWordIdsByTags(userId, tagIds)
                : null;

        if (keyword == null || keyword.trim().isEmpty()) {
            Page<Word> wordPage = wordRepository.searchWords(null, pos, pageable);
            List<Word> words = wordPage.getContent();

            if (tagFilteredWordIds != null) {
                words = words.stream()
                        .filter(w -> tagFilteredWordIds.contains(w.getId()))
                        .collect(Collectors.toList());
            }

            long total = tagFilteredWordIds != null
                    ? words.size()
                    : wordPage.getTotalElements();

            List<WordDTO.Response> list = words.stream()
                    .map(wordService::convertToDTO)
                    .collect(Collectors.toList());

            return SearchDTO.SearchResponse.builder()
                    .list(list)
                    .total(total)
                    .page(page)
                    .size(size)
                    .build();
        }

        saveSearchHistory(userId, keyword.trim());

        List<Word> allWords = wordRepository.findAll();
        String normalizedKeyword = keyword.trim().toLowerCase();

        List<Word> matchedWords = allWords.stream()
                .filter(word -> matchesKeyword(word, normalizedKeyword))
                .collect(Collectors.toList());

        if (pos != null && !pos.trim().isEmpty()) {
            matchedWords = matchedWords.stream()
                    .filter(word -> pos.equalsIgnoreCase(word.getPos()))
                    .collect(Collectors.toList());
        }

        if (tagFilteredWordIds != null) {
            matchedWords = matchedWords.stream()
                    .filter(w -> tagFilteredWordIds.contains(w.getId()))
                    .collect(Collectors.toList());
        }

        long total = matchedWords.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, matchedWords.size());
        List<Word> pagedWords = start < matchedWords.size()
                ? matchedWords.subList(start, end)
                : Collections.emptyList();

        List<WordDTO.Response> list = pagedWords.stream()
                .map(wordService::convertToDTO)
                .collect(Collectors.toList());

        return SearchDTO.SearchResponse.builder()
                .list(list)
                .total(total)
                .page(page)
                .size(size)
                .build();
    }

    private boolean matchesKeyword(Word word, String keyword) {
        String wordText = word.getWord() != null ? word.getWord().toLowerCase() : "";
        String meaning = word.getMeaning() != null ? word.getMeaning() : "";

        if (matchesWildcard(wordText, keyword)) {
            return true;
        }

        if (meaning.toLowerCase().contains(keyword)) {
            return true;
        }

        String meaningPinyinFirst = PinyinUtil.getFirstLetters(meaning);
        String meaningPinyinFull = PinyinUtil.getFullPinyin(meaning);
        if (meaningPinyinFirst.startsWith(keyword) || meaningPinyinFull.startsWith(keyword)) {
            return true;
        }

        return false;
    }

    private boolean matchesWildcard(String text, String pattern) {
        if (!pattern.contains("*") && !pattern.contains("?")) {
            return text.startsWith(pattern) || text.contains(pattern);
        }
        String regex = pattern
                .replace(".", "\\.")
                .replace("?", ".")
                .replace("*", ".*");
        return Pattern.matches(regex, text);
    }

    public List<SearchDTO.SearchSuggestion> getSuggestions(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedKeyword = keyword.trim().toLowerCase();
        List<Word> allWords = wordRepository.findAll();

        List<SearchDTO.SearchSuggestion> suggestions = new ArrayList<>();

        for (Word word : allWords) {
            if (suggestions.size() >= MAX_SUGGESTIONS) break;

            String wordText = word.getWord() != null ? word.getWord().toLowerCase() : "";
            String meaning = word.getMeaning() != null ? word.getMeaning() : "";
            String matchType = null;

            if (wordText.startsWith(normalizedKeyword)) {
                matchType = "word_prefix";
            } else if (wordText.contains(normalizedKeyword)) {
                matchType = "word_contains";
            } else if (matchesWildcard(wordText, normalizedKeyword)) {
                matchType = "wildcard";
            } else if (meaning.toLowerCase().contains(normalizedKeyword)) {
                matchType = "meaning_contains";
            } else {
                String meaningPinyinFirst = PinyinUtil.getFirstLetters(meaning);
                String meaningPinyinFull = PinyinUtil.getFullPinyin(meaning);
                if (meaningPinyinFirst.startsWith(normalizedKeyword) || meaningPinyinFull.startsWith(normalizedKeyword)) {
                    matchType = "pinyin";
                }
            }

            if (matchType != null) {
                suggestions.add(SearchDTO.SearchSuggestion.builder()
                        .id(word.getId())
                        .word(word.getWord())
                        .meaning(word.getMeaning())
                        .matchType(matchType)
                        .build());
            }
        }

        return suggestions;
    }

    @Transactional
    public void saveSearchHistory(Long userId, String keyword) {
        if (userId == null || keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        searchHistoryRepository.deleteByUserIdAndKeyword(userId, keyword.trim());

        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword.trim());
        searchHistoryRepository.save(history);

        List<SearchHistory> allHistory = searchHistoryRepository.findByUserIdOrderBySearchedAtDesc(userId);
        if (allHistory.size() > MAX_HISTORY) {
            List<SearchHistory> toDelete = allHistory.subList(MAX_HISTORY, allHistory.size());
            searchHistoryRepository.deleteAll(toDelete);
        }
    }

    public List<SearchDTO.SearchHistoryItem> getSearchHistory(Long userId) {
        List<SearchHistory> historyList = searchHistoryRepository.findByUserIdOrderBySearchedAtDesc(userId);
        return historyList.stream()
                .map(h -> SearchDTO.SearchHistoryItem.builder()
                        .id(h.getId())
                        .keyword(h.getKeyword())
                        .searchedAt(h.getSearchedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSearchHistory(Long userId, Long id) {
        if (id != null) {
            searchHistoryRepository.deleteByIdAndUserId(id, userId);
        } else {
            searchHistoryRepository.deleteAllByUserId(userId);
        }
    }

    public List<SearchDTO.HotKeyword> getHotKeywords() {
        List<Object[]> results = searchHistoryRepository.findHotKeywords();
        return results.stream()
                .limit(MAX_HOT_KEYWORDS)
                .map(row -> SearchDTO.HotKeyword.builder()
                        .keyword((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
    }
}
