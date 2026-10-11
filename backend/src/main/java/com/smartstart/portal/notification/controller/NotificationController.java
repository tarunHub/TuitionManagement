package com.smartstart.portal.notification.controller;

import com.smartstart.portal.common.dto.PageResponse;
import com.smartstart.portal.notification.dto.NotificationDtos.BulkSendResult;
import com.smartstart.portal.notification.dto.NotificationDtos.ChannelStatusResponse;
import com.smartstart.portal.notification.dto.NotificationDtos.CreateNotificationRequest;
import com.smartstart.portal.notification.dto.NotificationDtos.FeeReminderRequest;
import com.smartstart.portal.notification.dto.NotificationDtos.FeedbackEmailRequest;
import com.smartstart.portal.notification.dto.NotificationDtos.NotificationResponse;
import com.smartstart.portal.notification.dto.NotificationDtos.PlaceholderInfo;
import com.smartstart.portal.notification.dto.NotificationDtos.PreviewRequest;
import com.smartstart.portal.notification.dto.NotificationDtos.PreviewResponse;
import com.smartstart.portal.notification.dto.NotificationDtos.ScheduleBroadcastRequest;
import com.smartstart.portal.notification.dto.NotificationDtos.UpdateNotificationRequest;
import com.smartstart.portal.notification.entity.NotificationStatus;
import com.smartstart.portal.notification.entity.NotificationType;
import com.smartstart.portal.notification.service.NotificationService;
import com.smartstart.portal.notification.service.TemplateRenderer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public PageResponse<NotificationResponse> list(@RequestParam(required = false) NotificationStatus status,
                                                   @RequestParam(required = false) NotificationType type,
                                                   @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.list(status, type, pageable);
    }

    @GetMapping("/{id}")
    public NotificationResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Create a notification as a draft, send it now, or schedule it")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse create(@Valid @RequestBody CreateNotificationRequest request) {
        return service.create(request);
    }

    @Operation(summary = "Edit a draft or scheduled notification")
    @PutMapping("/{id}")
    public NotificationResponse update(@PathVariable Long id, @Valid @RequestBody UpdateNotificationRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Render a template/body with sample or selected-recipient data")
    @PostMapping("/preview")
    public PreviewResponse preview(@Valid @RequestBody PreviewRequest request) {
        return service.preview(request);
    }

    @Operation(summary = "Available template placeholders")
    @GetMapping("/placeholders")
    public List<PlaceholderInfo> placeholders() {
        return TemplateRenderer.PLACEHOLDERS;
    }

    @Operation(summary = "Which delivery channels are configured (SMS needs the phone gateway)")
    @GetMapping("/channels")
    public ChannelStatusResponse channels() {
        return service.channels();
    }

    @PostMapping("/{id}/send")
    public NotificationResponse send(@PathVariable Long id) {
        return service.send(id);
    }

    @PostMapping("/{id}/retry")
    public NotificationResponse retry(@PathVariable Long id) {
        return service.retry(id);
    }

    @Operation(summary = "Delete a draft or scheduled notification")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Send FEE_REMINDER emails for the selected unpaid/partial fees")
    @PostMapping("/fee-reminders")
    public BulkSendResult feeReminders(@Valid @RequestBody FeeReminderRequest request) {
        return service.feeReminders(request.feeIds());
    }

    @Operation(summary = "Email a student's monthly feedback to their parent")
    @PostMapping("/feedback-email")
    public NotificationResponse feedbackEmail(@Valid @RequestBody FeedbackEmailRequest request) {
        return service.feedbackEmail(request.studentId(), YearMonth.parse(request.month()));
    }

    @Operation(summary = "Send each class group's parents their weekly timetable")
    @PostMapping("/schedule-broadcast")
    public BulkSendResult scheduleBroadcast(@RequestBody(required = false) ScheduleBroadcastRequest request) {
        return service.scheduleBroadcast(request == null ? null : request.templateId());
    }
}
