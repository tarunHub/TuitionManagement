package com.smartstart.portal.notification.service;

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

    /** Text list of a batch's class dates, e.g. "Tue, Oct 6  17:00–18:00  Caledon Centre". */
    public String sessionTable(List<ClassSession> sessions) {
        if (sessions.isEmpty()) {
            return "(no classes scheduled)";
        }
        boolean location = sessions.stream().anyMatch(s -> s.getLocation() != null);
        boolean notes = sessions.stream().anyMatch(s -> s.getNotes() != null);
        List<String> headers = new ArrayList<>(List.of("Date", "Time"));
        if (location) {
            headers.add("Location");
        }
        if (notes) {
            headers.add("Notes");
        }
        List<String[]> rows = sessions.stream().map(s -> {
            List<String> r = new ArrayList<>(List.of(s.getClassDate().format(SHORT_DATE), s.getStartTime() + "–" + s.getEndTime()));
            if (location) {
                r.add(nz(s.getLocation()));
            }
            if (notes) {
                r.add(nz(s.getNotes()));
            }
            return r.toArray(String[]::new);
        }).toList();
        return table(headers.toArray(String[]::new), rows) + "\n\nTotal: " + sessions.size() + (sessions.size() == 1 ? " class" : " classes");
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
