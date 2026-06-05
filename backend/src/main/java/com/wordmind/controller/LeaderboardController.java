package com.wordmind.controller;

import com.wordmind.dto.ApiResponse;
import com.wordmind.dto.LeaderboardDTO;
import com.wordmind.security.JwtTokenProvider;
import com.wordmind.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @GetMapping("/leaderboard")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<LeaderboardDTO.Response> getLeaderboard(
            @RequestParam(required = false) String dimension,
            HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);

        if (dimension != null && !dimension.isEmpty()) {
            LeaderboardDTO.DimensionResponse dimResponse = leaderboardService.getLeaderboardByDimension(dimension, userId);
            LeaderboardDTO.Response response = new LeaderboardDTO.Response();
            switch (LeaderboardDTO.Dimension.fromValue(dimension)) {
                case MASTERED_WORDS:
                    response.setMasteredWords(dimResponse);
                    break;
                case QUIZ_SCORE:
                    response.setQuizScore(dimResponse);
                    break;
                case STREAK_DAYS:
                    response.setStreakDays(dimResponse);
                    break;
            }
            return ApiResponse.success(response);
        }

        return ApiResponse.success(leaderboardService.getAllLeaderboards(userId));
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return tokenProvider.getUserIdFromToken(token);
    }
}
