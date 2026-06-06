package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.TagDTO;
import com.wordmind.dto.WordDTO;
import com.wordmind.security.UserPrincipal;
import com.wordmind.service.TagService;
import com.wordmind.service.WordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/words")
public class WordController {
    
    @Autowired
    private WordService wordService;

    @Autowired
    private TagService tagService;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<WordDTO.ListResponse> getWords(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String pos,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(wordService.getWords(keyword, pos, page, size));
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<WordDTO.Response> getWordById(@PathVariable Long id) {
        return ApiResponse.success(wordService.getWordById(id));
    }

    @GetMapping("/{wordId}/tags")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<TagDTO.Response>> getWordTags(
            @PathVariable Long wordId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(tagService.getWordTags(principal.getId(), wordId));
    }

    @PostMapping("/{wordId}/tags")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<TagDTO.Response> bindTagToWord(
            @PathVariable Long wordId,
            @Valid @RequestBody TagDTO.WordTagRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(tagService.bindTagToWord(principal.getId(), wordId, request.getTagId()));
    }

    @DeleteMapping("/{wordId}/tags/{tagId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> unbindTagFromWord(
            @PathVariable Long wordId,
            @PathVariable Long tagId,
            @AuthenticationPrincipal UserPrincipal principal) {
        tagService.unbindTagFromWord(principal.getId(), wordId, tagId);
        return ApiResponse.success();
    }
}
