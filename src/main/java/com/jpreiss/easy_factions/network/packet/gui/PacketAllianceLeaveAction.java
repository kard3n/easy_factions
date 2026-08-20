package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketAllianceLeaveAction() implements CustomPacketPayload {

    public static final Type<PacketAllianceLeaveAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_alliance_leave_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketAllianceLeaveAction> STREAM_CODEC =
        StreamCodec.ofMember(PacketAllianceLeaveAction::write, PacketAllianceLeaveAction::read);

    private static PacketAllianceLeaveAction read(RegistryFriendlyByteBuf buf) {
        return new PacketAllianceLeaveAction();
    }

    private void write(RegistryFriendlyByteBuf buf) {
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketAllianceLeaveAction msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            AllianceStateManager manager = AllianceStateManager.get(server);

            try {
                manager.leaveAlliance(player, player.getServer());
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
    }
}
