package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndIsReadFalse(UUID userId);

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.user.id = :userId AND n.type = :type AND n.linkUrl = :linkUrl AND n.createdAt >= :since")
    boolean existsByUserIdAndTypeAndLinkUrlAndCreatedAtAfter(
            @Param("userId") UUID userId,
            @Param("type") String type,
            @Param("linkUrl") String linkUrl,
            @Param("since") java.time.LocalDateTime since
    );

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") UUID userId);
}
