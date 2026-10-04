package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.UserGamification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGamificationRepository extends JpaRepository<UserGamification, UUID> {

    Optional<UserGamification> findByUserId(UUID userId);

    @Query("SELECT g FROM UserGamification g JOIN FETCH g.user u WHERE u.status = 'ACTIVE' ORDER BY g.monthlyXp DESC, g.currentStreak DESC, g.totalXp DESC")
    List<UserGamification> findMonthlyLeaderboard(Pageable pageable);

    @Query("SELECT g FROM UserGamification g JOIN FETCH g.user u WHERE u.status = 'ACTIVE' ORDER BY g.totalXp DESC, g.currentStreak DESC")
    List<UserGamification> findAllTimeLeaderboard(Pageable pageable);

    @Query("SELECT COUNT(g) + 1 FROM UserGamification g WHERE g.monthlyXp > :monthlyXp")
    long findMonthlyRank(@Param("monthlyXp") Long monthlyXp);

    @Query("SELECT g FROM UserGamification g WHERE g.lastActivityDate < :thresholdDate AND g.currentStreak > 0")
    List<UserGamification> findBrokenStreaks(@Param("thresholdDate") LocalDate thresholdDate);

    @Modifying
    @Query("UPDATE UserGamification g SET g.monthlyXp = 0")
    int resetAllMonthlyXp();
}
