package com.jpreiss.easy_factions.alliance;

import com.jpreiss.easy_factions.Config;
import com.jpreiss.easy_factions.Utils;
import com.jpreiss.easy_factions.api.events.AllianceCreateEvent;
import com.jpreiss.easy_factions.api.events.AllianceDisbandEvent;
import com.jpreiss.easy_factions.api.events.AllianceJoinEvent;
import com.jpreiss.easy_factions.api.events.AllianceLeaveEvent;
import com.jpreiss.easy_factions.faction.Faction;
import com.jpreiss.easy_factions.faction.FactionStateManager;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.MinecraftForge;
import org.slf4j.Logger;

import java.util.*;

/**
 * Manages the state and business logic of the mod
 */
public class AllianceStateManager extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_NAME = "faction_alliance_data";

    public static final int MAX_ALLIANCE_SIZE = Config.maxAllianceSize;

    // In-memory data
    // Map<AllianceName, Alliance>
    private final Map<String, Alliance> alliances = new HashMap<>();

    // Map<FactionName, AllianceName> for fast lookups
    private final Map<String, String> factionAllianceMap = new HashMap<>();


    public static AllianceStateManager get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(AllianceStateManager::load, AllianceStateManager::create, DATA_NAME);
    }

    public static AllianceStateManager create() {
        return new AllianceStateManager();
    }

    /**
     * Creates a new alliance
     * @param name    Name of the alliance to be created
     * @param creator Player creating the alliance
     * @throws RuntimeException If the name is taken or the player is not owner of a faction or the max faction size has been reached
     */
    public void createAlliance(String name, ServerPlayer creator, MinecraftServer server) throws RuntimeException {
        // NOTE: If FactionStateManager is also converted to SavedData, you might need to pass 'creator.serverLevel()' here.
        Faction creatorFaction = FactionStateManager.get(server).getOwnedFaction(creator.getUUID());

        // Check that the faction isn't in an alliance
        if (factionAllianceMap.containsKey(creatorFaction.getName()))
            throw new RuntimeException("Your faction is already in the following alliance: \"" + factionAllianceMap.get(name) + "\"");

        Alliance alliance = new Alliance(name, new HashSet<>(Set.of(creatorFaction.getName())));
        alliances.put(name, alliance);
        factionAllianceMap.put(creatorFaction.getName(), name);

        MinecraftForge.EVENT_BUS.post(new AllianceCreateEvent(name, creatorFaction.getName()));
        Utils.refreshCommandTree(creator);

        this.setDirty();
    }

    /**
     * Invite a faction to the faction
     * @param invitingUser       The user inviting
     * @param invitedFactionName The name of the faction being invited
     * @throws RuntimeException If the player doesn't exist or the user is not the leader
     */
    public void inviteFaction(ServerPlayer invitingUser, String invitedFactionName, MinecraftServer server) throws RuntimeException {
        Faction invitingFaction = FactionStateManager.get(server).getOwnedFaction(invitingUser.getUUID());
        if (!factionAllianceMap.containsKey(invitingFaction.getName()))
            throw new RuntimeException("Your faction is not in an alliance. Create one first.");
        if (!FactionStateManager.get(server).factionExists(invitedFactionName))
            throw new RuntimeException("The invited faction does not exist.");
        Alliance alliance = alliances.get(factionAllianceMap.get(invitingFaction.getName()));
        if (alliance.getMembers().contains(invitedFactionName))
            throw new RuntimeException("The requested faction is already in your alliance.");
        if (alliance.getMembers().size() >= MAX_ALLIANCE_SIZE)
            throw new RuntimeException("Your alliance has already reached its maximum amount of members.");


        alliance.getInvited().add(invitedFactionName);
        this.setDirty();
    }

    /**
     * Accept an alliance invite
     *
     * @param player       The player accepting the invite
     * @param allianceName The name of the alliance whose invite the player's faction accepts
     * @throws RuntimeException if the user is already in a faction or hasn't been invited, or if the alliance is already full.
     */
    public void joinAlliance(ServerPlayer player, String allianceName, MinecraftServer server) throws RuntimeException {
        Faction playerFaction = FactionStateManager.get(server).getOwnedFaction(player.getUUID());

        if (factionAllianceMap.containsKey(playerFaction.getName()))
            throw new RuntimeException("Your faction is already in an alliance. Leave it first to join this one.");
        if (!alliances.containsKey(allianceName)) throw new RuntimeException("The requested alliance does not exist.");
        Alliance alliance = alliances.get(allianceName);

        if (!alliance.getInvited().contains(playerFaction.getName()))
            throw new RuntimeException("You are not invited to the requested alliance.");

        if (alliance.getMembers().size() >= MAX_ALLIANCE_SIZE)
            throw new RuntimeException("The alliance has already reached its maximum amount of members.");

        alliance.getInvited().remove(playerFaction.getName());
        alliance.getMembers().add(playerFaction.getName());

        factionAllianceMap.put(playerFaction.getName(), allianceName);

        MinecraftForge.EVENT_BUS.post(new AllianceJoinEvent(allianceName, playerFaction.getName()));
        Utils.refreshCommandTree(player);

        this.setDirty();
    }

    /**
     * Leave the alliance. If the last faction leaves it, it is abandoned
     *
     * @param player The player requesting his faction to leave
     * @throws RuntimeException If: the player is not in a faction or not the leader, or if the faction is not in an alliance
     */
    public void leaveAlliance(ServerPlayer player, MinecraftServer server) throws RuntimeException {
        Faction playerFaction = FactionStateManager.get(server).getOwnedFaction(player.getUUID());

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
        String allianceName = factionAllianceMap.get(faction.getName());
        if (allianceName == null) throw new RuntimeException("The faction is not in an alliance.");

        Alliance alliance = alliances.get(allianceName);
        alliance.getMembers().remove(faction.getName());
        factionAllianceMap.remove(faction.getName());

        // Disband alliance if no members are left
        if (alliance.getMembers().isEmpty()) {
            disbandAlliance(alliance);
        } else {
            this.setDirty();
        }

        MinecraftForge.EVENT_BUS.post(new AllianceLeaveEvent(allianceName, faction.getName()));
    }

    /**
     * Disband alliance
     *
     * @param alliance The alliance to disband
     */
    public void disbandAlliance(Alliance alliance) {
        for (String factionName : alliance.getMembers()) {
            factionAllianceMap.remove(factionName);
        }

        alliances.remove(alliance.getName());

        MinecraftForge.EVENT_BUS.post(new AllianceDisbandEvent(alliance.getName()));

        this.setDirty();
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
            if (f.getInvited().contains(factionName)) invitedAllianceNames.add(f.getName());
        }
        return invitedAllianceNames;
    }

    public Set<String> getAllianceNames() {
        return alliances.keySet();
    }

    public Alliance getAlliance(String allianceName) {
        return alliances.get(allianceName);
    }

    /**
     * Loads the data from NBT
     */
    public static AllianceStateManager load(CompoundTag tag) {
        AllianceStateManager data = new AllianceStateManager();

        if (tag.contains("Alliances", Tag.TAG_LIST)) {
            ListTag alliancesList = tag.getList("Alliances", Tag.TAG_COMPOUND);

            for (int i = 0; i < alliancesList.size(); i++) {
                CompoundTag allianceTag = alliancesList.getCompound(i);

                String name = allianceTag.getString("Name");
                Set<String> members = new HashSet<>();
                Set<String> invited = new HashSet<>();

                // Load Members
                ListTag membersTag = allianceTag.getList("Members", Tag.TAG_STRING);
                for (int j = 0; j < membersTag.size(); j++) {
                    members.add(membersTag.getString(j));
                }

                // Load Invites
                ListTag invitedTag = allianceTag.getList("Invited", Tag.TAG_STRING);
                for (int j = 0; j < invitedTag.size(); j++) {
                    invited.add(invitedTag.getString(j));
                }

                // Create Alliance Object
                Alliance alliance = new Alliance(name, members);
                alliance.getInvited().addAll(invited);

                // Add to internal maps
                data.alliances.put(name, alliance);

                // Rebuild lookup map
                for (String member : members) {
                    data.factionAllianceMap.put(member, name);
                }
            }
        }

        return data;
    }

    /**
     * Saves the data to NBT
     */
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag alliancesList = new ListTag();

        for (Alliance alliance : this.alliances.values()) {
            CompoundTag allianceTag = new CompoundTag();
            allianceTag.putString("Name", alliance.getName());

            // Save Members
            ListTag membersTag = new ListTag();
            for (String member : alliance.getMembers()) {
                membersTag.add(StringTag.valueOf(member));
            }
            allianceTag.put("Members", membersTag);

            // Save Invited
            ListTag invitedTag = new ListTag();
            for (String invitedFaction : alliance.getInvited()) {
                invitedTag.add(StringTag.valueOf(invitedFaction));
            }
            allianceTag.put("Invited", invitedTag);

            alliancesList.add(allianceTag);
        }

        tag.put("Alliances", alliancesList);
        return tag;
    }
}