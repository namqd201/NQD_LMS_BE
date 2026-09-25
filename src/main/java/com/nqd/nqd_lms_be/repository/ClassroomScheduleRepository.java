package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ClassroomSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassroomScheduleRepository extends JpaRepository<ClassroomSchedule, UUID> {
    List<ClassroomSchedule> findByClassroomIdOrderByDayOfWeekAscStartTimeAsc(UUID classroomId);
}
