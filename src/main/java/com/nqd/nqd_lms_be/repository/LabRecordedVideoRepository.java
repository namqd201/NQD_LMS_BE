package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.LabRecordedVideo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LabRecordedVideoRepository extends JpaRepository<LabRecordedVideo, UUID> {

    @Query("SELECT v FROM LabRecordedVideo v JOIN FETCH v.labRoom r JOIN FETCH v.ownerUser u " +
           "WHERE v.ownerUser.id = :ownerUserId AND v.isDeleted = false ORDER BY v.createdAt DESC")
    List<LabRecordedVideo> findByOwnerUserId(@Param("ownerUserId") UUID ownerUserId);

    @Query("SELECT v FROM LabRecordedVideo v JOIN FETCH v.labRoom r JOIN FETCH v.ownerUser u " +
           "WHERE v.isDeleted = false ORDER BY v.createdAt DESC")
    List<LabRecordedVideo> findAllForAdmin();

    List<LabRecordedVideo> findByLabRoomIdAndIsDeletedFalse(UUID labRoomId);

    boolean existsByRecordingAssetId(String recordingAssetId);
}
