package com.smartstart.portal.calendar.service;

import java.time.YearMonth;
import java.util.Set;

/** Published when classes are added, moved or removed, so fees for those months can be recounted. */
public record ClassCalendarChanged(Set<YearMonth> months) {
}
