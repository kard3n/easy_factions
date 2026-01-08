package com.jpreiss.easy_factions.network;

import com.jpreiss.easy_factions.network.packet.*;
import com.jpreiss.easy_factions.server.alliance.Alliance;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
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

        PacketSyncFactionAlliance onlinePlayerData = getOnlinePlayerData(server);
        // Send faction state to the new player
        NetworkHandler.sendToPlayer(onlinePlayerData, player);
    }

    /**
     * Send information about a player to all others
     */
    public static void broadcastPlayerInfo(ServerPlayer newPlayer, MinecraftServer server) {

        // Send info about the new players affiliations to the rest
        PacketSyncFactionAlliance newPlayerData = getSinglePlayerData(newPlayer.getUUID(), server);
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

        Map<String, String> factionToAbbreviation = new  HashMap<>();
        if(faction.getAbbreviation() != null) factionToAbbreviation.put(faction.getName(), faction.getAbbreviation());

        Map<String, String> allianceToAbbreviation = new HashMap<>();
        if(alliance != null && alliance.getAbbreviation() != null) allianceToAbbreviation.put(alliance.getName(), alliance.getAbbreviation());

        PacketSyncFactionAlliance packet = new PacketSyncFactionAlliance(playerFactions, factionAlliances, factionToAbbreviation, allianceToAbbreviation);
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
     * Broadcasts a faction abbreviation change
     */
    public static void broadcastFactionAbbreviationUpdate(String factionName, String abbreviation, MinecraftServer server){
        PacketUpdateFactionAbbreviation packet = new PacketUpdateFactionAbbreviation(factionName, abbreviation);
        NetworkHandler.sendToAllPresent(packet, server);
    }

    /**
     * Broadcasts an alliance abbreviation change
     */
    public static void broadcastAllianceAbbreviationUpdate(String allianceName, String abbreviation, MinecraftServer server){
        PacketUpdateAllianceAbbreviation packet = new PacketUpdateAllianceAbbreviation(allianceName, abbreviation);
        NetworkHandler.sendToAllPresent(packet, server);
    }


    /**
     * Helper method. Gets all factions containing at least one online player
     */
    private static PacketSyncFactionAlliance getOnlinePlayerData(MinecraftServer server) {
        FactionStateManager factionManager = FactionStateManager.get(server);
        AllianceStateManager allianceManager = AllianceStateManager.get(server);

        // Player UUID, faction name
        Map<UUID, String> playerFactions = new HashMap<>();
        // Faction name, alliance name
        Map<String, String> factionAlliances = new HashMap<>();

        Map<String, String> factionToAbbreviation = new  HashMap<>();
        Map<String, String> allianceToAbbreviation = new HashMap<>();


        Faction faction;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            faction = factionManager.getFactionByPlayer(player.getUUID());
            if (faction != null){
                playerFactions.put(player.getUUID(), faction.getName());
                if(faction.getAbbreviation() != null) factionToAbbreviation.put(faction.getName(), faction.getAbbreviation());

            }
        }

        Alliance alliance;
        for (String factionName : new HashSet<>(playerFactions.values())) {
            alliance = allianceManager.getAllianceByFaction(factionName);
            if (alliance != null){
                factionAlliances.put(factionName, alliance.getName());
                if(alliance.getAbbreviation() != null) allianceToAbbreviation.put(alliance.getName(), alliance.getAbbreviation());
            }
        }



        return new PacketSyncFactionAlliance(playerFactions, factionAlliances, factionToAbbreviation, allianceToAbbreviation);
    }

    /**
     * Gets the faction and alliance data for a single player
     *
     * @return His faction and alliance data if available, otherwise null
     */
    private static PacketSyncFactionAlliance getSinglePlayerData(UUID playerUUID, MinecraftServer server) {
        FactionStateManager factionManager = FactionStateManager.get(server);
        AllianceStateManager allianceManager = AllianceStateManager.get(server);

        Faction playerFaction = factionManager.getFactionByPlayer(playerUUID);

        if (playerFaction == null) return null;

        Map<UUID, String> playerFactions = Map.of(playerUUID, playerFaction.getName());
        Map<String, String> factionAbbreviatons = new HashMap<>();
        if(playerFaction.getAbbreviation() != null) factionAbbreviatons.put(playerFaction.getName(), playerFaction.getAbbreviation());

        Alliance factionAlliance = allianceManager.getAllianceByFaction(playerFaction.getName());
        Map<String, String> factionAlliances = new HashMap<>();
        if(factionAlliance != null) factionAlliances.put(playerFaction.getName(), factionAlliance.getName());

        Map<String, String> allianceAbbreviations = new HashMap<>();
        if(factionAlliance != null && factionAlliance.getAbbreviation() != null) allianceAbbreviations.put(factionAlliance.getName(), factionAlliance.getAbbreviation());

        return new PacketSyncFactionAlliance(playerFactions, factionAlliances, factionAbbreviatons, allianceAbbreviations);
    }

}