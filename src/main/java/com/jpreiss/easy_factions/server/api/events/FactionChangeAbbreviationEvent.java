package com.jpreiss.easy_factions.server.api.events;

import com.jpreiss.easy_factions.server.faction.Faction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when the abbreviation of an alliance changes
 */
public class FactionChangeAbbreviationEvent extends Event {
    private final Faction faction;
    private final ServerPlayer player;

    public FactionChangeAbbreviationEvent(Faction faction, ServerPlayer author) {
        this.faction = faction;
        this.player = author;
    }

    public Faction getFaction() {
        return faction;
    }

    /**
     * Returns the player who changed the abbreviation
     */
    public ServerPlayer getPlayer() {
        return player;
    }
}
