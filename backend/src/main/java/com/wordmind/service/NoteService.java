package com.wordmind.service;

import com.wordmind.dto.NoteDTO;
import com.wordmind.entity.Note;
import com.wordmind.entity.Word;
import com.wordmind.repository.NoteRepository;
import com.wordmind.repository.WordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoteService {
    
    @Autowired
    private NoteRepository noteRepository;
    
    @Autowired
    private WordRepository wordRepository;
    
    @Transactional
    public NoteDTO.Response createNote(Long userId, Long wordId, String content) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));
        
        if (noteRepository.existsByUserIdAndWordId(userId, wordId)) {
            throw new RuntimeException("该单词已有笔记，如需修改请使用更新接口");
        }
        
        Note note = new Note();
        note.setUserId(userId);
        note.setWordId(wordId);
        note.setContent(content);
        Note saved = noteRepository.save(note);
        
        return convertToDTO(saved, word);
    }
    
    @Transactional
    public NoteDTO.Response updateNote(Long userId, Long wordId, String content) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));
        
        Note note = noteRepository.findByUserIdAndWordId(userId, wordId)
                .orElseThrow(() -> new RuntimeException("该单词暂无笔记"));
        
        note.setContent(content);
        Note saved = noteRepository.save(note);
        
        return convertToDTO(saved, word);
    }
    
    @Transactional
    public void deleteNote(Long userId, Long wordId) {
        if (!noteRepository.existsByUserIdAndWordId(userId, wordId)) {
            throw new RuntimeException("该单词暂无笔记");
        }
        noteRepository.deleteByUserIdAndWordId(userId, wordId);
    }
    
    public NoteDTO.Response getNoteByWordId(Long userId, Long wordId) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new RuntimeException("单词不存在"));
        
        return noteRepository.findByUserIdAndWordId(userId, wordId)
                .map(note -> convertToDTO(note, word))
                .orElse(null);
    }
    
    public NoteDTO.PageResponse getNotes(Long userId, int page, int size, String keyword) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Note> notePage;
        
        if (StringUtils.hasText(keyword)) {
            notePage = noteRepository.findByUserIdAndContentContaining(userId, keyword.trim(), pageable);
        } else {
            notePage = noteRepository.findByUserId(userId, pageable);
        }
        
        List<NoteDTO.Response> list = notePage.getContent().stream()
                .map(note -> {
                    Word word = wordRepository.findById(note.getWordId()).orElse(null);
                    return convertToDTO(note, word);
                })
                .collect(Collectors.toList());
        
        return NoteDTO.PageResponse.builder()
                .list(list)
                .total(notePage.getTotalElements())
                .page(page)
                .size(size)
                .build();
    }
    
    private NoteDTO.Response convertToDTO(Note note, Word word) {
        String summary = note.getContent();
        if (summary.length() > 100) {
            summary = summary.substring(0, 100) + "...";
        }
        
        return NoteDTO.Response.builder()
                .id(note.getId())
                .userId(note.getUserId())
                .wordId(note.getWordId())
                .word(word != null ? word.getWord() : "")
                .phonetic(word != null ? word.getPhonetic() : "")
                .pos(word != null ? word.getPos() : "")
                .meaning(word != null ? word.getMeaning() : "")
                .content(note.getContent())
                .contentSummary(summary)
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
