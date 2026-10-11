package com.smartstart.portal.schedule.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.FeatureNotConfiguredException;
import com.smartstart.portal.common.util.AppTime;
import com.smartstart.portal.config.AppProperties;
import com.smartstart.portal.schedule.dto.ScheduleDtos.GoogleStatusResponse;
import com.smartstart.portal.schedule.dto.ScheduleDtos.SyncResult;
import com.smartstart.portal.schedule.entity.ScheduleSlot;
import com.smartstart.portal.schedule.entity.Term;
import com.smartstart.portal.schedule.repository.ScheduleSlotRepository;
import com.smartstart.portal.schedule.repository.TermRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Optional, feature-flagged Google Calendar sync. Each slot becomes one weekly recurring event between the
 * term's start and end dates (rule 9). When disabled or unconfigured every call fails with 501.
 */
@Slf4j
@Service
public class CalendarSyncService {

    private static final DateTimeFormatter RRULE_UNTIL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final String DEFAULT_CALENDAR = "Smart Start Classes";

    private final AppProperties.Google config;
    private final TermRepository termRepository;
    private final ScheduleSlotRepository slotRepository;
    private final RecipientResolver recipientResolver;

    public CalendarSyncService(AppProperties props, TermRepository termRepository,
                               ScheduleSlotRepository slotRepository, RecipientResolver recipientResolver) {
        this.config = props.google();
        this.termRepository = termRepository;
        this.slotRepository = slotRepository;
        this.recipientResolver = recipientResolver;
    }

    public boolean isEnabled() {
        return config != null && config.enabled() && config.credentialsPath() != null
                && !config.credentialsPath().isBlank() && Files.isReadable(Path.of(config.credentialsPath()));
    }

    public GoogleStatusResponse status() {
        if (isEnabled()) {
            return new GoogleStatusResponse(true, "Google Calendar sync is configured");
        }
        return new GoogleStatusResponse(false, disabledMessage());
    }

    @Transactional
    public SyncResult syncActiveTerm() {
        requireEnabled();
        Term term = termRepository.findFirstByActiveTrue()
                .orElseThrow(() -> new BadRequestException("NO_ACTIVE_TERM", "Configure an active term before syncing"));
        Calendar client = client();
        String calendarName = term.getCalendarName() == null ? DEFAULT_CALENDAR : term.getCalendarName();
        String calendarId = findOrCreateCalendar(client, calendarName);

        List<ScheduleSlot> slots = slotRepository.findByTerm(term.getId());
        Map<Long, List<String>> groupEmails = recipientResolver.activeParentEmailsByGroup(RecipientResolver.groupIds(slots));
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        for (ScheduleSlot slot : slots) {
            LocalDate first = ScheduleCalendar.firstOccurrence(slot);
            if (first.isAfter(term.getEndDate())) {
                skipped++;
                continue;
            }
            Event event = buildEvent(slot, term, first, recipientResolver.forSlot(slot, groupEmails));
            String label = slot.getClassGroup().getName() + " " + slot.getDayOfWeek();
            try {
                if (slot.getGoogleEventId() != null && tryUpdate(client, calendarId, slot.getGoogleEventId(), event)) {
                    updated++;
                } else {
                    Event inserted = client.events().insert(calendarId, event).setSendUpdates(sendUpdates()).execute();
                    slot.setGoogleEventId(inserted.getId());
                    created++;
                }
            } catch (IOException e) {
                log.warn("Google Calendar sync failed for slot {}: {}", slot.getId(), e.getMessage());
                errors.add(label + ": " + e.getMessage());
            }
        }
        return new SyncResult(calendarName, created, updated, skipped, errors);
    }

    public void deleteEvent(Term term, String eventId) {
        requireEnabled();
        Calendar client = client();
        String calendarName = term.getCalendarName() == null ? DEFAULT_CALENDAR : term.getCalendarName();
        try {
            String calendarId = findCalendar(client, calendarName);
            if (calendarId != null) {
                client.events().delete(calendarId, eventId).setSendUpdates(sendUpdates()).execute();
            }
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() != 404 && e.getStatusCode() != 410) {
                throw new UncheckedIOException(e);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean tryUpdate(Calendar client, String calendarId, String eventId, Event event) throws IOException {
        try {
            client.events().update(calendarId, eventId, event).setSendUpdates(sendUpdates()).execute();
            return true;
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404 || e.getStatusCode() == 410) {
                return false;
            }
            throw e;
        }
    }

    private Event buildEvent(ScheduleSlot slot, Term term, LocalDate first, List<String> recipients) {
        String title = slot.getClassGroup().getName() + (slot.getSubject() == null ? "" : " — " + slot.getSubject());
        StringBuilder description = new StringBuilder();
        if (slot.getTeacher() != null) {
            description.append("Teacher: ").append(slot.getTeacher()).append('\n');
        }
        if (slot.getNotes() != null) {
            description.append(slot.getNotes());
        }
        String until = ZonedDateTime.of(term.getEndDate(), LocalTime.of(23, 59, 59), AppTime.ZONE)
                .withZoneSameInstant(ZoneOffset.UTC).format(RRULE_UNTIL);
        Event event = new Event()
                .setSummary(title)
                .setLocation(slot.getLocation())
                .setDescription(description.toString().trim())
                .setStart(dateTime(first, slot.getStartTime()))
                .setEnd(dateTime(first, slot.getEndTime()))
                .setRecurrence(List.of("RRULE:FREQ=WEEKLY;BYDAY=" + slot.getDayOfWeek().rruleCode() + ";UNTIL=" + until));
        if (config.sendInvites()) {
            // Service accounts need domain-wide delegation to invite attendees, hence the separate flag.
            event.setAttendees(recipients.stream().map(e -> new EventAttendee().setEmail(e)).toList());
        }
        return event;
    }

    private static EventDateTime dateTime(LocalDate date, LocalTime time) {
        ZonedDateTime zdt = ZonedDateTime.of(date, time, AppTime.ZONE);
        return new EventDateTime()
                .setDateTime(DateTime.parseRfc3339(zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                .setTimeZone(AppTime.ZONE.getId());
    }

    private String findOrCreateCalendar(Calendar client, String name) {
        try {
            String id = findCalendar(client, name);
            if (id != null) {
                return id;
            }
            com.google.api.services.calendar.model.Calendar created = client.calendars()
                    .insert(new com.google.api.services.calendar.model.Calendar().setSummary(name).setTimeZone(AppTime.ZONE.getId()))
                    .execute();
            return created.getId();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not access Google Calendar: " + e.getMessage(), e);
        }
    }

    private static String findCalendar(Calendar client, String name) throws IOException {
        String pageToken = null;
        do {
            CalendarList list = client.calendarList().list().setPageToken(pageToken).execute();
            for (CalendarListEntry entry : list.getItems()) {
                if (name.equals(entry.getSummary())) {
                    return entry.getId();
                }
            }
            pageToken = list.getNextPageToken();
        } while (pageToken != null);
        return null;
    }

    private Calendar client() {
        try (InputStream in = Files.newInputStream(Path.of(config.credentialsPath()))) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in).createScoped(List.of(CalendarScopes.CALENDAR));
            return new Calendar.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(config.applicationName() == null ? "Smart Start Portal" : config.applicationName())
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Could not initialise the Google Calendar client: " + e.getMessage(), e);
        }
    }

    private String sendUpdates() {
        return config.sendInvites() ? "all" : "none";
    }

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new FeatureNotConfiguredException(disabledMessage());
        }
    }

    private String disabledMessage() {
        if (config == null || !config.enabled()) {
            return "Google Calendar sync is disabled. Set GOOGLE_CALENDAR_ENABLED=true and GOOGLE_CREDENTIALS_PATH to enable it.";
        }
        return "Google Calendar credentials were not found at GOOGLE_CREDENTIALS_PATH.";
    }
}
