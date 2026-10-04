package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.UserDailyMission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDailyMissionRepository extends JpaRepository<UserDailyMission, UUID> {

    List<UserDailyMission> findByUserIdAndMissionDate(UUID userId, LocalDate missionDate);

    Optional<UserDailyMission> findByUserIdAndMissionDateAndMissionKey(UUID userId, LocalDate missionDate, String missionKey);
}
