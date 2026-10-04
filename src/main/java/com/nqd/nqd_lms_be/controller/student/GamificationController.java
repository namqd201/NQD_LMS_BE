package com.nqd.nqd_lms_be.controller.student;

import com.nqd.nqd_lms_be.config.security.AppUserPrincipal;
import com.nqd.nqd_lms_be.dto.gamification.ClaimMissionResponse;
import com.nqd.nqd_lms_be.dto.gamification.GamificationDashboardResponse;
import com.nqd.nqd_lms_be.dto.gamification.LeaderboardResponse;
import com.nqd.nqd_lms_be.service.gamification.GamificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Gamification & Streak", description = "Endpoints for student gamification dashboard, streaks, XP, missions, and leaderboard")
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/api/v1/student/gamification/dashboard")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Get current authenticated student's gamification dashboard (Streak, XP, Missions, Weekly Study)")
    public ResponseEntity<GamificationDashboardResponse> getDashboard(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(gamificationService.getStudentDashboard(principal.getId()));
    }

    @PostMapping("/api/v1/student/gamification/missions/{missionId}/claim")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    @Operation(summary = "Claim daily mission XP reward")
    public ResponseEntity<ClaimMissionResponse> claimDailyMission(
            @PathVariable UUID missionId,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        return ResponseEntity.ok(gamificationService.claimDailyMission(principal.getId(), missionId));
    }

    @GetMapping("/api/v1/gamification/leaderboard")
    @Operation(summary = "Get monthly public leaderboard")
    public ResponseEntity<LeaderboardResponse> getMonthlyLeaderboard(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(gamificationService.getMonthlyLeaderboard(currentUserId, month));
    }

    @GetMapping("/api/v1/gamification/leaderboard/all-time")
    @Operation(summary = "Get all-time public leaderboard")
    public ResponseEntity<LeaderboardResponse> getAllTimeLeaderboard(
            @AuthenticationPrincipal AppUserPrincipal principal
    ) {
        UUID currentUserId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(gamificationService.getAllTimeLeaderboard(currentUserId));
    }

    @PostMapping("/api/v1/admin/gamification/process-monthly-rewards")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin manual trigger to distribute monthly rewards")
    public ResponseEntity<String> processMonthlyRewards(
            @RequestParam(required = false) String monthYear
    ) {
        gamificationService.processMonthlyRewards(monthYear);
        return ResponseEntity.ok("Xử lý trao thưởng tháng thành công!");
    }
}
