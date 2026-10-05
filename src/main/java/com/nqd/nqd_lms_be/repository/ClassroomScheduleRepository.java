package com.nqd.nqd_lms_be.repository;

import com.nqd.nqd_lms_be.entity.ClassroomSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassroomScheduleRepository extends JpaRepository<ClassroomSchedule, UUID> {
    List<ClassroomSchedule> findByClassroomIdOrderByDayOfWeekAscStartTimeAsc(UUID classroomId);
    List<ClassroomSchedule> findByDayOfWeek(String dayOfWeek);

    @Query("SELECT cs FROM ClassroomSchedule cs JOIN FETCH cs.classroom c LEFT JOIN FETCH c.teacher WHERE cs.dayOfWeek = :dayOfWeek")
    List<ClassroomSchedule> findByDayOfWeekWithClassroomAndTeacher(@Param("dayOfWeek") String dayOfWeek);
}
