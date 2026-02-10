package com.jpreiss.easy_factions.server.api.events;

import com.jpreiss.easy_factions.server.faction.Faction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when the color of an alliance changes
 */
public class FactionChangeColorEvent extends Event {
    private final Faction faction;
    private final ServerPlayer player;

    public FactionChangeColorEvent(Faction faction, ServerPlayer author) {
        this.faction = faction;
        this.player = author;
    }

    public Faction getFaction() {
        return faction;
    }

    /**
     * Returns the player who changed the color
     */
    public ServerPlayer getPlayer() {
        return player;
    }
}
