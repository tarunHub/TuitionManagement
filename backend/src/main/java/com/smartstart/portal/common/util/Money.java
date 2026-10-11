package com.smartstart.portal.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public final class Money {

    private Money() {
    }

    public static BigDecimal scale(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** Formats as e.g. {@code $260.00} or {@code -$10.00}. */
    public static String format(BigDecimal value) {
        BigDecimal v = scale(value);
        String formatted = NumberFormat.getCurrencyInstance(Locale.CANADA).format(v.abs());
        return v.signum() < 0 ? "-" + formatted : formatted;
    }
}
