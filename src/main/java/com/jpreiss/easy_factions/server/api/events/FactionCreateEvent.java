package com.jpreiss.easy_factions.server.api.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a new faction is created
 */
public class FactionCreateEvent extends Event {
    private final String factionName;
    private final ServerPlayer creator;

    public FactionCreateEvent(String factionName, ServerPlayer creator) {
        this.factionName = factionName;
        this.creator = creator;
    }

    public String getFactionName() {
        return factionName;
    }

    /**
     * Returns the creator of the faction
     */
    public ServerPlayer getCreator() {return creator;}
}
