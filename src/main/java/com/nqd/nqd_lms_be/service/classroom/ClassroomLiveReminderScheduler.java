package com.nqd.nqd_lms_be.service.classroom;

import com.nqd.nqd_lms_be.entity.ClassroomSchedule;
import com.nqd.nqd_lms_be.entity.ClassroomStudent;
import com.nqd.nqd_lms_be.entity.LabRoom;
import com.nqd.nqd_lms_be.entity.enums.ClassEnrollmentStatus;
import com.nqd.nqd_lms_be.entity.enums.LabStatus;
import com.nqd.nqd_lms_be.repository.ClassroomScheduleRepository;
import com.nqd.nqd_lms_be.repository.ClassroomStudentRepository;
import com.nqd.nqd_lms_be.repository.LabRoomRepository;
import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClassroomLiveReminderScheduler {

    private final ClassroomScheduleRepository scheduleRepository;
    private final ClassroomStudentRepository studentRepository;
    private final LabRoomRepository labRoomRepository;
    private final KafkaNotificationProducer notificationProducer;

    // In-memory cache to prevent duplicate reminders on the same day: key = type_id_yyyy-MM-dd
    private final Set<String> remindedKeys = ConcurrentHashMap.newKeySet();

    /**
     * Check every 5 minutes (in VN +7) for upcoming classes & labs starting in the next 15-20 minutes.
     */
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Ho_Chi_Minh")
    public void checkUpcomingClassesAndRemind() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        String dayOfWeek = today.getDayOfWeek().name();
        String dateKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 1. Remind Classroom Schedules
        try {
            List<ClassroomSchedule> todaySchedules = scheduleRepository.findByDayOfWeek(dayOfWeek);
            for (ClassroomSchedule schedule : todaySchedules) {
                String reminderKey = "CLASS_" + schedule.getId() + "_" + dateKey;
                if (remindedKeys.contains(reminderKey)) {
                    continue;
                }

                try {
                    String[] parts = schedule.getStartTime().split(":");
                    int hour = Integer.parseInt(parts[0].trim());
                    int minute = Integer.parseInt(parts[1].trim());
                    LocalTime startTime = LocalTime.of(hour, minute);

                    // If starting within 15 minutes (between now and now + 16 minutes)
                    if (!startTime.isBefore(now) && startTime.isBefore(now.plusMinutes(16))) {
                        remindedKeys.add(reminderKey);

                        String title = "⏰ Lớp học online sắp bắt đầu!";
                        String body = String.format("Buổi học '%s' của lớp %s sẽ bắt đầu lúc %s. Hãy chuẩn bị vào lớp nhé!",
                                schedule.getTitle(),
                                schedule.getClassroom().getName(),
                                schedule.getStartTime());
                        String linkUrl = "/classrooms/" + schedule.getClassroom().getId();

                        // Notify Teacher
                        if (schedule.getClassroom().getTeacher() != null) {
                            notificationProducer.sendNotification(
                                    schedule.getClassroom().getTeacher().getId(),
                                    "CLASS_LIVE_REMINDER",
                                    title,
                                    body,
                                    linkUrl
                            );
                        }

                        // Notify Students
                        List<ClassroomStudent> students = studentRepository.findByClassroomIdAndStatusOrderByJoinedAtDesc(
                                schedule.getClassroom().getId(), ClassEnrollmentStatus.ENROLLED);
                        for (ClassroomStudent cs : students) {
                            notificationProducer.sendNotification(
                                    cs.getStudent().getId(),
                                    "CLASS_LIVE_REMINDER",
                                    title,
                                    body,
                                    linkUrl
                            );
                        }

                        log.info("Dispatched live class reminder for schedule {} (class: {}) to {} students",
                                schedule.getId(), schedule.getClassroom().getName(), students.size());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse start time for schedule {}: {}", schedule.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error checking classroom schedule reminders: {}", e.getMessage(), e);
        }

        // 2. Remind Lab Rooms
        try {
            LocalDateTime nowDt = LocalDateTime.now();
            LocalDateTime upperDt = nowDt.plusMinutes(16);
            List<LabRoom> upcomingLabs = labRoomRepository.findByStatusAndScheduledStartTimeBetweenAndIsDeletedFalse(
                    LabStatus.SCHEDULED, nowDt, upperDt);

            for (LabRoom lab : upcomingLabs) {
                String reminderKey = "LAB_" + lab.getId() + "_" + dateKey;
                if (remindedKeys.contains(reminderKey)) {
                    continue;
                }
                remindedKeys.add(reminderKey);

                String startTimeStr = lab.getScheduledStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                String title = "⏰ Phòng Lab sắp mở: " + lab.getTitle();
                String body = String.format("Phòng thực hành '%s' do %s phụ trách sẽ mở lúc %s. Bấm để tham gia ngay!",
                        lab.getTitle(), lab.getSpeakerName(), startTimeStr);
                String linkUrl = "/labs";

                if (lab.getHostUser() != null) {
                    notificationProducer.sendNotification(
                            lab.getHostUser().getId(),
                            "LAB_LIVE_REMINDER",
                            title,
                            body,
                            linkUrl
                    );
                }
                log.info("Dispatched live lab reminder for lab room {} (title: {})", lab.getId(), lab.getTitle());
            }
        } catch (Exception e) {
            log.error("Error checking lab reminders: {}", e.getMessage(), e);
        }

        // Clean up keys older than today at midnight
        if (now.getHour() == 0 && now.getMinute() < 10) {
            remindedKeys.removeIf(k -> !k.endsWith(dateKey));
        }
    }
}
