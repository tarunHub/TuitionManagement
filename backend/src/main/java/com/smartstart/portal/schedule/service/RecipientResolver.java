package com.smartstart.portal.schedule.service;

import com.smartstart.portal.common.util.EmailListParser;
import com.smartstart.portal.common.util.PhoneNumbers;
import com.smartstart.portal.schedule.entity.ScheduleSlot;
import com.smartstart.portal.student.entity.Student;
import com.smartstart.portal.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Recipients for a slot = parent emails of ACTIVE students in the slot's group + the slot's extra emails, de-duplicated. */
@Service
@RequiredArgsConstructor
public class RecipientResolver {

    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public Map<Long, List<String>> activeParentEmailsByGroup(Collection<Long> groupIds) {
        Map<Long, List<String>> out = new HashMap<>();
        if (groupIds.isEmpty()) {
            return out;
        }
        for (Student s : studentRepository.findActiveInGroups(groupIds)) {
            out.computeIfAbsent(s.getClassGroup().getId(), k -> new ArrayList<>()).add(s.getEmail());
        }
        out.replaceAll((k, v) -> EmailListParser.dedupe(v));
        return out;
    }

    /** SMS numbers (E.164) of ACTIVE students' parents who opted in, by group. Slot extra emails have no SMS counterpart. */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> activeParentPhonesByGroup(Collection<Long> groupIds, String defaultCountryCode) {
        Map<Long, Set<String>> out = new HashMap<>();
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        for (Student s : studentRepository.findActiveInGroups(groupIds)) {
            smsNumber(s, defaultCountryCode).ifPresent(phone ->
                    out.computeIfAbsent(s.getClassGroup().getId(), k -> new LinkedHashSet<>()).add(phone));
        }
        return out.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> List.copyOf(e.getValue())));
    }

    /** The parent's SMS number if they opted in and the primary contact is a usable number. */
    public static Optional<String> smsNumber(Student s, String defaultCountryCode) {
        return s.isSmsOptIn() ? PhoneNumbers.toE164(s.getPrimaryContact(), defaultCountryCode) : Optional.empty();
    }

    public List<String> forSlot(ScheduleSlot slot, Map<Long, List<String>> groupEmails) {
        List<String> all = new ArrayList<>(groupEmails.getOrDefault(slot.getClassGroup().getId(), List.of()));
        all.addAll(EmailListParser.split(slot.getExtraEmails()));
        return EmailListParser.dedupe(all);
    }

    @Transactional(readOnly = true)
    public List<String> forSlot(ScheduleSlot slot) {
        return forSlot(slot, activeParentEmailsByGroup(Set.of(slot.getClassGroup().getId())));
    }

    /** Group parents plus extra emails of all the group's given slots. */
    public List<String> forGroup(Long groupId, List<ScheduleSlot> slots, Map<Long, List<String>> groupEmails) {
        List<String> all = new ArrayList<>(groupEmails.getOrDefault(groupId, List.of()));
        slots.stream().filter(s -> s.getClassGroup().getId().equals(groupId))
                .forEach(s -> all.addAll(EmailListParser.split(s.getExtraEmails())));
        return EmailListParser.dedupe(all);
    }

    public static Set<Long> groupIds(Collection<ScheduleSlot> slots) {
        return slots.stream().map(s -> s.getClassGroup().getId()).collect(Collectors.toSet());
    }
}
