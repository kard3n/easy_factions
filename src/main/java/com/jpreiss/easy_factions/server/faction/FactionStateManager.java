package com.jpreiss.easy_factions.server.faction;

import com.jpreiss.easy_factions.Utils;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.network.NetworkManager;
import com.jpreiss.easy_factions.server.ServerConfig;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.api.events.FactionCreateEvent;
import com.jpreiss.easy_factions.server.api.events.FactionDisbandEvent;
import com.jpreiss.easy_factions.server.api.events.FactionJoinEvent;
import com.jpreiss.easy_factions.server.api.events.FactionLeaveEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Manages the state and business logic of the mod
 */
public class FactionStateManager extends SavedData {
    private static final String DATA_NAME = "faction_data";

    public static final int MAX_FACTION_SIZE = ServerConfig.maxFactionSize;

    // In-memory data
    // Map<FactionName, FactionObject>
    private final Map<String, Faction> factions = new HashMap<>();

    // Map<PlayerUUID, FactionName> for fast lookups
    private final Map<UUID, String> playerFactionMap = new HashMap<>();


    public static FactionStateManager get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(FactionStateManager::load, FactionStateManager::create, DATA_NAME);
    }

    public static FactionStateManager create() {
        return new FactionStateManager();
    }


    /**
     * Creates a new faction
     */
    public void createFaction(String name, String abbreviation, ServerPlayer leader, MinecraftServer server) throws RuntimeException {
        if (factions.containsKey(name) || playerFactionMap.containsKey(leader.getUUID()))
            throw new RuntimeException("Either the name is taken or you are already a member of a faction.");
        Faction faction = new Faction(name, abbreviation, leader.getUUID());
        factions.put(name, faction);
        playerFactionMap.put(leader.getUUID(), name);

        MinecraftForge.EVENT_BUS.post(new FactionCreateEvent(name, leader));

        Utils.refreshCommandTree(leader);
        NetworkManager.broadcastFactionUpdate(faction, server);

        this.setDirty();
    }

    /**
     * Invite a player to the faction
     */
    public String invitePlayer(ServerPlayer invitingUser, ServerPlayer invitedUser) throws RuntimeException {
        Faction faction = getFactionByPlayer(invitingUser.getUUID());
        if (faction.getMembers().size() >= MAX_FACTION_SIZE)
            throw new RuntimeException("Your faction is already full. Please remove members before you invite more.");

        faction.getInvited().add(invitedUser.getUUID());

        this.setDirty();

        return faction.getName();
    }

    /**
     * Accept a faction invite
     */
    public void joinFaction(ServerPlayer player, String factionName, MinecraftServer server) throws RuntimeException {
        if (playerFactionMap.containsKey(player.getUUID()))
            throw new RuntimeException("You can't join another faction!");
        Faction f = factions.get(factionName);
        if (f == null || !f.getInvited().contains(player.getUUID()))
            throw new RuntimeException("You have not been invited to this faction or it does not exist.");
        if (f.getMembers().size() >= MAX_FACTION_SIZE) throw new RuntimeException("This faction is already full.");

        f.getInvited().remove(player.getUUID());
        f.getMembers().add(player.getUUID());
        playerFactionMap.put(player.getUUID(), factionName);

        MinecraftForge.EVENT_BUS.post(new FactionJoinEvent(factionName, player));

        Utils.refreshCommandTree(player);
        NetworkManager.broadcastPlayerInfo(player, server);

        this.setDirty();
    }

    /**
     * Leave the faction. If the owner leaves it, it is disbanded
     */
    public void leaveFaction(ServerPlayer player, MinecraftServer server) throws RuntimeException {
        UUID playerUUID = player.getUUID();
        String factionName = playerFactionMap.get(playerUUID);

        if (factionName == null) throw new RuntimeException("You are not in a faction!.");

        Faction f = factions.get(factionName);

        if (f.getOwner().equals(playerUUID)) {
            MinecraftForge.EVENT_BUS.post(new FactionLeaveEvent(factionName, player.getUUID()));
            disbandFaction(factionName, server);
        } else {
            forceRemoveMember(f, playerUUID);
        }
        Utils.refreshCommandTree(player);
        NetworkManager.broadcastPlayerInfo(player, server);
    }

    /**
     * Leave the faction. If the owner leaves it, it is disbanded
     *
     * @param kickingUser The person executing this
     * @param targetName  Name of the player being kicked
     * @throws RuntimeException if the player doesn't exist
     */
    public void kickFromFaction(ServerPlayer kickingUser, String targetName, MinecraftServer server) throws RuntimeException {
        if (kickingUser.getName().getString().equals(targetName))
            throw new RuntimeException("You can't kick yourself from the faction!");

        // Look up UUID of the player, in case they are offline
        UUID targetUUID = Utils.getPlayerUUIDOffline(targetName, server);

        String targetFactionName = playerFactionMap.get(targetUUID);
        Faction kickingUserFaction = getFactionByPlayer(kickingUser.getUUID());

        if (targetFactionName == null || !targetFactionName.equals(kickingUserFaction.getName()))
            throw new RuntimeException("The player is not in your faction.");

        boolean kickingUserIsOfficer = kickingUserFaction.getOfficers().contains(kickingUser.getUUID());

        if (!(kickingUserFaction.getOwner().equals(kickingUser.getUUID()) || kickingUserIsOfficer)) {
            throw new RuntimeException("You are neither the owner nor an officer of the faction.");
        }

        // Officers can't kick other officers
        if (kickingUserIsOfficer && (kickingUserFaction.getOfficers().contains(targetUUID) || kickingUserFaction.getOwner().equals(targetUUID))) {
            throw new RuntimeException("You can't kick other officers or the owner from the faction!");
        }

        forceRemoveMember(kickingUserFaction, targetUUID);

        // Notify the player and refresh his command tree if he's online
        ServerPlayer targetOnline = server.getPlayerList().getPlayer(targetUUID);
        if (targetOnline != null) {
            Utils.refreshCommandTree(targetOnline);
            targetOnline.sendSystemMessage(Component.literal("You were kicked from the faction."));
            NetworkManager.removeSinglePlayerInfo(targetUUID, server);
        }

    }

    /**
     * Forcefully removes the player from the faction
     */
    private void forceRemoveMember(Faction faction, UUID player) {
        faction.getMembers().remove(player);
        faction.getInvited().remove(player);
        faction.getOfficers().remove(player);
        playerFactionMap.remove(player);
        MinecraftForge.EVENT_BUS.post(new FactionLeaveEvent(faction.getName(), player));
        this.setDirty();
    }

    /**
     * Disband faction
     *
     * @param name The faction to disband
     */
    public void disbandFaction(String name, MinecraftServer server) {
        Faction faction = factions.get(name);
        if (faction == null) return;

        // Remove all members from lookup
        for (UUID member : faction.getMembers()) {
            playerFactionMap.remove(member);
        }

        // Remove this faction from its alliance
        try {
            AllianceStateManager.get(server).forceLeaveAlliance(faction, server);
        } catch (Exception ignored) {
        }

        factions.remove(name);

        MinecraftForge.EVENT_BUS.post(new FactionDisbandEvent(faction));

        NetworkManager.broadcastFactionDisband(faction, server);
        this.setDirty();
    }

    /**
     * Enable/Disable friendly fire for this faction
     *
     * @param requestingUser The user requesting the toggle
     * @param state          The new state (enabled/disabled)
     * @throws RuntimeException If the player is not in a faction or not its owner
     */
    public void setFriendlyFire(ServerPlayer requestingUser, boolean state) throws RuntimeException {
        Faction faction = getFactionByPlayer(requestingUser.getUUID());
        faction.setFriendlyFire(state);
        this.setDirty();
    }

    /**
     * Makes an existing faction member an officer
     */
    public void addOfficer(String newOfficerName, ServerPlayer invitingLeader, MinecraftServer server) throws RuntimeException {
        UUID newOfficer = Utils.getPlayerUUIDOffline(newOfficerName, server); // Works offline

        Faction officerFaction = getFactionByPlayer(newOfficer);
        if (officerFaction == null) throw new RuntimeException("Can only invite faction members!");
        Faction leaderFaction = getOwnedFaction(invitingLeader.getUUID());

        if (!leaderFaction.getName().equals(officerFaction.getName())) {
            throw new RuntimeException("The player is not in your faction!");
        }

        if (leaderFaction.getOfficers().contains(newOfficer)) {
            throw new RuntimeException("This player is already an officer in your faction!");
        }
        leaderFaction.getOfficers().add(newOfficer);

        // update officer command tree
        ServerPlayer targetOnline = server.getPlayerList().getPlayer(newOfficer);
        if (targetOnline != null) {
            Utils.refreshCommandTree(targetOnline);
            targetOnline.sendSystemMessage(Component.literal("You are now an officer of your faction."));
        }

        this.setDirty();
    }

    /**
     * Demotes an officer
     */
    public void removeOfficer(String officerName, ServerPlayer leader, MinecraftServer server) throws RuntimeException {
        UUID newOfficer = Utils.getPlayerUUIDOffline(officerName, server); // Works offline

        Faction leaderFaction = getOwnedFaction(leader.getUUID());

        if (!leaderFaction.getOfficers().contains(newOfficer)) {
            throw new RuntimeException("The player is not an officer in your faction!");
        }

        leaderFaction.getOfficers().remove(newOfficer);

        // update officer command tree
        ServerPlayer targetOnline = server.getPlayerList().getPlayer(newOfficer);
        if (targetOnline != null) {
            Utils.refreshCommandTree(targetOnline);
            targetOnline.sendSystemMessage(Component.literal("You are no longer an officer of your faction."));
        }

        this.setDirty();
    }

    public void setRelation(String otherFactionName, ServerPlayer player, RelationshipStatus status) throws  RuntimeException {
        Faction playerFaction = getFactionByPlayer(player.getUUID());
        Faction otherFaction = getFactionByName(otherFactionName);
        if(playerFaction == null || otherFaction == null) throw  new RuntimeException("The faction does not exist");

        if(status == RelationshipStatus.NEUTRAL){
            playerFaction.getOutgoingRelations().remove(otherFactionName);
            otherFaction.getIncomingRelations().remove(playerFaction.getName());
        }
        else {
            playerFaction.getOutgoingRelations().put(otherFactionName, status);
            otherFaction.getIncomingRelations().put(playerFaction.getName(), status);
        }
        // TODO: send over network for tag
        this.setDirty();
    }

    public Faction getFactionByPlayer(UUID player) {
        String name = playerFactionMap.get(player);
        return name != null ? factions.get(name) : null;
    }

    public Faction getFactionByName(String name) {
        return factions.get(name);
    }

    public boolean playerIsInFaction(UUID player) {
        return playerFactionMap.containsKey(player);
    }

    public boolean playerOwnsFaction(UUID player) {
        Faction playerFaction = getFactionByPlayer(player);
        return (playerFaction != null && playerFaction.getOwner().equals(player));
    }

    /**
     * Returns true when the player is either the owner or an officer of a faction
     */
    public boolean playerIsOwnerOrOfficer(UUID player) {
        Faction playerFaction = getFactionByPlayer(player);
        return (playerFaction != null && (playerFaction.getOwner().equals(player) || playerFaction.getOfficers().contains(player)));
    }

    public boolean factionExists(String factionName) {
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
     *
     * @param player The player whose faction to get
     * @return His owned faction
     * @throws RuntimeException If he is not in a faction, or not the owner of the one he is in
     */
    public Faction getOwnedFaction(UUID player) throws RuntimeException {
        Faction playerFaction = getFactionByPlayer(player);
        if (playerFaction == null) throw new RuntimeException("You are not in a faction.");
        if (!playerFaction.getOwner().equals(player))
            throw new RuntimeException("You are not the owner of the faction you are in.");
        return playerFaction;
    }

    /**
     * Set the abbreviation for a faction
     *
     * @param factionName  Name of the faction
     * @param abbreviation The abbreviation
     */
    public void setAbbreviation(String factionName, String abbreviation, MinecraftServer server) throws RuntimeException {
        if (!factions.containsKey(factionName)) throw new RuntimeException("The faction does not exist.");
        factions.get(factionName).setAbbreviation(abbreviation);
        NetworkManager.broadcastFactionAbbreviationUpdate(factionName, abbreviation, server);
        this.setDirty();
    }

    /**
     * Get the abbreviation for a faction
     *
     * @param factionName Name of the faction
     * @return The abbreviation, null if not set
     */
    public String getAbbreviation(String factionName) {
        if (!factions.containsKey(factionName)) return null;
        return factions.get(factionName).getAbbreviation();
    }


    /**
     * Return all alliance names
     */
    public Set<String> getAllFactionNames() {
        return factions.keySet();
    }

    public Map<UUID, String> getPlayerFactionMap() {
        return Collections.unmodifiableMap(playerFactionMap);
    }

    /**
     * Loads the data from NBT
     */
    public static FactionStateManager load(CompoundTag tag) {
        FactionStateManager stateManager = new FactionStateManager();

        if (tag.contains("Factions", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Factions", Tag.TAG_COMPOUND);

            Faction faction;
            for (int i = 0; i < list.size(); i++) {
                faction = Faction.deserialize(list.getCompound(i));

                // Populate Maps
                stateManager.factions.put(faction.getName(), faction);
                for (UUID member : faction.getMembers()) {
                    stateManager.playerFactionMap.put(member, faction.getName());
                }
            }
        }
        // All factions loaded. Populate incomingRelations for each faction
        for (Faction faction : stateManager.factions.values()) {
            for(Map.Entry<String, RelationshipStatus> entry: faction.getOutgoingRelations().entrySet()){
                stateManager.factions.get(entry.getKey()).getIncomingRelations().put(faction.getName(), entry.getValue());
            }
        }

        return stateManager;
    }

    /**
     * Saves the data to NBT
     */
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();

        for (Faction faction : factions.values()) {
            list.add(faction.serialize());
        }

        tag.put("Factions", list);
        return tag;
    }
}
