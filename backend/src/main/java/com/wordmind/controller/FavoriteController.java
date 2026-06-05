package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.FavoriteDTO;
import com.wordmind.security.JwtTokenProvider;
import com.wordmind.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    
    @Autowired
    private FavoriteService favoriteService;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<FavoriteDTO.PageResponse> getFavorites(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(favoriteService.getFavorites(userId, page, size));
    }
    
    @GetMapping("/word-ids")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<Long>> getFavoriteWordIds(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(favoriteService.getFavoriteWordIds(userId));
    }
    
    @GetMapping("/status/{wordId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<FavoriteDTO.FavoriteStatus> getFavoriteStatus(
            HttpServletRequest request,
            @PathVariable Long wordId) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(favoriteService.getFavoriteStatus(userId, wordId));
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<FavoriteDTO.Response> addFavorite(
            HttpServletRequest request,
            @Valid @RequestBody FavoriteDTO.AddRequest addRequest) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(favoriteService.addFavorite(userId, addRequest.getWordId()));
    }
    
    @DeleteMapping("/{wordId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> removeFavorite(
            HttpServletRequest request,
            @PathVariable Long wordId) {
        Long userId = getUserIdFromRequest(request);
        favoriteService.removeFavorite(userId, wordId);
        return ApiResponse.success();
    }
    
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return tokenProvider.getUserIdFromToken(token);
    }
}
