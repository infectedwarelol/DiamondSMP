package dev.diamondsmp.core.storage;

import java.util.List;

public record LeaderboardPage(int total, List<ProfileSnapshot> entries) {

    public LeaderboardPage {
        entries = List.copyOf(entries);
    }
}
