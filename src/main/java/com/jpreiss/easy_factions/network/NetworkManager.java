package com.jpreiss.easy_factions.network;

import com.jpreiss.easy_factions.alliance.Alliance;
import com.jpreiss.easy_factions.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.faction.Faction;
import com.jpreiss.easy_factions.faction.FactionStateManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class NetworkManager {

    /**
     * Updates a player about all others
     */
    public static void updatePlayerAboutOthers(ServerPlayer player, MinecraftServer server) {
        if (!NetworkHandler.CHANNEL.isRemotePresent(player.connection.connection)) return;

        PacketSyncFaction onlinePlayerData = getOnlinePlayerData(server);
        // Send faction state to the new player
        NetworkHandler.sendToPlayer(onlinePlayerData, player);
    }

    /**
     * Send information about a player to all others
     */
    public static void broadcastPlayerInfo(ServerPlayer newPlayer, MinecraftServer server) {

        // Send info about the new players affiliations to the rest
        PacketSyncFaction newPlayerData = getSinglePlayerData(newPlayer.getUUID(), server);
        if (newPlayerData != null) {
            for (ServerPlayer other : server.getPlayerList().getPlayers()) {
                // No need to update the new player, he already got the update before.
                if (other.getUUID().equals(newPlayer.getUUID())) continue;
                NetworkHandler.sendToPlayer(newPlayerData, other);
            }
        }
    }

    /**
     * Tell clients to delete info about the player
     */
    public static void removeSinglePlayerInfo(UUID playerUUID, MinecraftServer server) {
        PacketRemovePlayerData packet = new PacketRemovePlayerData(playerUUID);
        NetworkHandler.sendToAllPresent(packet, server);

    }

    /**
     * Updates data about a faction on all clients
     */
    public static void broadcastFactionUpdate(Faction faction, MinecraftServer server){
        Map<UUID, String> playerFactions = new HashMap<>();
        for (UUID member : faction.getMembers()){
            playerFactions.put(member, faction.getName());
        }

        Map<String, String> factionAlliances = new HashMap<>();
        Alliance alliance = AllianceStateManager.get(server).getAllianceByFaction(faction.getName());
        if(alliance != null) factionAlliances.put(faction.getName(), alliance.getName());

        PacketSyncFaction packet = new PacketSyncFaction(playerFactions, factionAlliances);
        NetworkHandler.sendToAllPresent(packet, server);
    }

    /**
     * Tells all clients that a faction has been disbanded
     */
    public static void broadcastFactionDisband(Faction faction, MinecraftServer server){
        for (UUID member : faction.getMembers()){
            removeSinglePlayerInfo(member, server);
        }
    }

    /**
     * Tells all clients that an alliance has been disbanded
     */
    public static void broadcastAllianceDisband(Alliance alliance, MinecraftServer server){
        for (String member : alliance.getMembers()){
            PacketFactionLeaveAlliance packet = new PacketFactionLeaveAlliance(member);
            NetworkHandler.sendToAllPresent(packet, server);
        }
    }

    /**
     * Tells all clients that a faction has left an alliance
     */
    public static void broadcastFactionAllianceLeave(Faction faction, MinecraftServer server){
        PacketFactionLeaveAlliance packet = new PacketFactionLeaveAlliance(faction.getName());
        NetworkHandler.sendToAllPresent(packet, server);
    }

    /**
     * Helper method. Gets all factions containing an online player
     */
    private static PacketSyncFaction getOnlinePlayerData(MinecraftServer server) {
        FactionStateManager factionManager = FactionStateManager.get(server);
        AllianceStateManager allianceManager = AllianceStateManager.get(server);

        // Player UUID, faction name
        Map<UUID, String> playerFactions = new HashMap<>();
        // Faction name, alliance name
        Map<String, String> factionAlliances = new HashMap<>();


        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            playerFactions.put(player.getUUID(), factionManager.getFactionByPlayer(player.getUUID()).getName());
        }

        for (String factionName : new HashSet<>(playerFactions.values())) {
            factionAlliances.put(factionName, allianceManager.getAllianceByFaction(factionName).getName());
        }

        return new PacketSyncFaction(playerFactions, factionAlliances);
    }

    /**
     * Gets the faction and alliance data for a single player
     *
     * @return His faction and alliance data if available, otherwise null
     */
    private static PacketSyncFaction getSinglePlayerData(UUID playerUUID, MinecraftServer server) {
        FactionStateManager factionManager = FactionStateManager.get(server);
        AllianceStateManager allianceManager = AllianceStateManager.get(server);

        Faction playerFaction = factionManager.getFactionByPlayer(playerUUID);

        if (playerFaction == null) return null;

        Map<UUID, String> playerFactions = Map.of(playerUUID, playerFaction.getName());

        Alliance factionAlliance = allianceManager.getAllianceByFaction(playerFaction.getName());

        if (factionAlliance == null) return new PacketSyncFaction(playerFactions, new HashMap<>());
        return new PacketSyncFaction(playerFactions, Map.of(playerFaction.getName(), factionAlliance.getName()));
    }

}