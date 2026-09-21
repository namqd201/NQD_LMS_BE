package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.UserRole;
import com.nqd.nqd_lms_be.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    List<UserRole> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);

    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);

    @Query("SELECT r.name FROM UserRole ur JOIN ur.role r WHERE ur.userId = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") UUID userId);

    long countByRoleId(UUID roleId);

    @Query("SELECT ur.user FROM UserRole ur JOIN ur.role r WHERE r.name = :roleName")
    List<com.nqd.nqd_lms_be.entity.User> findUsersByRoleName(@Param("roleName") String roleName);
}
