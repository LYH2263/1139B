package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.TagDTO;
import com.wordmind.security.UserPrincipal;
import com.wordmind.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<TagDTO.Response>> getTags(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(tagService.getTagsByUserId(principal.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<TagDTO.Response> getTagById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(tagService.getTagById(principal.getId(), id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<TagDTO.Response> createTag(
            @Valid @RequestBody TagDTO.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(tagService.createTag(principal.getId(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<TagDTO.Response> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagDTO.UpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(tagService.updateTag(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> deleteTag(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        tagService.deleteTag(principal.getId(), id);
        return ApiResponse.success();
    }

    @GetMapping("/preset-colors")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<String>> getPresetColors() {
        return ApiResponse.success(java.util.Arrays.asList(
                "#409EFF", "#67C23A", "#E6A23C", "#F56C6C",
                "#909399", "#8E44AD", "#16A085", "#D35400"
        ));
    }
}
