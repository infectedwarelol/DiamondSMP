package dev.diamondsmp.core.service;

import java.util.Locale;

public enum TeleportKind {

    SPAWN,
    WARP,
    BACK,
    TOP;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
