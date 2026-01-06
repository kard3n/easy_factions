package com.jpreiss.easy_factions.network;

import com.jpreiss.easy_factions.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.faction.FactionStateManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.UUID;

public class NetworkManager {

    /**
     * Broadcast a faction or alliance update to all players who have the mod installed.
     * OPTIMIZED: Fetches data once, then sends to multiple players.
     *
     * @param server The server to broadcast to
     */
    public static void broadcastUpdate(MinecraftServer server) {
        FactionStateManager factionManager = FactionStateManager.get(server);
        AllianceStateManager allianceManager = AllianceStateManager.get(server);

        Map<UUID, String> playerFactions = factionManager.getPlayerFactionMap();
        Map<String, String> factionAlliances = allianceManager.getFactionAllianceMap();

        PacketSyncFaction packet = new PacketSyncFaction(playerFactions, factionAlliances);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Check if the remote client has this channel (mod) active
            if (NetworkHandler.CHANNEL.isRemotePresent(player.connection.connection)) {
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

}