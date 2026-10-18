package dev.diamondsmp.core.storage;

import java.util.Locale;

public enum LeaderboardType {

    KILLS("kills DESC"),
    DEATHS("deaths DESC"),
    PLAYTIME("playtime DESC"),
    CURRENT("streak DESC"),
    STREAK("best_streak DESC");

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
}
