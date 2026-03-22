package com.jpreiss.easy_factions.server.event_handlers;


import com.jpreiss.easy_factions.server.ServerConfig;
import com.jpreiss.easy_factions.server.claims.ClaimManager;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import static net.minecraft.SharedConstants.TICKS_PER_SECOND;

@Mod.EventBusSubscriber
public class TickHandler {
    private static int pointTickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        pointTickCounter++;

        if (pointTickCounter >= TICKS_PER_SECOND * ServerConfig.pointGenerationInterval) {
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
                ClaimManager.get(server).addPoints(factionManager.getFactionByPlayer(player.getUUID()).getName(), ServerConfig.pointGenerationAmount);
            }
        }
    }

}
