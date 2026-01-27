package com.jpreiss.easy_factions.network;

import com.jpreiss.easy_factions.EasyFactions;
import com.jpreiss.easy_factions.network.packet.data_sync.*;
import com.jpreiss.easy_factions.network.packet.gui.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1.4";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EasyFactions.MODID, "main"),
            () -> PROTOCOL_VERSION,
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
            NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, PacketSyncFactionAlliance.class, PacketSyncFactionAlliance::encode, PacketSyncFactionAlliance::decode, PacketSyncFactionAlliance::handle);
        CHANNEL.registerMessage(id++, PacketRemovePlayerData.class, PacketRemovePlayerData::encode, PacketRemovePlayerData::decode, PacketRemovePlayerData::handle);
        CHANNEL.registerMessage(id++, PacketFactionLeaveAlliance.class, PacketFactionLeaveAlliance::encode, PacketFactionLeaveAlliance::decode, PacketFactionLeaveAlliance::handle);
        CHANNEL.registerMessage(id++, PacketUpdateFactionAbbreviation.class, PacketUpdateFactionAbbreviation::encode, PacketUpdateFactionAbbreviation::decode, PacketUpdateFactionAbbreviation::handle);
        CHANNEL.registerMessage(id++, PacketUpdateAllianceAbbreviation.class, PacketUpdateAllianceAbbreviation::encode, PacketUpdateAllianceAbbreviation::decode, PacketUpdateAllianceAbbreviation::handle);
        CHANNEL.registerMessage(id++, PacketSetFactionRelations.class, PacketSetFactionRelations::encode, PacketSetFactionRelations::decode, PacketSetFactionRelations::handle);
        CHANNEL.registerMessage(id++, PacketOpenFactionGui.class, PacketOpenFactionGui::encode, PacketOpenFactionGui::decode, PacketOpenFactionGui::handle);
        CHANNEL.registerMessage(id++, PacketSyncFactionGuiData.class, PacketSyncFactionGuiData::encode, PacketSyncFactionGuiData::decode, PacketSyncFactionGuiData::handle);
        CHANNEL.registerMessage(id++, PacketFactionMemberOperation.class, PacketFactionMemberOperation::encode, PacketFactionMemberOperation::decode, PacketFactionMemberOperation::handle);
        CHANNEL.registerMessage(id++, PacketFactionJoinAction.class, PacketFactionJoinAction::encode, PacketFactionJoinAction::decode, PacketFactionJoinAction::handle);
        CHANNEL.registerMessage(id++, PacketFactionCreateAction.class, PacketFactionCreateAction::encode, PacketFactionCreateAction::decode, PacketFactionCreateAction::handle);
        CHANNEL.registerMessage(id++, PacketFactionSetRelationAction.class, PacketFactionSetRelationAction::encode, PacketFactionSetRelationAction::decode, PacketFactionSetRelationAction::handle);
        CHANNEL.registerMessage(id++, PacketFactionLeaveAction.class, PacketFactionLeaveAction::encode, PacketFactionLeaveAction::decode, PacketFactionLeaveAction::handle);
        CHANNEL.registerMessage(id++, PacketAllianceOperation.class, PacketAllianceOperation::encode, PacketAllianceOperation::decode, PacketAllianceOperation::handle);
        CHANNEL.registerMessage(id++, PacketAllianceLeaveAction.class, PacketAllianceLeaveAction::encode, PacketAllianceLeaveAction::decode, PacketAllianceLeaveAction::handle);
        CHANNEL.registerMessage(id++, PacketAllianceSetRelationAction.class, PacketAllianceSetRelationAction::encode, PacketAllianceSetRelationAction::decode, PacketAllianceSetRelationAction::handle);
        CHANNEL.registerMessage(id++, PacketFactionFriendlyFireToggle.class, PacketFactionFriendlyFireToggle::encode, PacketFactionFriendlyFireToggle::decode, PacketFactionFriendlyFireToggle::handle);
        CHANNEL.registerMessage(id++, PacketOpenErrorPopup.class, PacketOpenErrorPopup::encode, PacketOpenErrorPopup::decode, PacketOpenErrorPopup::handle);
        CHANNEL.registerMessage(id++, PacketSetAbbreviation.class, PacketSetAbbreviation::encode, PacketSetAbbreviation::decode, PacketSetAbbreviation::handle);
    }

    /**
     * Safely sends a packet to a player.
     * IMPORTANT: Prevents crashing when sending to vanilla clients who don't have the mod.
     */
    public static void sendToPlayer(Object message, ServerPlayer player) {
        if (CHANNEL.isRemotePresent(player.connection.connection)) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }

    /**
     * Sends the update to all players
     */
    public static void sendToAllPresent(Object message, MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToPlayer(message, player);
        }
    }
}