package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.LabRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabRoomRepository extends JpaRepository<LabRoom, UUID> {

    @Query("SELECT r FROM LabRoom r JOIN FETCH r.hostUser u WHERE r.isDeleted = false " +
           "ORDER BY CASE WHEN r.status = 'LIVE' THEN 1 WHEN r.status = 'SCHEDULED' THEN 2 ELSE 3 END, r.scheduledStartTime ASC")
    List<LabRoom> findAllActiveRooms();

    List<LabRoom> findByHostUserIdAndIsDeletedFalseOrderByScheduledStartTimeDesc(UUID hostUserId);
}
