package com.jpreiss.easy_factions.server.event_handlers;


import com.jpreiss.easy_factions.server.ServerConfig;
import com.jpreiss.easy_factions.server.claims.ClaimManager;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import static net.minecraft.SharedConstants.TICKS_PER_SECOND;

@EventBusSubscriber
public class TickHandler {
    private static int pointTickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {

        pointTickCounter++;

        if (pointTickCounter >= TICKS_PER_SECOND * ServerConfig.POINT_GENERATION_INTERVAL.get()) {
            pointTickCounter = 0;

            grantPoints();

        }

    }

    /**
     * Gives one point for every online faction member
     */
    private static void grantPoints() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        FactionStateManager factionManager = FactionStateManager.get(server);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (factionManager.playerIsInFaction(player.getUUID())) {
                ClaimManager.get(server).addPoints(factionManager.getFactionByPlayer(player.getUUID()).getName(), ServerConfig.POINT_GENERATION_AMOUNT.get());
            }
        }
    }

}
