package com.jpreiss.easy_factions.server.api.events;

import com.jpreiss.easy_factions.server.faction.Faction;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a faction is disbanded
 */
public class FactionDisbandEvent extends Event {
    private final Faction faction;

    public FactionDisbandEvent(Faction faction) {
        this.faction = faction;
    }

    public Faction getFaction() {
        return faction;
    }
}
