package com.smartstart.portal.schedule.service;

import com.smartstart.portal.classgroup.service.ClassGroupService;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.NotFoundException;
import com.smartstart.portal.common.util.EmailListParser;
import com.smartstart.portal.schedule.dto.ScheduleDtos.RecipientsResponse;
import com.smartstart.portal.schedule.dto.ScheduleDtos.SlotFields;
import com.smartstart.portal.schedule.dto.ScheduleDtos.SlotResponse;
import com.smartstart.portal.schedule.entity.ScheduleSlot;
import com.smartstart.portal.schedule.entity.Term;
import com.smartstart.portal.schedule.repository.ScheduleSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleSlotService {

    public static final Comparator<ScheduleSlot> SLOT_ORDER = Comparator
            .comparing((ScheduleSlot s) -> s.getDayOfWeek().ordinal())
            .thenComparing(ScheduleSlot::getStartTime)
            .thenComparing(s -> s.getClassGroup().getName());

    private final ScheduleSlotRepository repository;
    private final TermService termService;
    private final ClassGroupService classGroupService;
    private final RecipientResolver recipientResolver;
    private final CalendarSyncService calendarSyncService;

    /** Slots of the given term (defaults to the active term), with recipient counts and overlap warnings. */
    @Transactional(readOnly = true)
    public List<SlotResponse> list(Long termId) {
        Long id = termId;
        if (id == null) {
            Term active = termService.activeTerm().orElse(null);
            if (active == null) {
                return List.of();
            }
            id = active.getId();
        }
        List<ScheduleSlot> slots = new ArrayList<>(repository.findByTerm(id));
        slots.sort(SLOT_ORDER);
        Map<Long, List<String>> groupEmails = recipientResolver.activeParentEmailsByGroup(RecipientResolver.groupIds(slots));
        return slots.stream().map(s -> toResponse(s, slots, groupEmails)).toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleSlot> activeTermSlots() {
        return termService.activeTerm()
                .map(t -> {
                    List<ScheduleSlot> slots = new ArrayList<>(repository.findByTerm(t.getId()));
                    slots.sort(SLOT_ORDER);
                    return slots;
                })
                .orElse(List.of());
    }

    @Transactional
    public SlotResponse create(SlotFields request) {
        ScheduleSlot slot = new ScheduleSlot();
        apply(slot, request);
        repository.saveAndFlush(slot);
        return responseWithSiblings(slot);
    }

    @Transactional
    public SlotResponse update(Long id, SlotFields request) {
        ScheduleSlot slot = require(id);
        apply(slot, request);
        repository.saveAndFlush(slot);
        return responseWithSiblings(slot);
    }

    @Transactional
    public void delete(Long id) {
        ScheduleSlot slot = require(id);
        if (slot.getGoogleEventId() != null && calendarSyncService.isEnabled()) {
            try {
                calendarSyncService.deleteEvent(slot.getTerm(), slot.getGoogleEventId());
            } catch (RuntimeException e) {
                log.warn("Could not delete Google Calendar event for slot {}: {}", id, e.getMessage());
            }
        }
        repository.delete(slot);
    }

    @Transactional(readOnly = true)
    public RecipientsResponse recipients(Long id) {
        ScheduleSlot slot = require(id);
        Map<Long, List<String>> groupEmails = recipientResolver.activeParentEmailsByGroup(Set.of(slot.getClassGroup().getId()));
        List<String> emails = recipientResolver.forSlot(slot, groupEmails);
        return new RecipientsResponse(id, emails, groupEmails.getOrDefault(slot.getClassGroup().getId(), List.of()).size(),
                EmailListParser.split(slot.getExtraEmails()).size());
    }

    @Transactional(readOnly = true)
    public ScheduleSlot require(Long id) {
        return repository.findWithRelations(id).orElseThrow(() -> new NotFoundException("Schedule slot", id));
    }

    private void apply(ScheduleSlot slot, SlotFields request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new BadRequestException("VALIDATION_FAILED", "must be after the start time", "endTime");
        }
        Term term = request.termId() != null
                ? termService.require(request.termId())
                : termService.activeTerm().orElseThrow(() -> new BadRequestException("NO_ACTIVE_TERM",
                "Configure the active term before adding schedule slots", "termId"));
        slot.setTerm(term);
        slot.setClassGroup(classGroupService.resolveOptional(request.classGroupId(), "classGroupId"));
        slot.setDayOfWeek(request.dayOfWeek());
        slot.setStartTime(request.startTime());
        slot.setEndTime(request.endTime());
        slot.setSubject(trim(request.subject()));
        slot.setLocation(trim(request.location()));
        slot.setTeacher(trim(request.teacher()));
        String extra = EmailListParser.normalize(request.extraEmails(), "extraEmails");
        slot.setExtraEmails(extra.isEmpty() ? null : extra);
        slot.setNotes(trim(request.notes()));
    }

    private SlotResponse responseWithSiblings(ScheduleSlot slot) {
        List<ScheduleSlot> siblings = repository.findByTerm(slot.getTerm().getId());
        Map<Long, List<String>> groupEmails = recipientResolver.activeParentEmailsByGroup(Set.of(slot.getClassGroup().getId()));
        return toResponse(slot, siblings, groupEmails);
    }

    private SlotResponse toResponse(ScheduleSlot slot, List<ScheduleSlot> termSlots, Map<Long, List<String>> groupEmails) {
        List<ScheduleSlot> overlaps = termSlots.stream()
                .filter(o -> !o.getId().equals(slot.getId()) && overlaps(slot, o))
                .toList();
        String warning = overlaps.isEmpty() ? null : "Overlaps at " + slot.getLocation() + " with "
                + String.join(", ", overlaps.stream().map(o -> o.getClassGroup().getName() + " "
                + o.getStartTime() + "–" + o.getEndTime()).toList());
        return new SlotResponse(slot.getId(), slot.getTerm().getId(), slot.getTerm().getName(),
                slot.getClassGroup().getId(), slot.getClassGroup().getName(), slot.getDayOfWeek(),
                slot.getStartTime(), slot.getEndTime(), slot.getSubject(), slot.getLocation(), slot.getTeacher(),
                slot.getExtraEmails(), slot.getNotes(), slot.getGoogleEventId(),
                recipientResolver.forSlot(slot, groupEmails).size(),
                overlaps.stream().map(ScheduleSlot::getId).toList(), warning);
    }

    /** Same day, same (non-blank) location, intersecting times. */
    static boolean overlaps(ScheduleSlot a, ScheduleSlot b) {
        return a.getDayOfWeek() == b.getDayOfWeek()
                && a.getLocation() != null && b.getLocation() != null
                && a.getLocation().equalsIgnoreCase(b.getLocation())
                && a.getStartTime().isBefore(b.getEndTime())
                && b.getStartTime().isBefore(a.getEndTime());
    }

    private static String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
