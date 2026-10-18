package dev.diamondsmp.core.service;

import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ScreenShareSession {

    private final UUID targetId;
    private final String targetName;
    private final UUID staffId;
    private final String staffName;
    private final int pointId;
    private final Location anchor;
    private final Location targetOrigin;
    private final Location staffOrigin;
    private final long startedAt;
    private final long voiceDeadline;
    private final AtomicBoolean voiceExpired = new AtomicBoolean();
    private volatile boolean arrived;
    private volatile ItemDisplay iceHead;
    private volatile PlayerKickEvent.Cause kickCause;

    ScreenShareSession(UUID targetId, String targetName, UUID staffId, String staffName, int pointId,
                       Location anchor, Location targetOrigin, Location staffOrigin, long startedAt, long voiceDeadline) {
        this.targetId = targetId;
        this.targetName = targetName;
        this.staffId = staffId;
        this.staffName = staffName;
        this.pointId = pointId;
        this.anchor = anchor;
        this.targetOrigin = targetOrigin;
        this.staffOrigin = staffOrigin;
        this.startedAt = startedAt;
        this.voiceDeadline = voiceDeadline;
    }

    public UUID targetId() {
        return targetId;
    }

    public String targetName() {
        return targetName;
    }

    public UUID staffId() {
        return staffId;
    }

    public String staffName() {
        return staffName;
    }

    public boolean isStaff(UUID id) {
        return id.equals(staffId);
    }

    public int pointId() {
        return pointId;
    }

    public Location anchor() {
        return anchor.clone();
    }

    Location targetOrigin() {
        return targetOrigin;
    }

    Location staffOrigin() {
        return staffOrigin;
    }

    public long elapsed() {
        return System.currentTimeMillis() - startedAt;
    }

    public long remaining() {
        return Math.max(0L, voiceDeadline - System.currentTimeMillis());
    }

    public boolean hasArrived() {
        return arrived;
    }

    void markArrived() {
        arrived = true;
    }

    boolean expireVoice() {
        return voiceExpired.compareAndSet(false, true);
    }

    ItemDisplay iceHead() {
        return iceHead;
    }

    void iceHead(ItemDisplay display) {
        iceHead = display;
    }

    PlayerKickEvent.Cause kickCause() {
        return kickCause;
    }

    void kickCause(PlayerKickEvent.Cause cause) {
        kickCause = cause;
    }
}
