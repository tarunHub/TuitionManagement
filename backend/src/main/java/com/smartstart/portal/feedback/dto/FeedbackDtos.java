package com.smartstart.portal.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class FeedbackDtos {

    private FeedbackDtos() {
    }

    public record UpsertFeedbackRequest(
            @NotBlank @Size(max = 8000) String topicsTaught,
            @Size(max = 8000) String improvementAreas,
            @Size(max = 8000) String achievements
    ) {
    }

    /** One row per active student; {@code id} is null when nothing has been entered for the month yet. */
    public record FeedbackRowResponse(
            Long id,
            String month,
            Long studentId,
            String studentCode,
            String fullName,
            String grade,
            String classGroupName,
            String guardianName,
            String topicsTaught,
            String improvementAreas,
            String achievements,
            String previousMonthTopics,
            Instant updatedAt
    ) {
    }

    public record FeedbackResponse(Long id, String month, Long studentId, String topicsTaught, String improvementAreas,
                                   String achievements, Instant createdAt, Instant updatedAt) {
    }

    public record CopyTopicsResult(int copied) {
    }
}
