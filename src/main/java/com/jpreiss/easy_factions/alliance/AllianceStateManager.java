package com.jpreiss.easy_factions.alliance;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jpreiss.easy_factions.Config;
import com.jpreiss.easy_factions.Utils;
import com.jpreiss.easy_factions.api.events.AllianceCreateEvent;
import com.jpreiss.easy_factions.api.events.AllianceDisbandEvent;
import com.jpreiss.easy_factions.api.events.AllianceJoinEvent;
import com.jpreiss.easy_factions.api.events.AllianceLeaveEvent;
import com.jpreiss.easy_factions.faction.Faction;
import com.jpreiss.easy_factions.faction.FactionStateManager;
import com.mojang.logging.LogUtils;
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
public class AllianceStateManager {
    private static final File SAVE_FILE = FMLPaths.GAMEDIR.get().resolve("faction_alliance_data.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int MAX_ALLIANCE_SIZE = Config.maxAllianceSize;

    private static final Logger LOGGER = LogUtils.getLogger();

    // In-memory data
    // Map<AllianceName, Alliance>
    private Map<String, Alliance> alliances = new HashMap<>();

    // Map<FactionName, AllianceName> for fast lookups
    private Map<String, String> factionAllianceMap = new HashMap<>();


    // Singleton accessor
    private static final AllianceStateManager INSTANCE = new AllianceStateManager();

    public static AllianceStateManager get() {
        return INSTANCE;
    }

    // Core logic

    /**
     * Creates a new alliance
     *
     * @param name    Name of the alliance to be created
     * @param creator Player creating the alliance
     * @throws RuntimeException If the name is taken or the player is not owner of a faction or the max faction size has been reached
     */
    public void createAlliance(String name, ServerPlayer creator) throws RuntimeException {
        Faction creatorFaction = FactionStateManager.get().getOwnedFaction(creator.getUUID());

        // Check that the faction isn't in an alliance
        if (factionAllianceMap.containsKey(creatorFaction.name))
            throw new RuntimeException("Your faction is already in the following alliance: \"" + factionAllianceMap.get(name) + "\"");

        Alliance alliance = new Alliance(name, Set.of(creatorFaction.name));
        alliances.put(name, alliance);
        factionAllianceMap.put(creatorFaction.name, name);

        MinecraftForge.EVENT_BUS.post(new AllianceCreateEvent(name, creatorFaction.name));
        Utils.refreshCommandTree(creator);

        save();
    }

    /**
     * Invite a player to the faction
     *
     * @param invitingUser       The user inviting
     * @param invitedFactionName The name of the faction being invited
     * @throws RuntimeException If the player doesn't exist or the user is not the leader
     */
    public void inviteFaction(ServerPlayer invitingUser, String invitedFactionName) throws RuntimeException {
        Faction invitingFaction = FactionStateManager.get().getOwnedFaction(invitingUser.getUUID());
        if (!factionAllianceMap.containsKey(invitingFaction.name))
            throw new RuntimeException("Your faction is not in an alliance. Create one first.");
        if (!FactionStateManager.get().factionExists(invitedFactionName))
            throw new RuntimeException("The invited faction does not exist.");
        Alliance alliance = alliances.get(factionAllianceMap.get(invitingFaction.name));
        if (alliance.members.contains(invitedFactionName))
            throw new RuntimeException("The requested faction is already in your alliance.");
        if (alliance.members.size() >= MAX_ALLIANCE_SIZE)
            throw new RuntimeException("Your alliance has already reached its maximum amount of members.");

        Faction invitedFaction = FactionStateManager.get().getFactionByPlayer(invitingUser.getUUID());
        if (invitedFaction == null) throw new RuntimeException("The invited faction does not exist.");

        alliance.invited.add(invitedFactionName);
        save();
    }

    /**
     * Accept an alliance invite
     *
     * @param player       The player accepting the invite
     * @param allianceName The name of the alliance whose invite the player's faction accepts
     * @throws RuntimeException if the user is already in a faction or hasn't been invited, or if the alliance is already full.
     */
    public void joinAlliance(ServerPlayer player, String allianceName) throws RuntimeException {
        Faction playerFaction = FactionStateManager.get().getOwnedFaction(player.getUUID());

        if (factionAllianceMap.containsKey(playerFaction.name))
            throw new RuntimeException("Your faction is already in an alliance. Leave it first to join this one.");
        if (!alliances.containsKey(allianceName)) throw new RuntimeException("The requested alliance does not exist.");
        Alliance alliance = alliances.get(allianceName);

        if (!alliance.invited.contains(playerFaction.name))
            throw new RuntimeException("You are not invited to the requested alliance.");

        if (alliance.members.size() >= MAX_ALLIANCE_SIZE)
            throw new RuntimeException("The alliance has already reached its maximum amount of members.");

        alliance.invited.remove(playerFaction.name);
        alliance.members.add(playerFaction.name);

        MinecraftForge.EVENT_BUS.post(new AllianceJoinEvent(allianceName, playerFaction.name));
        Utils.refreshCommandTree(player);

        save();
    }

    /**
     * Leave the alliance. If the last faction leaves it, it is abandoned
     *
     * @param player The player requesting his faction to leave
     * @throws RuntimeException If: the player is not in a faction or not the leader, or if the faction is not in an alliance
     */
    public void leaveAlliance(ServerPlayer player) throws RuntimeException {
        Faction playerFaction = FactionStateManager.get().getOwnedFaction(player.getUUID());

        forceLeaveAlliance(playerFaction);
        Utils.refreshCommandTree(player);
    }

    /**
     * Leave the alliance. If the last faction leaves it, it is abandoned
     *
     * @param faction The faction to remove from its alliance
     * @throws RuntimeException If: the faction does not exist or is not part of an alliance
     */
    public void forceLeaveAlliance(Faction faction) throws RuntimeException {
        String allianceName = factionAllianceMap.get(faction.name);
        if (allianceName == null) throw new RuntimeException("The faction is not in an alliance.");

        Alliance alliance = alliances.get(allianceName);
        alliance.members.remove(faction.name);
        factionAllianceMap.remove(faction.name);

        // Disband alliance if no members are left
        if (alliance.members.isEmpty()) disbandAlliance(alliance);

        MinecraftForge.EVENT_BUS.post(new AllianceLeaveEvent(allianceName, faction.name));

        save();
    }

    /**
     * Disband alliance
     *
     * @param alliance The alliance to disband
     */
    public void disbandAlliance(Alliance alliance) {
        for (String factionName : alliance.members) {
            factionAllianceMap.remove(factionName);
        }

        alliances.remove(alliance.name);

        MinecraftForge.EVENT_BUS.post(new AllianceDisbandEvent(alliance.name));

        save();
    }

    /**
     * Returns the alliance a faction is in
     *
     * @param factionName The name of the faction
     * @return The alliance the faction is in
     */
    public Alliance getAllianceByFaction(String factionName) {
        return alliances.get(factionAllianceMap.get(factionName));
    }

    /**
     * Returns the names of the alliances the faction is invited to.
     */
    public List<String> getInvitesForFaction(String factionName) {
        List<String> invitedAllianceNames = new ArrayList<>();
        for (Alliance f : alliances.values()) {
            if (f.invited.contains(factionName)) invitedAllianceNames.add(f.name);
        }
        return invitedAllianceNames;
    }

    public Set<String> getAllianceNames() {
        return alliances.keySet();
    }

    /**
     * Gets an alliance by its name
     * @param allianceName The name of the alliance
     * @return The alliance or null if it doesn't exist
     */
    public Alliance getAlliance(String allianceName) {
        return alliances.get(allianceName);
    }


    /**
     * Loads the data from the file
     */
    public void load() {
        if (!SAVE_FILE.exists()) {
            this.alliances = new HashMap<>();
            this.factionAllianceMap = new HashMap<>();
            return;
        }

        try (Reader reader = new FileReader(SAVE_FILE)) {
            Type type = new TypeToken<Map<String, Alliance>>() {
            }.getType();
            Map<String, Alliance> loadedData = GSON.fromJson(reader, type);

            if (loadedData != null) {
                this.alliances = loadedData;

                // Rebuild lookup map
                this.factionAllianceMap.clear();
                for (Alliance alliance : alliances.values()) {
                    for (String member : alliance.members) {
                        factionAllianceMap.put(member, alliance.name);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public void save() {
        try (Writer writer = new FileWriter(SAVE_FILE)) {
            GSON.toJson(alliances, writer);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }
}