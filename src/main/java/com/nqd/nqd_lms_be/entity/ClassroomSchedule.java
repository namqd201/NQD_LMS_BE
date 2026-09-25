package com.nqd.nqd_lms_be.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

@Entity
@Table(
    name = "classroom_schedules",
    indexes = {
        @Index(name = "idx_classroom_schedules_class", columnList = "classroom_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldNameConstants
public class ClassroomSchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_id", nullable = false)
    private Classroom classroom;

    /**
     * MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
     */
    @Column(name = "day_of_week", nullable = false, length = 30)
    private String dayOfWeek;

    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime; // e.g. "19:30"

    @Column(name = "end_time", nullable = false, length = 10)
    private String endTime; // e.g. "21:00"

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "room_note", length = 255)
    private String roomNote; // e.g. "Học trực tuyến qua Lark"
}
