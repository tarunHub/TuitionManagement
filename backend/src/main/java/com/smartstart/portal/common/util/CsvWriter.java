package com.smartstart.portal.common.util;

import java.util.List;
import java.util.stream.Collectors;

public final class CsvWriter {

    private final StringBuilder sb = new StringBuilder();

    public CsvWriter row(List<?> values) {
        sb.append(values.stream().map(CsvWriter::escape).collect(Collectors.joining(","))).append("\r\n");
        return this;
    }

    public String build() {
        return sb.toString();
    }

    private static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        // Guard against CSV/formula injection when opened in spreadsheet apps.
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0 && !s.matches("-?\\d+(\\.\\d+)?")) {
            s = "'" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
