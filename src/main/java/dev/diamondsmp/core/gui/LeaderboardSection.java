package dev.diamondsmp.core.gui;

import dev.diamondsmp.core.storage.LeaderboardType;
import dev.diamondsmp.core.storage.ProfileSnapshot;
import dev.diamondsmp.core.util.Format;

import java.util.Locale;

public enum LeaderboardSection {

    KILLS(LeaderboardType.KILLS),
    DEATHS(LeaderboardType.DEATHS),
    PLAYTIME(LeaderboardType.PLAYTIME),
    KILLSTREAKS(LeaderboardType.STREAK);

    private final LeaderboardType type;

    LeaderboardSection(LeaderboardType type) {
        this.type = type;
    }

    public LeaderboardType type() {
        return type;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String value(ProfileSnapshot entry) {
        return switch (this) {
            case KILLS -> String.valueOf(entry.kills());
            case DEATHS -> String.valueOf(entry.deaths());
            case PLAYTIME -> Format.duration(entry.playtime() / 1000L);
            case KILLSTREAKS -> String.valueOf(entry.bestStreak());
        };
    }

    public static LeaderboardSection parse(String input) {
        if (input == null) {
            return null;
        }
        for (LeaderboardSection section : values()) {
            if (section.id().equalsIgnoreCase(input)) {
                return section;
            }
        }
        return null;
    }
}
