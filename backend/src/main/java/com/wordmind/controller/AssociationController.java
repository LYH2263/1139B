package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.AssociationDTO;
import com.wordmind.security.JwtTokenProvider;
import com.wordmind.service.AssociationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api")
public class AssociationController {

    @Autowired
    private AssociationService associationService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @GetMapping("/words/{wordId}/associations")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<AssociationDTO.ListResponse> getAssociations(
            HttpServletRequest request,
            @PathVariable Long wordId) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(associationService.getAssociations(wordId, userId));
    }

    @PostMapping("/words/{wordId}/associations")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<AssociationDTO.Response> createAssociation(
            HttpServletRequest request,
            @PathVariable Long wordId,
            @Valid @RequestBody AssociationDTO.CreateRequest createRequest) {
        Long userId = getUserIdFromRequest(request);
        createRequest.setWordId(wordId);
        return ApiResponse.success(associationService.createAssociation(userId, createRequest));
    }

    @PostMapping("/associations/{id}/upvote")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<AssociationDTO.UpvoteResponse> upvote(
            @PathVariable Long id) {
        return ApiResponse.success(associationService.upvote(id));
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return tokenProvider.getUserIdFromToken(token);
    }
}
