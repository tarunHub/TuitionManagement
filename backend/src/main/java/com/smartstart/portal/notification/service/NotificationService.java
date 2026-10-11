package com.smartstart.portal.notification.service;

import com.smartstart.portal.classgroup.entity.ClassGroup;
import com.smartstart.portal.classgroup.repository.ClassGroupRepository;
import com.smartstart.portal.common.dto.PageResponse;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.ConflictException;
import com.smartstart.portal.common.exception.NotFoundException;
import com.smartstart.portal.common.util.EmailListParser;
import com.smartstart.portal.common.util.PhoneNumbers;
import com.smartstart.portal.fee.dto.FeeDtos.FeeRowResponse;
import com.smartstart.portal.fee.entity.FeeStatus;
import com.smartstart.portal.fee.service.FeeService;
import com.smartstart.portal.feedback.entity.MonthlyFeedback;
import com.smartstart.portal.feedback.service.FeedbackService;
import com.smartstart.portal.notification.dto.NotificationDtos.BulkSendResult;
import com.smartstart.portal.notification.dto.NotificationDtos.ChannelStatusResponse;
import com.smartstart.portal.notification.dto.NotificationDtos.ComposeAction;
import com.smartstart.portal.notification.dto.NotificationDtos.ComposeFields;
import com.smartstart.portal.notification.dto.NotificationDtos.NotificationResponse;
import com.smartstart.portal.notification.dto.NotificationDtos.PreviewRequest;
import com.smartstart.portal.notification.dto.NotificationDtos.PreviewResponse;
import com.smartstart.portal.notification.dto.NotificationDtos.RecipientSelection;
import com.smartstart.portal.notification.entity.Notification;
import com.smartstart.portal.notification.entity.NotificationChannel;
import com.smartstart.portal.notification.entity.NotificationStatus;
import com.smartstart.portal.notification.entity.NotificationTemplate;
import com.smartstart.portal.notification.entity.NotificationType;
import com.smartstart.portal.notification.mapper.NotificationMapper;
import com.smartstart.portal.notification.repository.NotificationRepository;
import com.smartstart.portal.notification.repository.NotificationTemplateRepository;
import com.smartstart.portal.notification.service.NotificationSender.NotificationDeliveryException;
import com.smartstart.portal.schedule.entity.ScheduleSlot;
import com.smartstart.portal.schedule.entity.Term;
import com.smartstart.portal.schedule.service.RecipientResolver;
import com.smartstart.portal.schedule.service.ScheduleSlotService;
import com.smartstart.portal.schedule.service.TermService;
import com.smartstart.portal.student.entity.Student;
import com.smartstart.portal.student.repository.StudentRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import com.smartstart.portal.calendar.entity.ClassSession;
import com.smartstart.portal.calendar.service.ClassSessionService;
import com.smartstart.portal.common.util.AppTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NotificationService {

    private static final Set<NotificationStatus> EDITABLE = EnumSet.of(NotificationStatus.DRAFT, NotificationStatus.SCHEDULED);

    private final NotificationRepository repository;
    private final NotificationTemplateRepository templateRepository;
    private final StudentRepository studentRepository;
    private final ClassGroupRepository classGroupRepository;
    private final FeeService feeService;
    private final FeedbackService feedbackService;
    private final ScheduleSlotService slotService;
    private final TermService termService;
    private final ClassSessionService classSessions;
    private final RecipientResolver recipientResolver;
    private final NotificationContextFactory contextFactory;
    private final TemplateRenderer renderer;
    private final NotificationMapper mapper;
    private final Map<NotificationChannel, NotificationSender> senders;
    private final SmsGatewayNotificationSender sms;
    private final Clock clock;

    public NotificationService(NotificationRepository repository, NotificationTemplateRepository templateRepository,
                               StudentRepository studentRepository, ClassGroupRepository classGroupRepository,
                               FeeService feeService, FeedbackService feedbackService, ScheduleSlotService slotService,
                               TermService termService, ClassSessionService classSessions, RecipientResolver recipientResolver,
                               NotificationContextFactory contextFactory, TemplateRenderer renderer,
                               NotificationMapper mapper, List<NotificationSender> senders,
                               SmsGatewayNotificationSender sms, Clock clock) {
        this.repository = repository;
        this.templateRepository = templateRepository;
        this.studentRepository = studentRepository;
        this.classGroupRepository = classGroupRepository;
        this.feeService = feeService;
        this.feedbackService = feedbackService;
        this.slotService = slotService;
        this.termService = termService;
        this.classSessions = classSessions;
        this.recipientResolver = recipientResolver;
        this.contextFactory = contextFactory;
        this.renderer = renderer;
        this.mapper = mapper;
        this.senders = senders.stream().collect(Collectors.toMap(NotificationSender::channel, Function.identity()));
        this.sms = sms;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- CRUD

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(NotificationStatus status, NotificationType type, Pageable pageable) {
        Specification<Notification> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (status != null) {
                p.add(cb.equal(root.get("status"), status));
            }
            if (type != null) {
                p.add(cb.equal(root.get("type"), type));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        Pageable safe = PageRequest.of(pageable.getPageNumber(), Math.min(Math.max(pageable.getPageSize(), 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.of(repository.findAll(spec, safe), mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(Long id) {
        return mapper.toResponse(require(id));
    }

    @Transactional
    public NotificationResponse create(ComposeFields request) {
        Notification n = new Notification();
        applyCompose(n, request);
        repository.saveAndFlush(n);
        if (request.action() == ComposeAction.SEND_NOW) {
            deliver(n);
        }
        return mapper.toResponse(n);
    }

    @Transactional
    public NotificationResponse update(Long id, ComposeFields request) {
        Notification n = require(id);
        if (!EDITABLE.contains(n.getStatus())) {
            throw new ConflictException("NOT_EDITABLE", "Only draft or scheduled notifications can be edited");
        }
        applyCompose(n, request);
        repository.saveAndFlush(n);
        if (request.action() == ComposeAction.SEND_NOW) {
            deliver(n);
        }
        return mapper.toResponse(n);
    }

    @Transactional
    public void delete(Long id) {
        Notification n = require(id);
        if (!EDITABLE.contains(n.getStatus())) {
            throw new ConflictException("NOT_DELETABLE", "Only draft or scheduled notifications can be deleted");
        }
        repository.delete(n);
    }

    @Transactional
    public NotificationResponse send(Long id) {
        Notification n = require(id);
        if (n.getStatus() == NotificationStatus.SENT) {
            throw new ConflictException("ALREADY_SENT", "This notification has already been sent");
        }
        deliver(n);
        return mapper.toResponse(n);
    }

    @Transactional
    public NotificationResponse retry(Long id) {
        Notification n = require(id);
        if (n.getStatus() != NotificationStatus.FAILED) {
            throw new ConflictException("NOT_FAILED", "Only failed notifications can be retried");
        }
        deliver(n);
        return mapper.toResponse(n);
    }

    /** Sends a scheduled notification if it is still scheduled (called by the dispatch job, one transaction each). */
    @Transactional
    public void sendIfStillScheduled(Long id) {
        repository.findById(id)
                .filter(n -> n.getStatus() == NotificationStatus.SCHEDULED)
                .ifPresent(this::deliver);
    }

    @Transactional(readOnly = true)
    public List<Long> dueScheduledIds() {
        return repository.findDueIds(NotificationStatus.SCHEDULED, clock.instant());
    }

    public ChannelStatusResponse channels() {
        return new ChannelStatusResponse(true, sms.isConfigured(), sms.isConfigured() ? null
                : "SMS is off. Set SMS_ENABLED=true and SMS_GATEWAY_URL (the Android SMS Gateway app) in .env.");
    }

    // ---------------------------------------------------------------- Preview

    @Transactional(readOnly = true)
    public PreviewResponse preview(PreviewRequest request) {
        String subject = request.subject();
        String body = request.body();
        if (request.templateId() != null) {
            NotificationTemplate t = requireTemplate(request.templateId());
            subject = subject == null || subject.isBlank() ? t.getSubject() : subject;
            body = body == null || body.isBlank() ? t.getBody() : body;
        }
        YearMonth month = request.month() == null ? YearMonth.now(clock) : YearMonth.parse(request.month());
        List<String> recipients = List.of();
        Map<String, String> ctx;
        if (request.recipients() != null) {
            Resolved resolved = resolve(request.recipients(), channelOf(request.channel()));
            recipients = resolved.addresses();
            ctx = context(resolved, month);
            if (resolved.student() == null) {
                // Show realistic per-parent values in the preview even for broadcasts.
                Map<String, String> sample = contextFactory.sample(month);
                sample.putAll(Map.of("scheduleTable", ctx.get("scheduleTable"), "className",
                        resolved.group() != null ? ctx.get("className") : sample.get("className")));
                ctx = sample;
            }
        } else {
            ctx = contextFactory.sample(month);
            ctx.put("scheduleTable", contextFactory.scheduleTable(slotService.activeTermSlots(), true));
        }
        return new PreviewResponse(renderer.render(subject, ctx), renderer.render(body, ctx), recipients);
    }

    // ---------------------------------------------------------------- Specialised flows

    /** One FEE_REMINDER per selected Unpaid/Partial fee, rendered per student and sent immediately. */
    @Transactional
    public BulkSendResult feeReminders(List<Long> feeIds) {
        List<FeeRowResponse> rows = feeService.rows(feeIds).stream()
                .filter(r -> r.status() == FeeStatus.UNPAID || r.status() == FeeStatus.PARTIAL)
                .toList();
        if (rows.isEmpty()) {
            throw new BadRequestException("NOTHING_TO_REMIND", "None of the selected fees are unpaid or partially paid");
        }
        Map<Long, Student> students = studentRepository.findAllById(rows.stream().map(FeeRowResponse::studentId).toList())
                .stream().collect(Collectors.toMap(Student::getId, Function.identity()));
        NotificationTemplate template = templateRepository.findByNameIgnoreCase(BuiltInTemplates.FEE_REMINDER.templateName()).orElse(null);
        NotificationTemplate smsTemplate = templateRepository.findByNameIgnoreCase(BuiltInTemplates.FEE_REMINDER_SMS.templateName()).orElse(null);
        List<Notification> created = new ArrayList<>();
        for (FeeRowResponse row : rows) {
            Student s = students.get(row.studentId());
            Map<String, String> ctx = contextFactory.forFee(row, s.getGuardianName());
            if (s.getEmail() != null) {
                created.add(createAndSend(NotificationChannel.EMAIL, NotificationType.FEE_REMINDER, template, BuiltInTemplates.FEE_REMINDER, ctx, List.of(s.getEmail())));
            }
            if (sms.isConfigured()) {
                RecipientResolver.smsNumber(s, sms.defaultCountryCode()).ifPresent(phone -> created.add(createAndSend(
                        NotificationChannel.SMS, NotificationType.FEE_REMINDER, smsTemplate, BuiltInTemplates.FEE_REMINDER_SMS, ctx, List.of(phone))));
            }
        }
        return result(created);
    }

    /** FEEDBACK email to one student's parent with that month's feedback. */
    @Transactional
    public NotificationResponse feedbackEmail(Long studentId, YearMonth month) {
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new NotFoundException("Student", studentId));
        if (student.getEmail() == null) {
            throw new BadRequestException("NO_EMAIL", student.getFullName() + " has no parent email on file");
        }
        MonthlyFeedback feedback = feedbackService.require(studentId, month);
        Map<String, String> ctx = contextFactory.forStudent(student, month);
        StringBuilder text = new StringBuilder("Topics taught:\n").append(feedback.getTopicsTaught());
        if (feedback.getImprovementAreas() != null) {
            text.append("\n\nAreas to improve:\n").append(feedback.getImprovementAreas());
        }
        if (feedback.getAchievements() != null) {
            text.append("\n\nAchievements:\n").append(feedback.getAchievements());
        }
        ctx.put("feedback", text.toString());
        NotificationTemplate template = templateRepository.findByNameIgnoreCase(BuiltInTemplates.FEEDBACK.templateName()).orElse(null);
        Notification n = createAndSend(NotificationChannel.EMAIL, NotificationType.FEEDBACK, template, BuiltInTemplates.FEEDBACK, ctx, List.of(student.getEmail()));
        return mapper.toResponse(n);
    }

    /** One SCHEDULE email per class group of the active term, containing that group's weekly timetable. */
    @Transactional
    public BulkSendResult scheduleBroadcast(Long templateId) {
        List<ScheduleSlot> slots = slotService.activeTermSlots();
        if (slots.isEmpty()) {
            throw new BadRequestException("NO_SLOTS", "The active term has no schedule slots");
        }
        NotificationTemplate template = templateId != null ? requireTemplate(templateId)
                : templateRepository.findByNameIgnoreCase(BuiltInTemplates.WEEKLY_SCHEDULE.templateName()).orElse(null);
        Map<Long, List<String>> groupEmails = recipientResolver.activeParentEmailsByGroup(RecipientResolver.groupIds(slots));
        Map<ClassGroup, List<ScheduleSlot>> byGroup = new LinkedHashMap<>();
        slots.forEach(s -> byGroup.computeIfAbsent(s.getClassGroup(), k -> new ArrayList<>()).add(s));

        List<Notification> created = new ArrayList<>();
        for (Map.Entry<ClassGroup, List<ScheduleSlot>> e : byGroup.entrySet()) {
            List<String> recipients = recipientResolver.forGroup(e.getKey().getId(), e.getValue(), groupEmails);
            if (recipients.isEmpty()) {
                continue;
            }
            Map<String, String> ctx = contextFactory.base(YearMonth.now(clock));
            ctx.put("className", e.getKey().getName());
            ctx.put("scheduleTable", contextFactory.scheduleTable(e.getValue(), false));
            created.add(createAndSend(NotificationChannel.EMAIL, NotificationType.SCHEDULE, template, BuiltInTemplates.WEEKLY_SCHEDULE, ctx, recipients));
        }
        if (created.isEmpty()) {
            throw new BadRequestException("NO_RECIPIENTS", "No class group in the active term has any recipients");
        }
        return result(created);
    }

    /**
     * One SCHEDULE email per batch with that batch's class dates for the month from the class calendar, sent to the
     * parents of its active students. {@code groupIds} empty means every batch with classes that month.
     */
    @Transactional
    public BulkSendResult monthlySchedule(YearMonth month, List<Long> groupIds) {
        Map<ClassGroup, List<ClassSession>> byGroup = new LinkedHashMap<>();
        classSessions.sessions(month, null).stream()
                .filter(s -> groupIds == null || groupIds.isEmpty() || groupIds.contains(s.getClassGroup().getId()))
                .forEach(s -> byGroup.computeIfAbsent(s.getClassGroup(), k -> new ArrayList<>()).add(s));
        if (byGroup.isEmpty()) {
            throw new BadRequestException("NO_CLASSES", "No classes are on the calendar for " + AppTime.monthLabel(month)
                    + (groupIds == null || groupIds.isEmpty() ? "" : " for the selected batch"));
        }
        NotificationTemplate template = templateRepository.findByNameIgnoreCase(BuiltInTemplates.MONTHLY_SCHEDULE.templateName()).orElse(null);
        Map<Long, List<String>> groupEmails = recipientResolver.activeParentEmailsByGroup(
                byGroup.keySet().stream().map(ClassGroup::getId).toList());
        List<Notification> created = new ArrayList<>();
        List<String> without = new ArrayList<>();
        for (Map.Entry<ClassGroup, List<ClassSession>> e : byGroup.entrySet()) {
            List<String> recipients = groupEmails.getOrDefault(e.getKey().getId(), List.of());
            if (recipients.isEmpty()) {
                without.add(e.getKey().getName());
                continue;
            }
            Map<String, String> ctx = contextFactory.base(month);
            ctx.put("className", e.getKey().getName());
            ctx.put("scheduleTable", contextFactory.sessionTable(e.getValue()));
            created.add(createAndSend(NotificationChannel.EMAIL, NotificationType.SCHEDULE, template, BuiltInTemplates.MONTHLY_SCHEDULE, ctx, recipients));
        }
        if (created.isEmpty()) {
            throw new BadRequestException("NO_RECIPIENTS", "No parent email addresses on file for " + String.join(", ", without));
        }
        return result(created);
    }

    /** Reminders for every class occurring on {@code date} (the class-reminder job passes tomorrow). */
    @Transactional
    public BulkSendResult classReminders(LocalDate date) {
        Term term = termService.activeTerm().orElse(null);
        if (term == null || !term.contains(date)) {
            return new BulkSendResult(0, 0, 0, List.of());
        }
        List<ScheduleSlot> slots = slotService.activeTermSlots().stream()
                .filter(s -> s.getDayOfWeek().toDayOfWeek() == date.getDayOfWeek())
                .toList();
        if (slots.isEmpty()) {
            return new BulkSendResult(0, 0, 0, List.of());
        }
        NotificationTemplate template = templateRepository.findByNameIgnoreCase(BuiltInTemplates.CLASS_REMINDER.templateName()).orElse(null);
        NotificationTemplate smsTemplate = templateRepository.findByNameIgnoreCase(BuiltInTemplates.CLASS_REMINDER_SMS.templateName()).orElse(null);
        Map<Long, List<String>> groupEmails = recipientResolver.activeParentEmailsByGroup(RecipientResolver.groupIds(slots));
        Map<Long, List<String>> groupPhones = sms.isConfigured()
                ? recipientResolver.activeParentPhonesByGroup(RecipientResolver.groupIds(slots), sms.defaultCountryCode())
                : Map.of();
        List<Notification> created = new ArrayList<>();
        for (ScheduleSlot slot : slots) {
            Map<String, String> ctx = contextFactory.base(YearMonth.from(date));
            ctx.put("className", slot.getClassGroup().getName());
            ctx.put("date", contextFactory.formatDate(date));
            ctx.put("time", slot.getStartTime() + "–" + slot.getEndTime());
            ctx.put("location", slot.getLocation() == null || slot.getLocation().isBlank() ? "the usual location" : slot.getLocation());
            ctx.put("scheduleTable", contextFactory.scheduleTable(List.of(slot), false));
            List<String> recipients = recipientResolver.forSlot(slot, groupEmails);
            if (!recipients.isEmpty()) {
                created.add(createAndSend(NotificationChannel.EMAIL, NotificationType.SCHEDULE, template, BuiltInTemplates.CLASS_REMINDER, ctx, recipients));
            }
            List<String> phones = groupPhones.getOrDefault(slot.getClassGroup().getId(), List.of());
            if (!phones.isEmpty()) {
                created.add(createAndSend(NotificationChannel.SMS, NotificationType.SCHEDULE, smsTemplate, BuiltInTemplates.CLASS_REMINDER_SMS, ctx, phones));
            }
        }
        return result(created);
    }

    /** Fee reminders for the given month's Unpaid/Partial fees (the monthly job passes the previous month). */
    @Transactional
    public BulkSendResult feeRemindersForMonth(YearMonth month) {
        List<Long> ids = feeService.rowsForMonth(month).stream()
                .filter(r -> r.status() == FeeStatus.UNPAID || r.status() == FeeStatus.PARTIAL)
                .map(FeeRowResponse::id)
                .toList();
        if (ids.isEmpty()) {
            return new BulkSendResult(0, 0, 0, List.of());
        }
        return feeReminders(ids);
    }

    // ---------------------------------------------------------------- internals

    private void applyCompose(Notification n, ComposeFields request) {
        NotificationTemplate template = request.templateId() == null ? null : requireTemplate(request.templateId());
        YearMonth month = request.month() == null ? YearMonth.now(clock) : YearMonth.parse(request.month());
        NotificationChannel channel = channelOf(request.channel());
        if (channel == NotificationChannel.SMS && !sms.isConfigured() && request.action() != ComposeAction.DRAFT) {
            throw new BadRequestException("SMS_NOT_CONFIGURED", "SMS is not configured; save as a draft or switch to email", "channel");
        }
        Resolved resolved = resolve(request.recipients(), channel);
        if (resolved.addresses().isEmpty() && request.action() != ComposeAction.DRAFT) {
            throw new BadRequestException("NO_RECIPIENTS", channel == NotificationChannel.SMS
                    ? "None of the selected parents have opted in to SMS with a valid phone number"
                    : "The selected recipients do not include any email address", "recipients");
        }
        Map<String, String> ctx = context(resolved, month);
        n.setChannel(channel);
        n.setType(request.type());
        n.setTemplate(template);
        n.setSubject(truncate(renderer.render(request.subject().trim(), ctx), 200));
        n.setBody(renderer.render(request.body(), ctx));
        n.assignRecipients(resolved.addresses());
        n.setErrorMessage(null);
        switch (request.action()) {
            case DRAFT -> {
                n.setStatus(NotificationStatus.DRAFT);
                n.setScheduledAt(null);
            }
            case SCHEDULE -> {
                Instant at = request.scheduledAt();
                if (at == null || !at.isAfter(clock.instant())) {
                    throw new BadRequestException("VALIDATION_FAILED", "must be in the future", "scheduledAt");
                }
                n.setStatus(NotificationStatus.SCHEDULED);
                n.setScheduledAt(at);
            }
            case SEND_NOW -> n.setScheduledAt(null);
        }
    }

    private Map<String, String> context(Resolved resolved, YearMonth month) {
        Map<String, String> ctx;
        if (resolved.student() != null) {
            Student s = resolved.student();
            ctx = contextFactory.forStudent(s, month);
            feeService.rowsForMonth(month).stream()
                    .filter(r -> r.studentId().equals(s.getId()))
                    .findFirst()
                    .ifPresent(fee -> ctx.putAll(contextFactory.forFee(fee, s.getGuardianName())));
        } else {
            ctx = contextFactory.base(month);
        }
        List<ScheduleSlot> slots = slotService.activeTermSlots();
        ClassGroup group = resolved.group() != null ? resolved.group()
                : resolved.student() != null ? resolved.student().getClassGroup() : null;
        if (group != null) {
            ctx.put("className", group.getName());
            ctx.put("scheduleTable", contextFactory.scheduleTable(
                    slots.stream().filter(s -> s.getClassGroup().getId().equals(group.getId())).toList(), false));
        } else {
            ctx.put("scheduleTable", contextFactory.scheduleTable(slots, true));
        }
        return ctx;
    }

    /** Email addresses or, for SMS, E.164 numbers of opted-in parents (others are skipped, the preview shows the count). */
    private Resolved resolve(RecipientSelection selection, NotificationChannel channel) {
        List<Student> picked = new ArrayList<>();
        Student single = null;
        ClassGroup group = null;
        switch (selection.mode()) {
            case ALL_ACTIVE -> picked.addAll(studentRepository.findAllActive());
            case CLASS_GROUPS -> {
                List<Long> ids = selection.classGroupIds() == null ? List.of() : selection.classGroupIds();
                if (ids.isEmpty()) {
                    throw new BadRequestException("VALIDATION_FAILED", "Select at least one class group", "recipients.classGroupIds");
                }
                List<ClassGroup> groups = classGroupRepository.findAllById(ids);
                if (groups.size() != Set.copyOf(ids).size()) {
                    throw new BadRequestException("VALIDATION_FAILED", "Unknown class group selected", "recipients.classGroupIds");
                }
                picked.addAll(studentRepository.findActiveInGroups(ids));
                if (groups.size() == 1) {
                    group = groups.getFirst();
                }
            }
            case STUDENTS -> {
                List<Long> ids = selection.studentIds() == null ? List.of() : selection.studentIds();
                if (ids.isEmpty()) {
                    throw new BadRequestException("VALIDATION_FAILED", "Select at least one student", "recipients.studentIds");
                }
                List<Student> students = studentRepository.findAllWithGroupByIdIn(ids);
                if (students.size() != Set.copyOf(ids).size()) {
                    throw new BadRequestException("VALIDATION_FAILED", "Unknown student selected", "recipients.studentIds");
                }
                picked.addAll(students);
                if (students.size() == 1) {
                    single = students.getFirst();
                }
            }
            case NONE -> {
                // extra emails / phones only
            }
        }
        List<String> addresses = new ArrayList<>();
        if (channel == NotificationChannel.SMS) {
            picked.forEach(s -> RecipientResolver.smsNumber(s, sms.defaultCountryCode()).ifPresent(addresses::add));
            addresses.addAll(PhoneNumbers.parse(selection.extraPhones(), sms.defaultCountryCode(), "recipients.extraPhones"));
        } else {
            picked.forEach(s -> addresses.add(s.getEmail()));
            addresses.addAll(EmailListParser.parse(selection.extraEmails(), "recipients.extraEmails"));
        }
        return new Resolved(EmailListParser.dedupe(addresses), single, group);
    }

    private static NotificationChannel channelOf(NotificationChannel requested) {
        return requested == null ? NotificationChannel.EMAIL : requested;
    }

    private Notification createAndSend(NotificationChannel channel, NotificationType type, NotificationTemplate template,
                                       BuiltInTemplates fallback, Map<String, String> ctx, List<String> recipients) {
        Notification n = new Notification();
        n.setChannel(channel);
        n.setType(type);
        n.setTemplate(template);
        n.setSubject(truncate(renderer.render(template != null ? template.getSubject() : fallback.subject(), ctx), 200));
        n.setBody(renderer.render(template != null ? template.getBody() : fallback.body(), ctx));
        n.assignRecipients(recipients);
        repository.saveAndFlush(n);
        deliver(n);
        return n;
    }

    /** Sends through the channel's sender and records SENT or FAILED (with the error) — never throws. */
    private void deliver(Notification n) {
        NotificationSender sender = senders.get(n.getChannel());
        try {
            if (sender == null) {
                throw new NotificationDeliveryException("No sender configured for channel " + n.getChannel(), null);
            }
            sender.send(n);
            n.setStatus(NotificationStatus.SENT);
            n.setSentAt(clock.instant());
            n.setErrorMessage(null);
        } catch (NotificationDeliveryException e) {
            log.warn("Notification {} failed: {}", n.getId(), e.getMessage());
            n.setStatus(NotificationStatus.FAILED);
            n.setErrorMessage(e.getMessage());
        }
    }

    private BulkSendResult result(List<Notification> created) {
        int sent = (int) created.stream().filter(n -> n.getStatus() == NotificationStatus.SENT).count();
        return new BulkSendResult(created.size(), sent, created.size() - sent, created.stream().map(mapper::toResponse).toList());
    }

    private Notification require(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Notification", id));
    }

    private NotificationTemplate requireTemplate(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("VALIDATION_FAILED", "Template " + id + " does not exist", "templateId"));
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private record Resolved(List<String> addresses, Student student, ClassGroup group) {
    }
}
