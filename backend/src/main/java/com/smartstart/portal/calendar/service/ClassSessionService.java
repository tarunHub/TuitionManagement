package com.smartstart.portal.calendar.service;

import com.smartstart.portal.calendar.dto.ClassCalendarDtos.RepeatWeeklyRequest;
import com.smartstart.portal.calendar.dto.ClassCalendarDtos.RepeatWeeklyResult;
import com.smartstart.portal.calendar.dto.ClassCalendarDtos.SessionRequest;
import com.smartstart.portal.calendar.dto.ClassCalendarDtos.SessionResponse;
import com.smartstart.portal.calendar.entity.ClassSession;
import com.smartstart.portal.calendar.repository.ClassSessionRepository;
import com.smartstart.portal.classgroup.entity.ClassGroup;
import com.smartstart.portal.classgroup.service.ClassGroupService;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.ConflictException;
import com.smartstart.portal.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The monthly class calendar: which days each batch has class. */
@Service
@RequiredArgsConstructor
public class ClassSessionService {

    private final ClassSessionRepository repository;
    private final ClassGroupService groups;

    @Transactional(readOnly = true)
    public List<SessionResponse> list(YearMonth month, Long groupId) {
        return sessions(month, groupId).stream().map(ClassSessionService::toResponse).toList();
    }

    /** Entities for the month, for the attendance grid and the schedule email. */
    @Transactional(readOnly = true)
    public List<ClassSession> sessions(YearMonth month, Long groupId) {
        return repository.findBetween(month.atDay(1), month.atEndOfMonth(), groupId);
    }

    /** Classes on one day (for the morning reminders). */
    @Transactional(readOnly = true)
    public List<ClassSession> sessionsOn(LocalDate date) {
        return repository.findBetween(date, date, null);
    }

    /** Batch id → days of the month that batch has class. */
    @Transactional(readOnly = true)
    public Map<Long, Set<LocalDate>> datesByGroup(YearMonth month) {
        Map<Long, Set<LocalDate>> out = new HashMap<>();
        for (ClassSession s : sessions(month, null)) {
            out.computeIfAbsent(s.getClassGroup().getId(), k -> new HashSet<>()).add(s.getClassDate());
        }
        return out;
    }

    @Transactional
    public SessionResponse create(SessionRequest request) {
        ClassGroup group = groups.require(request.classGroupId());
        checkTimes(request.startTime(), request.endTime());
        if (repository.existsByClassGroupIdAndClassDate(group.getId(), request.date())) {
            throw duplicate(group, request.date());
        }
        ClassSession s = new ClassSession();
        s.setClassGroup(group);
        apply(s, request);
        return toResponse(repository.saveAndFlush(s));
    }

    @Transactional
    public SessionResponse update(Long id, SessionRequest request) {
        ClassSession s = require(id);
        ClassGroup group = groups.require(request.classGroupId());
        checkTimes(request.startTime(), request.endTime());
        if (repository.existsByClassGroupIdAndClassDateAndIdNot(group.getId(), request.date(), id)) {
            throw duplicate(group, request.date());
        }
        s.setClassGroup(group);
        apply(s, request);
        return toResponse(repository.saveAndFlush(s));
    }

    /** Removes the class day only; attendance already marked for it is kept (and still billed). */
    @Transactional
    public void delete(Long id) {
        repository.delete(require(id));
    }

    @Transactional
    public RepeatWeeklyResult repeatWeekly(RepeatWeeklyRequest request) {
        ClassGroup group = groups.require(request.classGroupId());
        checkTimes(request.startTime(), request.endTime());
        YearMonth month = YearMonth.parse(request.month());
        Set<LocalDate> taken = new HashSet<>();
        repository.findBetween(month.atDay(1), month.atEndOfMonth(), group.getId()).forEach(s -> taken.add(s.getClassDate()));

        List<ClassSession> created = new ArrayList<>();
        int skipped = 0;
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            LocalDate date = month.atDay(d);
            if (!request.weekdays().contains(date.getDayOfWeek())) {
                continue;
            }
            if (taken.contains(date)) {
                skipped++;
                continue;
            }
            ClassSession s = new ClassSession();
            s.setClassGroup(group);
            s.setClassDate(date);
            s.setStartTime(request.startTime());
            s.setEndTime(request.endTime());
            s.setLocation(blankToNull(request.location()));
            created.add(s);
        }
        repository.saveAll(created);
        return new RepeatWeeklyResult(created.size(), skipped);
    }

    public ClassSession require(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Class", id));
    }

    private static void apply(ClassSession s, SessionRequest r) {
        s.setClassDate(r.date());
        s.setStartTime(r.startTime());
        s.setEndTime(r.endTime());
        s.setLocation(blankToNull(r.location()));
        s.setNotes(blankToNull(r.notes()));
    }

    private static void checkTimes(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new BadRequestException("VALIDATION_FAILED", "must be after the start time", "endTime");
        }
    }

    private static ConflictException duplicate(ClassGroup group, LocalDate date) {
        return new ConflictException("CLASS_EXISTS", group.getName() + " already has a class on " + date);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    public static SessionResponse toResponse(ClassSession s) {
        return new SessionResponse(s.getId(), s.getClassGroup().getId(), s.getClassGroup().getName(), s.getClassDate(),
                s.getStartTime(), s.getEndTime(), s.getLocation(), s.getNotes());
    }
}
