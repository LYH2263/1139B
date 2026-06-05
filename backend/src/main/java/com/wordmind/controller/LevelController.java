package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.LevelDTO;
import com.wordmind.security.JwtTokenProvider;
import com.wordmind.service.LevelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/levels")
public class LevelController {
    
    @Autowired
    private LevelService levelService;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<LevelDTO.LevelResponse>> getLevels(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(levelService.getLevelsWithProgress(userId));
    }
    
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<LevelDTO.StartResponse> startLevel(
            HttpServletRequest request,
            @PathVariable Long id) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(levelService.startLevel(userId, id));
    }
    
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<LevelDTO.SubmitResponse> submitLevel(
            HttpServletRequest request,
            @PathVariable Long id,
            @Valid @RequestBody LevelDTO.SubmitRequest submitRequest) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(levelService.submitLevel(userId, submitRequest));
    }
    
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return tokenProvider.getUserIdFromToken(token);
    }
}
