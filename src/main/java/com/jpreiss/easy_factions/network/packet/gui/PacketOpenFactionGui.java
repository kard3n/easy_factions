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
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.*;

public record PacketOpenFactionGui() implements CustomPacketPayload {
    public static final Type<PacketOpenFactionGui> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_open_faction_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketOpenFactionGui> STREAM_CODEC =
        StreamCodec.ofMember(PacketOpenFactionGui::write, PacketOpenFactionGui::read);

    private static PacketOpenFactionGui read(RegistryFriendlyByteBuf buf) {
        return new PacketOpenFactionGui();
    }

    private void write(RegistryFriendlyByteBuf buf) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketOpenFactionGui msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
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
                    if (faction.getOwner().equals(uuid)) {
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
                Map<String, RelationshipStatus> incomingRelationships = faction.getIncomingRelations();
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

                boolean factionAllowAbbreviationChange = ServerConfig.ENABLE_ABBREVIATION.get() && (ServerConfig.ALLOW_ABBREVIATION_CHANGE.get() || faction.getAbbreviation() == null);
                boolean allianceAllowAbbreviationChange = alliance != null && ServerConfig.ENABLE_ABBREVIATION.get() && (ServerConfig.ALLOW_ABBREVIATION_CHANGE.get() || alliance.getAbbreviation() == null);

                response = new PacketSyncFactionGuiData(
                        faction.getName(),
                        memberRanks,
                        playerNames,
                        invitedUsers,
                        outgoingRelationships,
                        incomingRelationships,
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
                        ServerConfig.FACTION_ABBREVIATION_MAX_LENGTH.get(),
                        ServerConfig.ALLIANCE_ABBREVIATION_MAX_LENGTH.get(),
                        factionAllowAbbreviationChange,
                        allianceAllowAbbreviationChange
                );
            } else {
                // Player is NOT in a faction -> Return pending invites
                response = new PacketSyncFactionGuiData(factionManager.getInvitesForPlayer(player.getUUID()));
            }

            PacketDistributor.sendToPlayer(player, response);
        });
    }
}