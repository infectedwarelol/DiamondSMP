package dev.diamondsmp.core.storage;

import java.util.Locale;

public enum LeaderboardType {

    KILLS("kills DESC"),
    CURRENT("streak DESC"),
    STREAK("best_streak DESC"),
    KDR("CAST(kills AS REAL) / CASE WHEN deaths = 0 THEN 1 ELSE deaths END DESC");

    private final String ordering;

    LeaderboardType(String ordering) {
        this.ordering = ordering;
    }

    public String ordering() {
        return ordering;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LeaderboardType parse(String input) {
        if (input == null) {
            return null;
        }
        for (LeaderboardType type : values()) {
            if (type.id().equalsIgnoreCase(input)) {
                return type;
            }
        }
        return null;
    }
}
