package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.Utils;
import com.jpreiss.easy_factions.common.MemberRank;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.ServerConfig;
import com.jpreiss.easy_factions.server.alliance.Alliance;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.function.Supplier;

/**
 * Packet sent when the player opens the faction GUI
 * Server replies with GUI data
 */
public class PacketOpenFactionGui {
    public PacketOpenFactionGui() {
    }

    public static void encode(PacketOpenFactionGui msg, FriendlyByteBuf buf) {
    }

    public static PacketOpenFactionGui decode(FriendlyByteBuf buf) {
        return new PacketOpenFactionGui();
    }

    /**
     * Handled on server side
     *
     * @param msg
     * @param ctx
     */
    public static void handle(PacketOpenFactionGui msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;

            FactionStateManager factionManager = FactionStateManager.get(server);
            AllianceStateManager allianceManager = AllianceStateManager.get(server);
            Faction faction = factionManager.getFactionByPlayer(player.getUUID());

            PacketSyncFactionGuiData response;
            if (faction != null) { // Player is in a faction -> Return faction information
                Map<UUID, MemberRank> memberRanks = new HashMap<>();
                Map<UUID, String> playerNames = new HashMap<>();
                for (UUID uuid : faction.getMembers()) {
                    String name = Utils.getPlayerNameOffline(uuid, server);
                    if (faction.getOwner() == uuid) {
                        memberRanks.put(uuid, MemberRank.OWNER);
                    } else if (faction.getOfficers().contains(uuid)) {
                        memberRanks.put(uuid, MemberRank.OFFICER);
                    } else {
                        memberRanks.put(uuid, MemberRank.MEMBER);
                    }
                    playerNames.put(uuid, name);
                }

                // Add all online players to name list
                for (ServerPlayer serverPlayer : server.getPlayerList().getPlayers()) {
                    playerNames.put(serverPlayer.getUUID(), serverPlayer.getGameProfile().getName());
                }

                List<UUID> invitedUsers = new ArrayList<>();
                for (UUID playerName : faction.getInvited()) {
                    String name = Utils.getPlayerNameOffline(playerName, server);
                    invitedUsers.add(playerName);
                    playerNames.put(playerName, name);
                }

                Map<String, RelationshipStatus> outgoingRelationships = faction.getOutgoingRelations();
                List<String> factionNames = factionManager.getAllFactionNames().stream().toList();

                Alliance alliance = allianceManager.getAllianceByFaction(faction.getName());
                List<String> allianceMembers = new ArrayList<>();
                List<String> allianceInvites = new ArrayList<>();
                Map<String, RelationshipStatus> outgoingAllianceRelations = new HashMap<>();
                Map<String, RelationshipStatus> incomingAllianceRelations = new HashMap<>();
                int allianceColor = 0xFFFFFF;
                if (alliance != null) {
                    allianceMembers.addAll(alliance.getMembers());
                    allianceInvites.addAll(alliance.getInvited());
                    outgoingAllianceRelations.putAll(alliance.getOutgoingRelations());
                    incomingAllianceRelations.putAll(alliance.getIncomingRelations());
                    allianceColor = alliance.getColor();
                }
                String allianceName = alliance != null ? alliance.getName() : null;

                List<String> allianceNames = allianceManager.getAllianceNames().stream().toList();

                boolean factionAllowAbbreviationChange = ServerConfig.enableAbbreviation && (ServerConfig.allowAbbreviationChange || faction.getAbbreviation() == null);
                boolean allianceAllowAbbreviationChange = alliance != null && ServerConfig.enableAbbreviation && (ServerConfig.allowAbbreviationChange || alliance.getAbbreviation() == null);

                response = new PacketSyncFactionGuiData(
                        faction.getName(),
                        memberRanks,
                        playerNames,
                        invitedUsers,
                        outgoingRelationships,
                        factionNames,
                        allianceName,
                        allianceMembers,
                        allianceInvites,
                        allianceNames,
                        outgoingAllianceRelations,
                        incomingAllianceRelations,
                        faction.getFriendlyFire(),
                        faction.getColor(),
                        allianceColor,
                        ServerConfig.factionAbbreviationMaxLength,
                        ServerConfig.allianceAbbreviationMaxLength,
                        factionAllowAbbreviationChange,
                        allianceAllowAbbreviationChange
                );
            } else {
                // Player is NOT in a faction -> Return pending invites
                response = new PacketSyncFactionGuiData(factionManager.getInvitesForPlayer(player.getUUID()));
            }

            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), response);
        });
        ctx.get().setPacketHandled(true);
    }
}