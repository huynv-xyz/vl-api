package com.vlife.api.util;

import com.vlife.shared.util.CommonUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public class ApiUtil {


    public static Long parseLong(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Long.parseLong(v.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static OffsetDateTime parseOffsetDateTime(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return OffsetDateTime.parse(v.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static String trimToNull(String v) {
        if (v == null) return null;
        String s = v.trim();
        return s.isEmpty() ? null : s;
    }

    public static Integer parseIntOrNull(String s) {
        s = trimToNull(s);
        if (s == null) return null;
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static OffsetDateTime parseTimeOrNull(String s) {
        s = trimToNull(s);
        if (s == null) return null;
        try {
            return OffsetDateTime.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static String trim(String s) {
        return s == null ? null : s.trim();
    }

    public static BigDecimal nvl(BigDecimal x) {
        return x != null ? x : BigDecimal.ZERO;
    }

    public static LocalDate toDate(String s) {
        if (CommonUtil.isNullOrEmpty(s)) return null;
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalDateTime toDateTime(String s) {
        if (CommonUtil.isNullOrEmpty(s)) return null;
        return LocalDate.parse(s).atStartOfDay();
    }

    public static Integer parseInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }

    public static String toCamelCase(String name) {

        String[] parts = name.toLowerCase().split("_");

        StringBuilder sb = new StringBuilder(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1));
        }

        return sb.toString();
    }
}
