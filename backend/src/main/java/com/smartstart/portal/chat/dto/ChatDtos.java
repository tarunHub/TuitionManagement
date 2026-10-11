package com.smartstart.portal.chat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ChatDtos {

    private ChatDtos() {
    }

    public enum ChatRole {
        USER,
        ASSISTANT
    }

    /** One visible turn; the client keeps the conversation and sends it back (tool calls are not part of it). */
    public record ChatMessage(@NotNull ChatRole role, @NotBlank @Size(max = 4000) String content) {
    }

    /** The conversation so far; the last message must be the user's new question. */
    public record ChatRequest(@NotEmpty @Size(max = 20) List<@Valid ChatMessage> messages) {
    }

    /** The app screen an answer's numbers came from, so the admin can check them. */
    public record ChatSource(String label, String link) {
    }

    /**
     * {@code grounded} is false when the model answered without looking anything up (the UI flags such answers);
     * {@code toolsUsed} lists the lookups for transparency.
     */
    public record ChatResponse(String answer, List<ChatSource> sources, boolean grounded, List<String> toolsUsed,
                               String model, long durationMs) {
    }

    public record ChatStatusResponse(boolean enabled, boolean available, String model, String message) {
    }

    /**
     * The admin's rough notes plus whatever is already in the three feedback boxes; at least one must have text.
     * {@code month} is YYYY-MM.
     */
    public record FeedbackDraftRequest(
            @NotNull Long studentId,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}", message = "must be YYYY-MM") String month,
            @Size(max = 4000) String notes,
            @Size(max = 8000) String topicsTaught,
            @Size(max = 8000) String improvementAreas,
            @Size(max = 8000) String achievements
    ) {
    }

    /** A suggested wording for the three feedback boxes; nothing is saved until the admin saves the form. */
    public record FeedbackDraftResponse(String topicsTaught, String improvementAreas, String achievements, String model,
                                        long durationMs) {
    }
}
