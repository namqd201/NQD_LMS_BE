package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.MonthlyLeaderboardReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MonthlyLeaderboardRewardRepository extends JpaRepository<MonthlyLeaderboardReward, UUID> {

    List<MonthlyLeaderboardReward> findByMonthYearOrderByRankPositionAsc(String monthYear);

    List<MonthlyLeaderboardReward> findByUserIdOrderByMonthYearDesc(UUID userId);

    @Query("SELECT r FROM MonthlyLeaderboardReward r JOIN FETCH r.user WHERE r.monthYear = :monthYear ORDER BY r.rankPosition ASC")
    List<MonthlyLeaderboardReward> findRewardsWithUserByMonthYear(@Param("monthYear") String monthYear);

    boolean existsByMonthYear(String monthYear);
}
