package com.nqd.nqd_lms_be.dto.teacher;

import jakarta.validation.constraints.NotEmpty;
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
public class ReorderItemsRequest {
    @NotEmpty(message = "Ordered IDs cannot be empty")
    private List<UUID> orderedIds;
}
