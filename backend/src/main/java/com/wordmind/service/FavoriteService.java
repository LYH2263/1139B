package com.wordmind.service;

import com.wordmind.dto.FavoriteDTO;
import com.wordmind.entity.Favorite;
import com.wordmind.entity.Word;
import com.wordmind.repository.FavoriteRepository;
import com.wordmind.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {
    
    @Autowired
    private FavoriteRepository favoriteRepository;
    
    @Autowired
    private WordRepository wordRepository;
    
    @Transactional
    public FavoriteDTO.Response addFavorite(Long userId, Long wordId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));
        
        if (favoriteRepository.existsByUserIdAndWordId(userId, wordId)) {
            throw new RuntimeException("该单词已收藏");
        }
        
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setWordId(wordId);
        Favorite saved = favoriteRepository.save(favorite);
        
        return convertToDTO(saved, word);
    }
    
    @Transactional
    public void removeFavorite(Long userId, Long wordId) {
        if (!favoriteRepository.existsByUserIdAndWordId(userId, wordId)) {
            throw new RuntimeException("该单词未收藏");
        }
        favoriteRepository.deleteByUserIdAndWordId(userId, wordId);
    }
    
    public FavoriteDTO.PageResponse getFavorites(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Favorite> favoritePage = favoriteRepository.findByUserId(userId, pageable);
        
        List<FavoriteDTO.Response> list = favoritePage.getContent().stream()
                .map(favorite -> {
                    Word word = wordRepository.findById(favorite.getWordId()).orElse(null);
                    return convertToDTO(favorite, word);
                })
                .collect(Collectors.toList());
        
        return FavoriteDTO.PageResponse.builder()
                .list(list)
                .total(favoritePage.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }
    
    public List<Long> getFavoriteWordIds(Long userId) {
        return favoriteRepository.findFavoriteWordIdsByUserId(userId);
    }
    
    public FavoriteDTO.FavoriteStatus getFavoriteStatus(Long userId, Long wordId) {
        boolean isFavorite = favoriteRepository.existsByUserIdAndWordId(userId, wordId);
        return FavoriteDTO.FavoriteStatus.builder()
                .wordId(wordId)
                .isFavorite(isFavorite)
                .build();
    }
    
    private FavoriteDTO.Response convertToDTO(Favorite favorite, Word word) {
        return FavoriteDTO.Response.builder()
                .id(favorite.getId())
                .wordId(favorite.getWordId())
                .word(word != null ? word.getWord() : "")
                .phonetic(word != null ? word.getPhonetic() : "")
                .pos(word != null ? word.getPos() : "")
                .meaning(word != null ? word.getMeaning() : "")
                .createdAt(favorite.getCreatedAt())
                .build();
    }
}
