package com.smartstart.portal.common.util;

import com.smartstart.portal.common.exception.BadRequestException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;

public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("America/Toronto");
    public static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.CANADA);

    private AppTime() {
    }

    public static YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BadRequestException("INVALID_MONTH", "month must be in YYYY-MM format");
        }
    }

    public static String monthLabel(YearMonth month) {
        return month.format(MONTH_LABEL);
    }

    public static String dayLetter(DayOfWeek dayOfWeek) {
        return dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH);
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
