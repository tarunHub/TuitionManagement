package com.smartstart.portal.common.util;

import com.smartstart.portal.common.exception.BadRequestException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class EmailListParser {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+'-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private EmailListParser() {
    }

    /** Splits a comma/semicolon/whitespace separated list, validating every entry. */
    public static List<String> parse(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String part : raw.split("[,;\\s]+")) {
            String email = part.trim().toLowerCase();
            if (email.isEmpty()) {
                continue;
            }
            if (!EMAIL.matcher(email).matches()) {
                throw new BadRequestException("VALIDATION_FAILED", field + " contains an invalid email: " + email, field);
            }
            out.add(email);
        }
        return List.copyOf(out);
    }

    public static String normalize(String raw, String field) {
        return String.join(", ", parse(raw, field));
    }

    public static boolean isValid(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    public static List<String> dedupe(List<String> emails) {
        Set<String> out = new LinkedHashSet<>();
        emails.stream().filter(e -> e != null && !e.isBlank()).map(e -> e.trim().toLowerCase()).forEach(out::add);
        return List.copyOf(out);
    }

    public static List<String> split(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        return Arrays.stream(stored.split("[,;\\s]+")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
