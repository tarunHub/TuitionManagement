package com.smartstart.portal.common.util;

import com.smartstart.portal.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

/** Past months are closed: their class calendar, feedback and fee bills can no longer be changed. */
@Component
@RequiredArgsConstructor
public class MonthLock {

    private final Clock clock;

    public YearMonth current() {
        return YearMonth.now(clock);
    }

    public boolean isPast(YearMonth month) {
        return month.isBefore(current());
    }

    public void requireOpen(YearMonth month, String what) {
        if (isPast(month)) {
            throw new ConflictException("MONTH_LOCKED", AppTime.monthLabel(month) + " is a past month, so its "
                    + what + " can no longer be changed");
        }
    }

    public void requireOpen(LocalDate date, String what) {
        requireOpen(YearMonth.from(date), what);
    }
}
