package com.smartstart.portal.common.util;

import com.smartstart.portal.common.exception.BadRequestException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Normalises the free-form phone numbers stored on students (e.g. "416-555-0101") to E.164 ("+14165550101") for SMS. */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /**
     * {@code +} numbers are kept as dialled; {@code 00} becomes {@code +}; anything else is treated as a national number
     * and gets {@code defaultCountryCode} (dropping a leading trunk 0, and accepting North American numbers that already
     * start with 1). Empty when the result is not 8-15 digits.
     */
    public static Optional<String> toE164(String raw, String defaultCountryCode) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String trimmed = raw.trim();
        String digits = trimmed.replaceAll("\\D", "");
        String cc = defaultCountryCode == null ? "" : defaultCountryCode.replaceAll("\\D", "");
        String full;
        if (trimmed.startsWith("+")) {
            full = digits;
        } else if (digits.startsWith("00")) {
            full = digits.substring(2);
        } else if (cc.equals("1") && digits.length() == 11 && digits.startsWith("1")) {
            full = digits;
        } else {
            full = cc + (digits.startsWith("0") ? digits.substring(1) : digits);
        }
        return full.length() >= 8 && full.length() <= 15 ? Optional.of("+" + full) : Optional.empty();
    }

    /** Parses a comma, semicolon or newline separated list (spaces may appear inside numbers), validating every entry. */
    public static List<String> parse(String raw, String defaultCountryCode, String field) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String part : raw.split("[,;\\n]+")) {
            if (part.isBlank()) {
                continue;
            }
            out.add(toE164(part, defaultCountryCode).orElseThrow(() ->
                    new BadRequestException("VALIDATION_FAILED", field + " contains an invalid phone number: " + part.trim(), field)));
        }
        return List.copyOf(out);
    }
}
