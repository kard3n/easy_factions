package com.jpreiss.easy_factions.server.api.events;

import net.minecraftforge.eventbus.api.Event;

import java.util.UUID;

/**
 * Fired when a player leaves the faction
 */
public class FactionLeaveEvent extends Event {
    private final String factionName;
    private final UUID player;

    public FactionLeaveEvent(String factionName, UUID player) {
        this.factionName = factionName;
        this.player = player;
    }

    public String getFactionName() {
        return factionName;
    }

    /**
     * Returns the UUID of the player who left
     */
    public UUID getPlayer() {return player;}
}
