package com.smartstart.portal.notification.service;

import com.smartstart.portal.notification.dto.NotificationDtos.PlaceholderInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Replaces {@code {{placeholder}}} tokens; unknown placeholders are left untouched. */
@Component
public class TemplateRenderer {

    public static final List<PlaceholderInfo> PLACEHOLDERS = List.of(
            new PlaceholderInfo("guardianName", "Parent/guardian name"),
            new PlaceholderInfo("studentName", "Student full name"),
            new PlaceholderInfo("month", "Month, e.g. August 2026"),
            new PlaceholderInfo("amountDue", "Amount due for the month"),
            new PlaceholderInfo("balance", "Outstanding balance for the month"),
            new PlaceholderInfo("scheduleTable", "Weekly timetable (the month's class dates in the Monthly Schedule email)"),
            new PlaceholderInfo("className", "Class group name"),
            new PlaceholderInfo("date", "A date, e.g. the class date"),
            new PlaceholderInfo("time", "Class time, e.g. 17:00–18:00 (class reminders only)"),
            new PlaceholderInfo("location", "Class location (class reminders only)"),
            new PlaceholderInfo("feedback", "Monthly feedback text (feedback emails only)"));

    private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    public String render(String template, Map<String, String> values) {
        if (template == null) {
            return "";
        }
        Matcher m = TOKEN.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = values.get(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(value != null ? value : m.group()));
        }
        m.appendTail(out);
        return out.toString();
    }
}
