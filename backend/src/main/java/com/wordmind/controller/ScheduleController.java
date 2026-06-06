package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.ScheduleDTO;
import com.wordmind.security.JwtTokenProvider;
import com.wordmind.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.ListResponse> getSchedules(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(scheduleService.getSchedules(userId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.ScheduleResponse> getSchedule(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(scheduleService.getSchedule(id, userId));
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.ScheduleDetailResponse> getScheduleDetail(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(scheduleService.getScheduleDetail(id, userId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.ScheduleResponse> createSchedule(
            @Valid @RequestBody ScheduleDTO.CreateRequest createRequest,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(scheduleService.createSchedule(userId, createRequest));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.ScheduleResponse> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleDTO.UpdateRequest updateRequest,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(scheduleService.updateSchedule(id, userId, updateRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<Void> deleteSchedule(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        scheduleService.deleteSchedule(id, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/today")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.TodayResponse> getTodaySchedule(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(scheduleService.getTodaySchedule(id, userId));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<ScheduleDTO.TodayResponse> completeToday(
            @PathVariable Long id,
            @RequestBody(required = false) ScheduleDTO.CompleteTodayRequest completeRequest,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        return ApiResponse.success(scheduleService.completeToday(id, userId, completeRequest));
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return tokenProvider.getUserIdFromToken(token);
    }
}
