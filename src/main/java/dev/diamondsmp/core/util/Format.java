package dev.diamondsmp.core.util;

import java.util.Locale;

public final class Format {

    private Format() {
    }

    public static long parseSeconds(String input, long fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        String value = input.trim().toLowerCase(Locale.ROOT);
        long total = 0L;
        long number = -1L;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isDigit(character)) {
                number = (number < 0 ? 0 : number * 10) + (character - '0');
                continue;
            }
            if (Character.isWhitespace(character)) {
                continue;
            }
            if (number < 0) {
                return fallback;
            }
            long unit = switch (character) {
                case 's' -> 1L;
                case 'm' -> 60L;
                case 'h' -> 3600L;
                case 'd' -> 86400L;
                default -> -1L;
            };
            if (unit < 0) {
                return fallback;
            }
            total += number * unit;
            number = -1L;
        }
        if (number >= 0) {
            total += number;
        }
        return total;
    }

    public static String duration(long seconds) {
        long remaining = Math.max(0L, seconds);
        long days = remaining / 86400L;
        long hours = remaining % 86400L / 3600L;
        long minutes = remaining % 3600L / 60L;
        long secs = remaining % 60L;
        StringBuilder builder = new StringBuilder();
        append(builder, days, "d");
        append(builder, hours, "h");
        append(builder, minutes, "m");
        if (secs > 0 || builder.isEmpty()) {
            append(builder, secs, "s");
        }
        return builder.toString();
    }

    public static String pretty(String enumName) {
        if (enumName == null || enumName.isEmpty()) {
            return "";
        }
        String[] words = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder(enumName.length());
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
        }
        return builder.toString();
    }

    public static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public static String coordinates(double x, double y, double z) {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", x, y, z);
    }

    private static void append(StringBuilder builder, long value, String unit) {
        if (value <= 0 && !(unit.equals("s") && builder.isEmpty())) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(' ');
        }
        builder.append(value).append(unit);
    }
}
