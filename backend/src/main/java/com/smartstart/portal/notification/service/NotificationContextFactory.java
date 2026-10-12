package com.smartstart.portal.notification.service;

import com.smartstart.portal.calendar.dto.ClassCalendarDtos.ClassChangeRequest;
import com.smartstart.portal.calendar.entity.ClassSession;
import com.smartstart.portal.common.util.AppTime;
import com.smartstart.portal.common.util.Money;
import com.smartstart.portal.fee.dto.FeeDtos.FeeRowResponse;
import com.smartstart.portal.schedule.entity.ScheduleSlot;
import com.smartstart.portal.student.entity.Student;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds placeholder values for template rendering. */
@Component
public class NotificationContextFactory {

    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MMM d", Locale.CANADA);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.CANADA);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.CANADA);

    private final Clock clock;

    public NotificationContextFactory(Clock clock) {
        this.clock = clock;
    }

    /** Generic values used for broadcasts where per-student details are unknown. */
    public Map<String, String> base(YearMonth month) {
        Map<String, String> ctx = new HashMap<>();
        ctx.put("guardianName", "Parent/Guardian");
        ctx.put("studentName", "your child");
        ctx.put("month", AppTime.monthLabel(month == null ? YearMonth.now(clock) : month));
        ctx.put("date", formatDate(LocalDate.now(clock)));
        ctx.put("className", "your class");
        ctx.put("amountDue", "");
        ctx.put("balance", "");
        ctx.put("scheduleTable", "");
        ctx.put("classDates", "");
        ctx.put("time", "");
        ctx.put("location", "");
        return ctx;
    }

    public Map<String, String> forStudent(Student student, YearMonth month) {
        Map<String, String> ctx = base(month);
        ctx.put("guardianName", student.getGuardianName());
        ctx.put("studentName", student.getFullName());
        if (student.getClassGroup() != null) {
            ctx.put("className", student.getClassGroup().getName());
        }
        return ctx;
    }

    public Map<String, String> forFee(FeeRowResponse fee, String guardianName) {
        Map<String, String> ctx = base(YearMonth.parse(fee.month()));
        ctx.put("guardianName", guardianName);
        ctx.put("studentName", fee.fullName());
        ctx.put("amountDue", Money.format(fee.amountDue()));
        ctx.put("balance", Money.format(fee.balance()));
        if (fee.classGroupName() != null) {
            ctx.put("className", fee.classGroupName());
        }
        return ctx;
    }

    /** Sample values for previews without a selected student. */
    public Map<String, String> sample(YearMonth month) {
        Map<String, String> ctx = base(month);
        ctx.put("guardianName", "Jordan Sample");
        ctx.put("studentName", "Alex Sample");
        ctx.put("className", "Batch1");
        ctx.put("amountDue", "$200.00");
        ctx.put("balance", "$50.00");
        ctx.put("time", "17:00–18:00");
        ctx.put("location", "Main Centre");
        ctx.put("feedback", "Topics taught: Fractions and decimals\nAreas to improve: Word problems\nAchievements: Perfect score on the weekly quiz");
        return ctx;
    }

    public String formatDate(LocalDate date) {
        return date.format(DATE);
    }

    /** Fixed-width text timetable, e.g. "Thu  17:00–18:00  Math & English  Caledon Centre  Tarun". */
    public String scheduleTable(List<ScheduleSlot> slots, boolean includeGroup) {
        if (slots.isEmpty()) {
            return "(no classes scheduled)";
        }
        String[] headers = includeGroup
                ? new String[]{"Class", "Day", "Time", "Subject", "Location", "Teacher"}
                : new String[]{"Day", "Time", "Subject", "Location", "Teacher"};
        List<String[]> rows = slots.stream().map(s -> {
            String day = s.getDayOfWeek().toDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.CANADA);
            String time = s.getStartTime() + "–" + s.getEndTime();
            String[] base = {day, time, nz(s.getSubject()), nz(s.getLocation()), nz(s.getTeacher())};
            if (!includeGroup) {
                return base;
            }
            String[] withGroup = new String[base.length + 1];
            withGroup[0] = s.getClassGroup().getName();
            System.arraycopy(base, 0, withGroup, 1, base.length);
            return withGroup;
        }).toList();
        return table(headers, rows);
    }

    /** Text table of class dates, e.g. "Tue, Oct 6  17:00–18:00  Caledon Centre"; {@code includeGroup} adds a Batch column. */
    public String sessionTable(List<ClassSession> sessions, boolean includeGroup) {
        if (sessions.isEmpty()) {
            return "(no classes scheduled)";
        }
        boolean location = sessions.stream().anyMatch(s -> s.getLocation() != null);
        boolean notes = sessions.stream().anyMatch(s -> s.getNotes() != null);
        List<String> headers = new ArrayList<>();
        if (includeGroup) {
            headers.add("Batch");
        }
        headers.addAll(List.of("Date", "Time"));
        if (location) {
            headers.add("Location");
        }
        if (notes) {
            headers.add("Notes");
        }
        List<String[]> rows = sessions.stream().map(s -> {
            List<String> r = new ArrayList<>();
            if (includeGroup) {
                r.add(s.getClassGroup().getName());
            }
            r.add(s.getClassDate().format(SHORT_DATE));
            r.add(s.getStartTime() + "–" + s.getEndTime());
            if (location) {
                r.add(nz(s.getLocation()));
            }
            if (notes) {
                r.add(nz(s.getNotes()));
            }
            return r.toArray(String[]::new);
        }).toList();
        String table = table(headers.toArray(String[]::new), rows);
        return sessions.size() == 1 ? table : table + "\n\nTotal: " + sessions.size() + " classes";
    }

    /** One sentence describing a class that was added, changed or cancelled, for the Class Change messages. */
    public String describeChange(ClassChangeRequest r) {
        String now = r.date().format(SHORT_DATE) + ", " + r.startTime() + "–" + r.endTime()
                + (r.location() == null || r.location().isBlank() ? "" : " at " + r.location().strip());
        return switch (r.kind()) {
            case ADDED -> "A class has been added on " + now + ".";
            case CANCELLED -> "The class on " + now + " is cancelled.";
            case CHANGED -> {
                if (r.previousDate() == null) {
                    yield "The class is now on " + now + ".";
                }
                String before = r.previousDate().format(SHORT_DATE)
                        + (r.previousStartTime() == null ? "" : " (" + r.previousStartTime() + "–" + r.previousEndTime() + ")");
                yield r.previousDate().equals(r.date()) && !java.util.Objects.equals(r.previousLocation(), r.location())
                        && r.startTime().equals(r.previousStartTime()) && r.endTime().equals(r.previousEndTime())
                        ? "The class on " + before + " will now be held at " + (r.location() == null || r.location().isBlank() ? "the usual location" : r.location().strip()) + "."
                        : "The class on " + before + " has moved to " + now + ".";
            }
        };
    }

    /**
     * Short one-line form for SMS. Classes at the usual (most common) time are listed as "Oct 1, 6, 8 at 17:00–18:00";
     * any at another time follow with their own time, e.g. "; Oct 24 at 10:00–11:30".
     */
    public String compactDates(List<ClassSession> sessions) {
        if (sessions.isEmpty()) {
            return "no classes scheduled";
        }
        Map<String, Long> byTime = sessions.stream().collect(java.util.stream.Collectors.groupingBy(
                s -> s.getStartTime() + "–" + s.getEndTime(), java.util.LinkedHashMap::new, java.util.stream.Collectors.counting()));
        String usual = byTime.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        StringBuilder out = new StringBuilder();
        java.time.Month currentMonth = null;
        for (ClassSession s : sessions) {
            if (!usual.equals(s.getStartTime() + "–" + s.getEndTime())) {
                continue;
            }
            java.time.Month m = s.getClassDate().getMonth();
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(m == currentMonth ? String.valueOf(s.getClassDate().getDayOfMonth()) : s.getClassDate().format(MONTH_DAY));
            currentMonth = m;
        }
        out.append(" at ").append(usual);
        for (ClassSession s : sessions) {
            String t = s.getStartTime() + "–" + s.getEndTime();
            if (!usual.equals(t)) {
                out.append("; ").append(s.getClassDate().format(MONTH_DAY)).append(" at ").append(t);
            }
        }
        return out.toString();
    }

    private static String table(String[] headers, List<String[]> rows) {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
            for (String[] r : rows) {
                widths[i] = Math.max(widths[i], r[i].length());
            }
        }
        StringBuilder sb = new StringBuilder();
        appendRow(sb, headers, widths);
        String[] rule = new String[headers.length];
        for (int i = 0; i < headers.length; i++) {
            rule[i] = "-".repeat(widths[i]);
        }
        appendRow(sb, rule, widths);
        rows.forEach(r -> appendRow(sb, r, widths));
        return sb.toString().stripTrailing();
    }

    private static void appendRow(StringBuilder sb, String[] cells, int[] widths) {
        for (int i = 0; i < cells.length; i++) {
            sb.append(String.format("%-" + widths[i] + "s", cells[i]));
            if (i < cells.length - 1) {
                sb.append("  ");
            }
        }
        sb.append('\n');
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
