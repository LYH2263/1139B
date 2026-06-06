package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.SearchDTO;
import com.wordmind.security.UserPrincipal;
import com.wordmind.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<SearchDTO.SearchResponse> searchWords(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String pos,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getId() : null;
        return ApiResponse.success(searchService.searchWords(keyword, pos, page, size, userId));
    }

    @GetMapping("/suggestions")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<SearchDTO.SearchSuggestion>> getSuggestions(
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(searchService.getSuggestions(keyword));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<SearchDTO.SearchHistoryItem>> getSearchHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(searchService.getSearchHistory(principal.getId()));
    }

    @DeleteMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> deleteSearchHistory(
            @RequestParam(required = false) Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        searchService.deleteSearchHistory(principal.getId(), id);
        return ApiResponse.success();
    }

    @DeleteMapping("/history/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> deleteSearchHistoryById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        searchService.deleteSearchHistory(principal.getId(), id);
        return ApiResponse.success();
    }

    @GetMapping("/hot")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<SearchDTO.HotKeyword>> getHotKeywords() {
        return ApiResponse.success(searchService.getHotKeywords());
    }
}
