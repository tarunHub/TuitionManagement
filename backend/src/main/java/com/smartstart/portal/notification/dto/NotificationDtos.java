package com.smartstart.portal.notification.dto;

import com.smartstart.portal.notification.entity.NotificationChannel;
import com.smartstart.portal.notification.entity.NotificationStatus;
import com.smartstart.portal.notification.entity.NotificationType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class NotificationDtos {

    public static final String MONTH_REGEX = "^\\d{4}-(0[1-9]|1[0-2])$";

    private NotificationDtos() {
    }

    public enum RecipientMode {
        /** Parents of every active student. */
        ALL_ACTIVE,
        /** Parents of active students in the selected class groups. */
        CLASS_GROUPS,
        /** Parents of the selected students. */
        STUDENTS,
        /** Only the extra emails (or, for SMS, the extra phone numbers). */
        NONE
    }

    public enum ComposeAction {
        DRAFT,
        SEND_NOW,
        SCHEDULE
    }

    public record RecipientSelection(
            @NotNull RecipientMode mode,
            List<Long> classGroupIds,
            List<Long> studentIds,
            @Size(max = 4000) String extraEmails,
            /** SMS only: comma/semicolon/newline separated numbers. */
            @Size(max = 4000) String extraPhones
    ) {
    }

    public record CreateNotificationRequest(
            @NotNull NotificationType type,
            Long templateId,
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 20000) String body,
            /** EMAIL when omitted. */
            NotificationChannel channel,
            @NotNull @Valid RecipientSelection recipients,
            @NotNull ComposeAction action,
            Instant scheduledAt,
            @Pattern(regexp = MONTH_REGEX, message = "must be YYYY-MM") String month
    ) implements ComposeFields {
    }

    public record UpdateNotificationRequest(
            @NotNull NotificationType type,
            Long templateId,
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 20000) String body,
            /** EMAIL when omitted. */
            NotificationChannel channel,
            @NotNull @Valid RecipientSelection recipients,
            @NotNull ComposeAction action,
            Instant scheduledAt,
            @Pattern(regexp = MONTH_REGEX, message = "must be YYYY-MM") String month
    ) implements ComposeFields {
    }

    public interface ComposeFields {
        NotificationType type();

        Long templateId();

        String subject();

        String body();

        NotificationChannel channel();

        RecipientSelection recipients();

        ComposeAction action();

        Instant scheduledAt();

        String month();
    }

    public record PreviewRequest(
            Long templateId,
            @Size(max = 200) String subject,
            @Size(max = 20000) String body,
            NotificationChannel channel,
            @Valid RecipientSelection recipients,
            @Pattern(regexp = MONTH_REGEX, message = "must be YYYY-MM") String month
    ) {
    }

    public record PreviewResponse(String subject, String body, List<String> recipients) {
    }

    /** Which delivery channels are usable right now; SMS needs the phone gateway configured. */
    public record ChannelStatusResponse(boolean email, boolean sms, String smsMessage) {
    }

    public record NotificationResponse(
            Long id,
            NotificationType type,
            NotificationChannel channel,
            String subject,
            String body,
            List<String> recipients,
            int recipientCount,
            NotificationStatus status,
            Instant scheduledAt,
            Instant sentAt,
            String errorMessage,
            Long templateId,
            Instant createdAt,
            Instant updatedAt
    ) {
    }

    public record FeeReminderRequest(@NotEmpty List<Long> feeIds) {
    }

    public record FeedbackEmailRequest(
            @NotNull Long studentId,
            @NotBlank @Pattern(regexp = MONTH_REGEX, message = "must be YYYY-MM") String month
    ) {
    }

    public record ScheduleBroadcastRequest(Long templateId) {
    }

    /** One email or SMS ready to go, shown to the admin to review (and edit) before sending. */
    public record OutgoingMessage(Long studentId, String studentName, String className, NotificationChannel channel,
                                  NotificationType type, Long templateId, String recipient, String subject, String body) {
    }

    /** {@code skipped} lists students with no parent email or SMS opt-in, who would get nothing. */
    public record MessagePreview(List<OutgoingMessage> messages, List<String> skipped) {
    }

    public record MessageToSend(
            @NotNull Long studentId,
            @NotNull NotificationChannel channel,
            @NotNull NotificationType type,
            Long templateId,
            @Size(max = 200) String subject,
            @NotBlank @Size(max = 20000) String body
    ) {
    }

    public record SendReviewedRequest(@NotEmpty @Size(max = 500) List<@Valid MessageToSend> messages) {
    }

    public record BulkSendResult(int created, int sent, int failed, List<NotificationResponse> notifications) {
    }

    public record CreateTemplateRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull NotificationType type,
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 20000) String body
    ) {
    }

    public record UpdateTemplateRequest(
            @NotBlank @Size(max = 100) String name,
            @NotNull NotificationType type,
            @NotBlank @Size(max = 200) String subject,
            @NotBlank @Size(max = 20000) String body
    ) {
    }

    public record TemplateResponse(Long id, String name, NotificationType type, String subject, String body,
                                   Instant createdAt, Instant updatedAt) {
    }

    public record PlaceholderInfo(String name, String description) {
    }
}
