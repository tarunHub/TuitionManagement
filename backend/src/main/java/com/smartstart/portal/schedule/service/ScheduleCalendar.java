package com.smartstart.portal.schedule.service;

import com.smartstart.portal.schedule.dto.ScheduleDtos.ClassOccurrence;
import com.smartstart.portal.schedule.entity.ScheduleSlot;
import com.smartstart.portal.schedule.entity.Term;
import com.smartstart.portal.schedule.repository.ScheduleSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Expands weekly slots into dated occurrences, bounded by each slot's term (rule 9). */
@Service
@RequiredArgsConstructor
public class ScheduleCalendar {

    private final ScheduleSlotRepository slotRepository;

    /** Number of scheduled class occurrences per class group in the month. Groups without slots are absent. */
    @Transactional(readOnly = true)
    public Map<Long, Integer> classesScheduledByGroup(YearMonth month) {
        Map<Long, Integer> out = new HashMap<>();
        for (ScheduleSlot slot : slotRepository.findInTermsOverlapping(month.atDay(1), month.atEndOfMonth())) {
            int n = dates(slot, month.atDay(1), month.atEndOfMonth()).size();
            out.merge(slot.getClassGroup().getId(), n, Integer::sum);
        }
        return out;
    }

    /** All class occurrences between the two dates (inclusive), ordered by date and start time. */
    @Transactional(readOnly = true)
    public List<ClassOccurrence> occurrences(LocalDate from, LocalDate to) {
        List<ClassOccurrence> out = new ArrayList<>();
        for (ScheduleSlot slot : slotRepository.findInTermsOverlapping(from, to)) {
            for (LocalDate date : dates(slot, from, to)) {
                out.add(new ClassOccurrence(slot.getId(), date, slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime(),
                        slot.getClassGroup().getId(), slot.getClassGroup().getName(), slot.getSubject(),
                        slot.getLocation(), slot.getTeacher()));
            }
        }
        out.sort(Comparator.comparing(ClassOccurrence::date).thenComparing(ClassOccurrence::startTime));
        return out;
    }

    public static List<LocalDate> dates(ScheduleSlot slot, LocalDate from, LocalDate to) {
        Term term = slot.getTerm();
        LocalDate start = from.isAfter(term.getStartDate()) ? from : term.getStartDate();
        LocalDate end = to.isBefore(term.getEndDate()) ? to : term.getEndDate();
        List<LocalDate> dates = new ArrayList<>();
        LocalDate d = start.with(TemporalAdjusters.nextOrSame(slot.getDayOfWeek().toDayOfWeek()));
        while (!d.isAfter(end)) {
            dates.add(d);
            d = d.plusWeeks(1);
        }
        return dates;
    }

    public static LocalDate firstOccurrence(ScheduleSlot slot) {
        return slot.getTerm().getStartDate().with(TemporalAdjusters.nextOrSame(slot.getDayOfWeek().toDayOfWeek()));
    }
}
