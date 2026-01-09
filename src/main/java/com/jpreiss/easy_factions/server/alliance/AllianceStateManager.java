package com.jpreiss.easy_factions.server.alliance;

import com.jpreiss.easy_factions.Utils;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.network.NetworkManager;
import com.jpreiss.easy_factions.server.ServerConfig;
import com.jpreiss.easy_factions.server.api.events.AllianceCreateEvent;
import com.jpreiss.easy_factions.server.api.events.AllianceDisbandEvent;
import com.jpreiss.easy_factions.server.api.events.AllianceJoinEvent;
import com.jpreiss.easy_factions.server.api.events.AllianceLeaveEvent;
import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Manages the state and business logic of the mod
 */
public class AllianceStateManager extends SavedData {
    private static final String DATA_NAME = "faction_alliance_data";

    public static final int MAX_ALLIANCE_SIZE = ServerConfig.maxAllianceSize;

    // In-memory data
    // Map<AllianceName, Alliance>
    private final Map<String, Alliance> alliances = new HashMap<>();

    // Map<FactionName, AllianceName> for fast lookups
    private final Map<String, String> factionAllianceMap = new HashMap<>();


    public static AllianceStateManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(AllianceStateManager::load, AllianceStateManager::create, DATA_NAME);
    }

    public static AllianceStateManager create() {
        return new AllianceStateManager();
    }

    /**
     * Creates a new alliance
     *
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

        Alliance alliance = new Alliance(name, null, new HashSet<>(Set.of(creatorFaction.getName())));
        alliances.put(name, alliance);
        factionAllianceMap.put(creatorFaction.getName(), name);

        MinecraftForge.EVENT_BUS.post(new AllianceCreateEvent(name, creatorFaction.getName()));
        Utils.refreshCommandTree(creator);


        NetworkManager.broadcastFactionUpdate(creatorFaction, server);

        this.setDirty();
    }

    /**
     * Invite a faction to the faction
     *
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
        NetworkManager.broadcastFactionUpdate(playerFaction, server);

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

        forceLeaveAlliance(playerFaction, server);
        Utils.refreshCommandTree(player);
    }

    /**
     * Leave the alliance. If the last faction leaves it, it is abandoned
     *
     * @param faction The faction to remove from its alliance
     * @throws RuntimeException If: the faction does not exist or is not part of an alliance
     */
    public void forceLeaveAlliance(Faction faction, MinecraftServer server) throws RuntimeException {
        String allianceName = factionAllianceMap.get(faction.getName());
        if (allianceName == null) throw new RuntimeException("The faction is not in an alliance.");

        Alliance alliance = alliances.get(allianceName);
        alliance.getMembers().remove(faction.getName());
        factionAllianceMap.remove(faction.getName());

        // Disband alliance if no members are left
        if (alliance.getMembers().isEmpty()) {
            disbandAlliance(alliance, server);
        } else {
            this.setDirty();
        }
        NetworkManager.broadcastFactionAllianceLeave(faction, server);

        MinecraftForge.EVENT_BUS.post(new AllianceLeaveEvent(allianceName, faction.getName()));
    }

    /**
     * Disband alliance
     *
     * @param alliance The alliance to disband
     */
    public void disbandAlliance(Alliance alliance, MinecraftServer server) {

        alliances.remove(alliance.getName());

        for (String factionName : alliance.getMembers()) {
            factionAllianceMap.remove(factionName);
        }

        NetworkManager.broadcastAllianceDisband(alliance, server);

        MinecraftForge.EVENT_BUS.post(new AllianceDisbandEvent(alliance));

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

    public Map<String, String> getFactionAllianceMap() {
        return Collections.unmodifiableMap(factionAllianceMap);
    }

    public void setAbbreviation(String allianceName, String abbreviation, MinecraftServer server) throws RuntimeException {
        if (!alliances.containsKey(allianceName)) throw new RuntimeException("The alliance does not exist.");
        alliances.get(allianceName).setAbbreviation(abbreviation);
        NetworkManager.broadcastAllianceAbbreviationUpdate(allianceName, abbreviation, server);
        this.setDirty();
    }

    public String getAbbreviation(String allianceName) {
        if (!alliances.containsKey(allianceName)) return null;
        return alliances.get(allianceName).getAbbreviation();
    }

    public void setRelation(String otherAllianceName, ServerPlayer player, RelationshipStatus status) throws  RuntimeException {
        MinecraftServer server = player.getServer();
        if(server == null) throw  new RuntimeException("The player is null.");

        FactionStateManager factionStateManager = FactionStateManager.get(player.getServer());
        Faction playerFaction = factionStateManager.getFactionByPlayer(player.getUUID());
        if(playerFaction == null) throw  new RuntimeException("Faction not found");

        Alliance playerAlliance = this.getAllianceByFaction(playerFaction.getName());
        Alliance otherAlliance = this.getAlliance(otherAllianceName);
        if(playerAlliance == null ||otherAlliance == null) throw  new RuntimeException("The alliance does not exist");

        if(status == RelationshipStatus.NEUTRAL){
            otherAlliance.getIncomingRelations().remove(playerFaction.getName());
            playerAlliance.getOutgoingRelations().remove(otherAllianceName);

        }
        else {
            otherAlliance.getIncomingRelations().put(playerAlliance.getName(), status);
            playerAlliance.getOutgoingRelations().put(otherAllianceName, status);
        }
        // TODO: send over network for tag
        this.setDirty();
    }


    /**
     * Loads the data from NBT
     */
    public static AllianceStateManager load(CompoundTag tag) {
        AllianceStateManager stateManager = new AllianceStateManager();

        if (tag.contains("Alliances", Tag.TAG_LIST)) {
            ListTag alliancesList = tag.getList("Alliances", Tag.TAG_COMPOUND);

            Alliance alliance;

            for (int i = 0; i < alliancesList.size(); i++) {
                alliance = Alliance.deserialize(alliancesList.getCompound(i));

                // Add to internal maps
                stateManager.alliances.put(alliance.getName(), alliance);

                // Rebuild lookup map
                for (String member : alliance.getMembers()) {
                    stateManager.factionAllianceMap.put(member, alliance.getName());
                }
            }
        }

        // All alliances loaded. Populate incomingRelations for each
        for (Alliance alliance : stateManager.alliances.values()) {
            for(Map.Entry<String, RelationshipStatus> entry: alliance.getOutgoingRelations().entrySet()){
                stateManager.alliances.get(entry.getKey()).getIncomingRelations().put(alliance.getName(), entry.getValue());
            }
        }

        return stateManager;
    }

    /**
     * Saves the data to NBT
     */
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag alliancesList = new ListTag();

        for (Alliance alliance : this.alliances.values()) {
            alliancesList.add(alliance.serialize());
        }

        tag.put("Alliances", alliancesList);
        return tag;
    }
}
