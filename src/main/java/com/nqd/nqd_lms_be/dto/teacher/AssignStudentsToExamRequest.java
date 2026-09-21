package com.nqd.nqd_lms_be.dto.teacher;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignStudentsToExamRequest {

    @NotNull(message = "Student IDs list cannot be null")
    private List<UUID> studentIds;
}
