package com.smartstart.portal.schedule.service;

import com.smartstart.portal.calendar.entity.ClassSession;
import com.smartstart.portal.calendar.repository.ClassSessionRepository;
import com.smartstart.portal.schedule.entity.ScheduleDay;
import com.smartstart.portal.schedule.dto.ScheduleDtos.ClassOccurrence;
import com.smartstart.portal.schedule.entity.ScheduleSlot;
import com.smartstart.portal.schedule.entity.Term;
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

/**
 * Class dates for fees ("classes scheduled") and the dashboard's upcoming classes. These come from the monthly
 * Class Calendar; the static helpers still expand weekly slots for the legacy Google Calendar sync.
 */
@Service
@RequiredArgsConstructor
public class ScheduleCalendar {

    private final ClassSessionRepository sessionRepository;

    /** Number of classes per batch on the class calendar in the month. Batches without classes are absent. */
    @Transactional(readOnly = true)
    public Map<Long, Integer> classesScheduledByGroup(YearMonth month) {
        Map<Long, Integer> out = new HashMap<>();
        for (ClassSession s : sessionRepository.findBetween(month.atDay(1), month.atEndOfMonth(), null)) {
            out.merge(s.getClassGroup().getId(), 1, Integer::sum);
        }
        return out;
    }

    /** All classes between the two dates (inclusive), ordered by date and start time. */
    @Transactional(readOnly = true)
    public List<ClassOccurrence> occurrences(LocalDate from, LocalDate to) {
        return sessionRepository.findBetween(from, to, null).stream()
                .map(s -> new ClassOccurrence(s.getId(), s.getClassDate(), ScheduleDay.of(s.getClassDate().getDayOfWeek()),
                        s.getStartTime(), s.getEndTime(), s.getClassGroup().getId(), s.getClassGroup().getName(), null,
                        s.getLocation(), null))
                .sorted(Comparator.comparing(ClassOccurrence::date).thenComparing(ClassOccurrence::startTime))
                .toList();
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
