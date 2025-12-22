package com.jpreiss.easy_factions.api.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a player joins the faction
 */
public class FactionJoinEvent extends Event {
    private final String factionName;
    private final ServerPlayer player;

    public FactionJoinEvent(String factionName, ServerPlayer creator) {
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
