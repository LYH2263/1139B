package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.NoteDTO;
import com.wordmind.security.JwtTokenProvider;
import com.wordmind.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api")
public class NoteController {
    
    @Autowired
    private NoteService noteService;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @GetMapping("/notes")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<NoteDTO.PageResponse> getNotes(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(noteService.getNotes(userId, page, size, keyword));
    }
    
    @GetMapping("/words/{wordId}/notes")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<NoteDTO.Response> getNoteByWordId(
            HttpServletRequest request,
            @PathVariable Long wordId) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(noteService.getNoteByWordId(userId, wordId));
    }
    
    @PostMapping("/words/{wordId}/notes")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<NoteDTO.Response> createNote(
            HttpServletRequest request,
            @PathVariable Long wordId,
            @Valid @RequestBody NoteDTO.CreateRequest createRequest) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(noteService.createNote(userId, wordId, createRequest.getContent()));
    }
    
    @PutMapping("/words/{wordId}/notes")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<NoteDTO.Response> updateNote(
            HttpServletRequest request,
            @PathVariable Long wordId,
            @Valid @RequestBody NoteDTO.UpdateRequest updateRequest) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(noteService.updateNote(userId, wordId, updateRequest.getContent()));
    }
    
    @DeleteMapping("/words/{wordId}/notes")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> deleteNote(
            HttpServletRequest request,
            @PathVariable Long wordId) {
        Long userId = getUserIdFromRequest(request);
        noteService.deleteNote(userId, wordId);
        return ApiResponse.success();
    }
    
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return tokenProvider.getUserIdFromToken(token);
    }
}
