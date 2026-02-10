package com.jpreiss.easy_factions.server.api.events;

import com.jpreiss.easy_factions.server.alliance.Alliance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when the color of an alliance changes
 */
public class AllianceChangeColorEvent extends Event {
    private final Alliance alliance;
    private final ServerPlayer player;

    public AllianceChangeColorEvent(Alliance alliance, ServerPlayer author) {
        this.alliance = alliance;
        this.player = author;
    }

    public Alliance getAlliance() {
        return alliance;
    }

    /**
     * Returns the player who changed the color
     */
    public ServerPlayer getPlayer() {
        return player;
    }
}
