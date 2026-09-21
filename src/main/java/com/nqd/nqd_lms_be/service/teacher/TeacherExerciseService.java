package com.nqd.nqd_lms_be.service.teacher;

import com.nqd.nqd_lms_be.dto.exercise.TeacherExerciseQuestionItemRequest;
import com.nqd.nqd_lms_be.dto.exercise.TeacherExerciseRequest;
import com.nqd.nqd_lms_be.dto.exercise.TeacherExerciseResponse;
import com.nqd.nqd_lms_be.entity.enums.ExerciseStatus;

import java.util.List;
import java.util.UUID;

public interface TeacherExerciseService {
    List<TeacherExerciseResponse> getExercises(
            UUID lessonId,
            UUID courseId,
            UUID subjectId,
            ExerciseStatus status,
            String keyword,
            UUID teacherId
    );

    TeacherExerciseResponse getExerciseById(UUID id, UUID teacherId);

    TeacherExerciseResponse createExercise(TeacherExerciseRequest request, UUID teacherId);

    TeacherExerciseResponse updateExercise(UUID id, TeacherExerciseRequest request, UUID teacherId);

    TeacherExerciseResponse publishExercise(UUID id, UUID teacherId);

    TeacherExerciseResponse archiveExercise(UUID id, UUID teacherId);

    void deleteExercise(UUID id, UUID teacherId);

    TeacherExerciseResponse addQuestionToExercise(UUID id, TeacherExerciseQuestionItemRequest itemRequest, UUID teacherId);

    TeacherExerciseResponse removeQuestionFromExercise(UUID id, UUID questionId, UUID teacherId);
}
