package com.smartstart.portal.schedule.entity;

import java.time.DayOfWeek;

public enum ScheduleDay {
    MON(DayOfWeek.MONDAY),
    TUE(DayOfWeek.TUESDAY),
    WED(DayOfWeek.WEDNESDAY),
    THU(DayOfWeek.THURSDAY),
    FRI(DayOfWeek.FRIDAY),
    SAT(DayOfWeek.SATURDAY),
    SUN(DayOfWeek.SUNDAY);

    private final DayOfWeek dayOfWeek;

    ScheduleDay(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public DayOfWeek toDayOfWeek() {
        return dayOfWeek;
    }

    public static ScheduleDay of(DayOfWeek day) {
        return values()[day.getValue() - 1];
    }

    /** Two-letter RRULE code, e.g. MO. */
    public String rruleCode() {
        return name().substring(0, 2);
    }
}
