package com.nqd.nqd_lms_be.service.classroom;

import com.nqd.nqd_lms_be.entity.ClassroomSchedule;
import com.nqd.nqd_lms_be.entity.ClassroomStudent;
import com.nqd.nqd_lms_be.entity.LabRoom;
import com.nqd.nqd_lms_be.entity.enums.ClassEnrollmentStatus;
import com.nqd.nqd_lms_be.entity.enums.LabStatus;
import com.nqd.nqd_lms_be.repository.ClassroomScheduleRepository;
import com.nqd.nqd_lms_be.repository.ClassroomStudentRepository;
import com.nqd.nqd_lms_be.repository.LabRoomRepository;
import com.nqd.nqd_lms_be.repository.NotificationRepository;
import com.nqd.nqd_lms_be.service.notification.KafkaNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClassroomLiveReminderScheduler {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ClassroomScheduleRepository scheduleRepository;
    private final ClassroomStudentRepository studentRepository;
    private final LabRoomRepository labRoomRepository;
    private final NotificationRepository notificationRepository;
    private final KafkaNotificationProducer notificationProducer;

    // In-memory cache to prevent duplicate reminders on the same day: key = type_id_yyyy-MM-dd
    private final Set<String> remindedKeys = ConcurrentHashMap.newKeySet();

    /**
     * Check every minute (in VN +7) for upcoming classes & labs starting in the next 15 minutes.
     * Running every minute ensures precision: teachers and students are alerted promptly 15 minutes before class.
     */
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional(readOnly = true)
    public void checkUpcomingClassesAndRemind() {
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalTime now = LocalTime.now(VN_ZONE);
        String dayOfWeek = today.getDayOfWeek().name();
        String dateKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // 1. Remind Classroom Schedules (Online 100ms, Lab, Exam)
        try {
            List<ClassroomSchedule> todaySchedules = scheduleRepository.findByDayOfWeekWithClassroomAndTeacher(dayOfWeek);
            for (ClassroomSchedule schedule : todaySchedules) {
                if (schedule.getClassroom() == null) continue;

                String reminderKey = "CLASS_" + schedule.getId() + "_" + dateKey;
                if (remindedKeys.contains(reminderKey)) {
                    continue;
                }

                try {
                    String[] parts = schedule.getStartTime().split(":");
                    int hour = Integer.parseInt(parts[0].trim());
                    int minute = Integer.parseInt(parts[1].trim());

                    int nowMinutes = now.getHour() * 60 + now.getMinute();
                    int startMinutes = hour * 60 + minute;
                    int diffMinutes = startMinutes - nowMinutes;

                    // Trigger notification when starting within 15 minutes (diff between 0 and 15)
                    if (diffMinutes >= 0 && diffMinutes <= 15) {
                        String linkUrl = "/classrooms/" + schedule.getClassroom().getId() + "?tab=schedule";

                        // Avoid duplicate push if server restarted today
                        if (schedule.getClassroom().getTeacher() != null) {
                            boolean alreadySent = notificationRepository.existsByUserIdAndTypeAndLinkUrlAndCreatedAtAfter(
                                    schedule.getClassroom().getTeacher().getId(),
                                    "CLASS_LIVE_REMINDER",
                                    linkUrl,
                                    today.atStartOfDay()
                            );
                            if (alreadySent) {
                                remindedKeys.add(reminderKey);
                                continue;
                            }
                        }

                        remindedKeys.add(reminderKey);

                        String sessionType = schedule.getSessionType() != null ? schedule.getSessionType() : "ONLINE_100MS";
                        String title;
                        String body;

                        if ("LAB".equalsIgnoreCase(sessionType)) {
                            title = "⏰ Buổi thực hành Lab ảo sắp bắt đầu (15 phút nữa)!";
                            body = String.format("Buổi thực hành '%s' của lớp %s sẽ bắt đầu lúc %s. Hãy chuẩn bị tham gia!",
                                    schedule.getTitle(), schedule.getClassroom().getName(), schedule.getStartTime());
                        } else if ("EXAM".equalsIgnoreCase(sessionType)) {
                            title = "⏰ Buổi luyện đề trực tiếp sắp bắt đầu (15 phút nữa)!";
                            body = String.format("Buổi luyện đề '%s' của lớp %s sẽ bắt đầu lúc %s. Hãy sẵn sàng vào làm bài nhé!",
                                    schedule.getTitle(), schedule.getClassroom().getName(), schedule.getStartTime());
                        } else {
                            title = "⏰ Lớp học online sắp bắt đầu (15 phút nữa)!";
                            body = String.format("Buổi học '%s' của lớp %s sẽ bắt đầu lúc %s qua phòng học trực tuyến 100ms. Hãy chuẩn bị vào lớp nhé!",
                                    schedule.getTitle(), schedule.getClassroom().getName(), schedule.getStartTime());
                        }

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
                        List<ClassroomStudent> students = studentRepository.findByClassroomIdAndStatusWithStudent(
                                schedule.getClassroom().getId(), ClassEnrollmentStatus.ENROLLED);
                        for (ClassroomStudent cs : students) {
                            if (cs.getStudent() != null) {
                                notificationProducer.sendNotification(
                                        cs.getStudent().getId(),
                                        "CLASS_LIVE_REMINDER",
                                        title,
                                        body,
                                        linkUrl
                                );
                            }
                        }

                        log.info("Dispatched 15-min live class reminder for schedule {} (class: {}) to teacher and {} students",
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
            LocalDateTime nowDt = LocalDateTime.now(VN_ZONE);
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
        if (now.getHour() == 0 && now.getMinute() == 0) {
            remindedKeys.removeIf(k -> !k.endsWith(dateKey));
        }
    }
}
