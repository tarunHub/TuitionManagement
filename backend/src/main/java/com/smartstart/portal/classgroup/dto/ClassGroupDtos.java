package com.smartstart.portal.classgroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ClassGroupDtos {

    private ClassGroupDtos() {
    }

    public record CreateClassGroupRequest(@NotBlank @Size(max = 100) String name, @Size(max = 2000) String description) {
    }

    public record UpdateClassGroupRequest(@NotBlank @Size(max = 100) String name, @Size(max = 2000) String description) {
    }

    public record ClassGroupResponse(Long id, String name, String description, long activeStudentCount,
                                     Instant createdAt, Instant updatedAt) {
    }
}
