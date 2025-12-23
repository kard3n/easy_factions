package com.jpreiss.easy_factions.faction;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jpreiss.easy_factions.Config;
import com.jpreiss.easy_factions.Utils;
import com.jpreiss.easy_factions.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.api.events.FactionCreateEvent;
import com.jpreiss.easy_factions.api.events.FactionDisbandEvent;
import com.jpreiss.easy_factions.api.events.FactionJoinEvent;
import com.jpreiss.easy_factions.api.events.FactionLeaveEvent;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Manages the state and business logic of the mod
 */
public class FactionStateManager {
    private static final File SAVE_FILE = FMLPaths.GAMEDIR.get().resolve("faction_data.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int MAX_FACTION_SIZE = Config.maxFactionSize;

    private static final Logger LOGGER = LogUtils.getLogger();

    // In-memory data
    // Map<FactionName, FactionObject>
    private Map<String, Faction> factions = new HashMap<>();
    // Map<PlayerUUID, FactionName> for fast lookups
    private Map<UUID, String> playerFactionMap = new HashMap<>();

    // Singleton accessor
    private static final FactionStateManager INSTANCE = new FactionStateManager();

    public static FactionStateManager get() {
        return INSTANCE;
    }

    // Core logic

    /**
     * Creates a new faction
     * @param name Name of the faction to be created
     * @param leader Player creating the faction (will be the leader)
     * @throws RuntimeException If the name is taken or the player is already a member of a faction
     */
    public void createFaction(String name, ServerPlayer leader) throws RuntimeException{
        if (factions.containsKey(name) || playerFactionMap.containsKey(leader.getUUID())) throw new RuntimeException("Either the name is taken or you are already a member of a faction.");
        Faction f = new Faction(name, leader.getUUID());
        factions.put(name, f);
        playerFactionMap.put(leader.getUUID(), name);

        MinecraftForge.EVENT_BUS.post(new FactionCreateEvent(name, leader));

        Utils.refreshCommandTree(leader);

        save();
    }

    /**
     * Invite a player to the faction
     * @param invitingUser The user inviting
     * @param invitedUser The user being invited
     * @return The name of the faction the player was invited to
     * @throws RuntimeException If the player doesn't exist or the user is not the leader
     */
    public String invitePlayer(ServerPlayer invitingUser, ServerPlayer invitedUser) throws RuntimeException {
        Faction faction = getOwnedFaction(invitingUser.getUUID());
        if(faction.getMembers().size() >= MAX_FACTION_SIZE) throw new  RuntimeException("Your faction is already full. Please remove members before you invite more.");

        faction.getInvited().add(invitedUser.getUUID());

        save();

        return faction.getName();
    }

    /**
     * Accept a faction invite
     * @param player The player accepting the invite
     * @param factionName The name of the faction whose invite the player accepts
     * @throws RuntimeException if the user is already in a faction or hasn't been invited
     */
    public void joinFaction(ServerPlayer player, String factionName) throws RuntimeException {
        if (playerFactionMap.containsKey(player.getUUID())) throw new RuntimeException("You can't join another faction!");
        Faction f = factions.get(factionName);
        if (f == null || !f.getInvited().contains(player.getUUID())) throw new RuntimeException("You have not been invited to this faction or it does not exist.");
        if(f.getMembers().size() >= MAX_FACTION_SIZE) throw new  RuntimeException("This faction is already full.");

        f.getInvited().remove(player.getUUID());
        f.getMembers().add(player.getUUID());
        playerFactionMap.put(player.getUUID(), factionName);

        MinecraftForge.EVENT_BUS.post(new FactionJoinEvent(factionName, player));

        Utils.refreshCommandTree(player);

        save();
    }

    /**
     * Leave the faction. If the owner leaves it, it is disbanded
     * @param player The player leaving the faction
     * @throws RuntimeException if the player doesn't exist
     */
    public void leaveFaction(ServerPlayer player) throws RuntimeException {
        UUID playerUUID = player.getUUID();
        String factionName = playerFactionMap.get(playerUUID);
        if (factionName == null) throw new RuntimeException("You are not in a faction!.");
        Faction f = factions.get(factionName);

        if (f.getOwner().equals(playerUUID)) {
            MinecraftForge.EVENT_BUS.post(new FactionLeaveEvent(factionName, player.getUUID()));
            disbandFaction(factionName);
        } else {
            f.getMembers().remove(playerUUID);
            playerFactionMap.remove(playerUUID);

            MinecraftForge.EVENT_BUS.post(new FactionLeaveEvent(factionName, player.getUUID()));
            save();
        }
        Utils.refreshCommandTree(player);
    }

    /**
     * Leave the faction. If the owner leaves it, it is disbanded
     * @param leader The person executing this
     * @param targetName Name of the player being kicked
     * @throws RuntimeException if the player doesn't exist
     */
    public void kickFromFaction(ServerPlayer leader, String targetName, MinecraftServer server) throws RuntimeException {
        if (leader.getName().getString().equals(targetName)) throw new RuntimeException("You can't kick yourself from the faction!");

        // Look up UUID of the player, in case they are offline
        UUID playerUUID = Utils.getPlayerUUIDOffline(targetName, server);

        String targetFactionName = playerFactionMap.get(playerUUID);

        Faction leaderFaction = getOwnedFaction(leader.getUUID());

        if (targetFactionName == null || !targetFactionName.equals(leaderFaction.getName())) throw new RuntimeException("The player is not in your faction.");

        leaderFaction.getMembers().remove(playerUUID);
        playerFactionMap.remove(playerUUID);

        MinecraftForge.EVENT_BUS.post(new FactionLeaveEvent(leaderFaction.getName(), playerUUID));

        // Notify the player and refresh his command tree if he's online
        ServerPlayer targetOnline =server.getPlayerList().getPlayer(playerUUID);
        if (targetOnline != null) {
            Utils.refreshCommandTree(targetOnline);
            targetOnline.sendSystemMessage(Component.literal("You were kicked from the faction."));
        }


        save();
    }

    /**
     * Disband faction
     * @param name The faction to disband
     */
    public void disbandFaction(String name) {
        Faction faction = factions.get(name);
        if (faction == null) return;

        // Remove all members from lookup
        for (UUID member : faction.getMembers()) {
            playerFactionMap.remove(member);
        }

        // Remove this faction from its alliance
        try{
            AllianceStateManager.get().forceLeaveAlliance(faction);
        }
        catch (Exception ignored){}

        factions.remove(name);

        MinecraftForge.EVENT_BUS.post(new FactionDisbandEvent(faction));

        save();
    }

    /**
     * Enable/Disable friendly fire for this faction
     * @param requestingUser The user requesting the toggle
     * @param state The new state (enabled/disabled)
     * @throws RuntimeException If the player is not in a faction or not its owner
     */
    public void setFriendlyFire(ServerPlayer requestingUser, boolean state) throws RuntimeException {
        Faction faction = getOwnedFaction(requestingUser.getUUID());

        faction.setFriendlyFire(state);

        save();
    }

    public Faction getFactionByPlayer(UUID player) {
        String name = playerFactionMap.get(player);
        return name != null ? factions.get(name) : null;
    }

    public boolean playerIsInFaction(UUID player) {
        return playerFactionMap.containsKey(player);
    }

    public boolean playerOwnsFaction(UUID player) {
        Faction playerFaction = getFactionByPlayer(player);
        return (playerFaction != null && playerFaction.getOwner().equals(player));
    }

    public boolean factionExists(String factionName){
        return factions.get(factionName) != null;
    }

    /**
     * Returns the names
     */
    public List<String> getInvitesForPlayer(UUID playerUUID) {
        List<String> invitedFactionNames = new ArrayList<>();
        for (Faction f : factions.values()) {
            if (f.getInvited().contains(playerUUID)) {
                invitedFactionNames.add(f.getName());
            }
        }
        return invitedFactionNames;
    }

    /**
     * Returns the faction the player owns
     * @param player The player whose faction to get
     * @return His owned faction
     * @throws RuntimeException If he is not in a faction, or not the owner of the one he is in
     */
    public Faction getOwnedFaction(UUID player) throws RuntimeException {
        Faction playerFaction = getFactionByPlayer(player);
        if (playerFaction == null) throw new RuntimeException("You are not in a faction.");
        if (!playerFaction.getOwner().equals(player)) throw new RuntimeException("You are not the owner of the faction you are in.");
        return playerFaction;
    }


    /**
     * Loads the data from the file
     */
    public void load() {
        if (!SAVE_FILE.exists()) {
            this.factions = new HashMap<>();
            this.playerFactionMap = new HashMap<>();
            return;
        }

        try (Reader reader = new FileReader(SAVE_FILE)) {
            Type type = new TypeToken<Map<String, Faction>>() {}.getType();
            Map<String, Faction> loadedData = GSON.fromJson(reader, type);

            if (loadedData != null) {
                this.factions = loadedData;

                // Rebuild lookup map
                this.playerFactionMap.clear();
                for (Faction f : factions.values()) {
                    for (UUID member : f.getMembers()) {
                        playerFactionMap.put(member, f.getName());
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(SAVE_FILE)) {
            GSON.toJson(factions, writer);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }
}