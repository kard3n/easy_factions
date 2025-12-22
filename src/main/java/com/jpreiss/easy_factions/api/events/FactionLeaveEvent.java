package com.jpreiss.easy_factions.api.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a player leaves the faction
 */
public class FactionLeaveEvent extends Event {
    private final String factionName;
    private final ServerPlayer player;

    public FactionLeaveEvent(String factionName, ServerPlayer creator) {
        this.factionName = factionName;
        this.player = creator;
    }

    public String getFactionName() {
        return factionName;
    }

    /**
     * Returns the creator of the faction
     */
    public ServerPlayer getPlayer() {return player;}
}
