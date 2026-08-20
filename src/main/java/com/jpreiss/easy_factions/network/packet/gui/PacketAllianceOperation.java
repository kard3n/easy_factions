package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketAllianceOperation(Action action, String factionName) implements CustomPacketPayload {
    public enum Action {INVITE, REVOKE_INVITE, CREATE}

    public static final Type<PacketAllianceOperation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_alliance_operation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketAllianceOperation> STREAM_CODEC =
        StreamCodec.ofMember(PacketAllianceOperation::write, PacketAllianceOperation::read);

    private static PacketAllianceOperation read(RegistryFriendlyByteBuf buf) {
        return new PacketAllianceOperation(buf.readEnum(Action.class), buf.readUtf());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUtf(factionName);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketAllianceOperation msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            AllianceStateManager manager = AllianceStateManager.get(server);
            FactionStateManager factionStateManager = FactionStateManager.get(server);
            if(!factionStateManager.playerOwnsFaction(player.getUUID())) return;

            try {
                switch (msg.action()) {
                    case INVITE -> manager.inviteFaction(player, msg.factionName(), server);
                    case REVOKE_INVITE -> manager.revokeInvitation(player, msg.factionName(), server);
                    case CREATE -> manager.createAlliance(msg.factionName(), player, server);
                }
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
    }
}
