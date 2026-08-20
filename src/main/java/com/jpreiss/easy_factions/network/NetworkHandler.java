package com.jpreiss.easy_factions.network;

import com.jpreiss.easy_factions.EasyFactions;
import com.jpreiss.easy_factions.network.packet.claims.PacketChunkClaim;
import com.jpreiss.easy_factions.network.packet.claims.PacketChunkUnclaim;
import com.jpreiss.easy_factions.network.packet.factions_alliances.*;
import com.jpreiss.easy_factions.network.packet.gui.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@EventBusSubscriber(modid = EasyFactions.MODID)
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1.5";

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(EasyFactions.MODID).versioned(PROTOCOL_VERSION).optional();

        // Client-bound packets
        registrar.playToClient(PacketSyncFactionAlliance.TYPE, PacketSyncFactionAlliance.STREAM_CODEC, PacketSyncFactionAlliance::handle);
        registrar.playToClient(PacketRemovePlayerData.TYPE, PacketRemovePlayerData.STREAM_CODEC, PacketRemovePlayerData::handle);
        registrar.playToClient(PacketFactionLeaveAlliance.TYPE, PacketFactionLeaveAlliance.STREAM_CODEC, PacketFactionLeaveAlliance::handle);
        registrar.playToClient(PacketUpdateFactionAbbreviation.TYPE, PacketUpdateFactionAbbreviation.STREAM_CODEC, PacketUpdateFactionAbbreviation::handle);
        registrar.playToClient(PacketUpdateAllianceAbbreviation.TYPE, PacketUpdateAllianceAbbreviation.STREAM_CODEC, PacketUpdateAllianceAbbreviation::handle);
        registrar.playToClient(PacketSetFactionRelations.TYPE, PacketSetFactionRelations.STREAM_CODEC, PacketSetFactionRelations::handle);
        registrar.playToClient(PacketSyncFactionGuiData.TYPE, PacketSyncFactionGuiData.STREAM_CODEC, PacketSyncFactionGuiData::handle);
        registrar.playToClient(PacketOpenErrorPopup.TYPE, PacketOpenErrorPopup.STREAM_CODEC, PacketOpenErrorPopup::handle);
        registrar.playToClient(PacketChunkClaim.TYPE, PacketChunkClaim.STREAM_CODEC, PacketChunkClaim::handle);
        registrar.playToClient(PacketChunkUnclaim.TYPE, PacketChunkUnclaim.STREAM_CODEC, PacketChunkUnclaim::handle);
        registrar.playToClient(PacketRemoveFactionData.TYPE, PacketRemoveFactionData.STREAM_CODEC, PacketRemoveFactionData::handle);

        // Server-bound packets
        registrar.playToServer(PacketOpenFactionGui.TYPE, PacketOpenFactionGui.STREAM_CODEC, PacketOpenFactionGui::handle);
        registrar.playToServer(PacketFactionMemberOperation.TYPE, PacketFactionMemberOperation.STREAM_CODEC, PacketFactionMemberOperation::handle);
        registrar.playToServer(PacketFactionJoinAction.TYPE, PacketFactionJoinAction.STREAM_CODEC, PacketFactionJoinAction::handle);
        registrar.playToServer(PacketFactionCreateAction.TYPE, PacketFactionCreateAction.STREAM_CODEC, PacketFactionCreateAction::handle);
        registrar.playToServer(PacketFactionSetRelationAction.TYPE, PacketFactionSetRelationAction.STREAM_CODEC, PacketFactionSetRelationAction::handle);
        registrar.playToServer(PacketFactionLeaveAction.TYPE, PacketFactionLeaveAction.STREAM_CODEC, PacketFactionLeaveAction::handle);
        registrar.playToServer(PacketAllianceOperation.TYPE, PacketAllianceOperation.STREAM_CODEC, PacketAllianceOperation::handle);
        registrar.playToServer(PacketAllianceLeaveAction.TYPE, PacketAllianceLeaveAction.STREAM_CODEC, PacketAllianceLeaveAction::handle);
        registrar.playToServer(PacketAllianceSetRelationAction.TYPE, PacketAllianceSetRelationAction.STREAM_CODEC, PacketAllianceSetRelationAction::handle);
        registrar.playToServer(PacketFactionFriendlyFireToggle.TYPE, PacketFactionFriendlyFireToggle.STREAM_CODEC, PacketFactionFriendlyFireToggle::handle);
        registrar.playToServer(PacketSetAbbreviation.TYPE, PacketSetAbbreviation.STREAM_CODEC, PacketSetAbbreviation::handle);
        registrar.playToServer(PacketSetColor.TYPE, PacketSetColor.STREAM_CODEC, PacketSetColor::handle);

    }

    /**
     * Safely sends a packet to a player.
     * IMPORTANT: Prevents crashing when sending to vanilla clients who don't have the mod.
     */
    public static void sendToPlayer(CustomPacketPayload message, ServerPlayer player) {
        try {
            PacketDistributor.sendToPlayer(player, message);
        } catch (Exception e) {
            // Player may not have the channel
        }
    }

    /**
     * Sends the update to all players
     */
    public static void sendToAllPresent(CustomPacketPayload message, MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendToPlayer(message, player);
        }
    }
}