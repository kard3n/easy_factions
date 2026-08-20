package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public record PacketFactionMemberOperation(Action action, UUID playerUUID) implements CustomPacketPayload {
    public enum Action { KICK, INVITE, REVOKE_INVITE, PROMOTE, DEMOTE }

    public static final Type<PacketFactionMemberOperation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_faction_member_operation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketFactionMemberOperation> STREAM_CODEC =
        StreamCodec.ofMember(PacketFactionMemberOperation::write, PacketFactionMemberOperation::read);

    private static PacketFactionMemberOperation read(RegistryFriendlyByteBuf buf) {
        return new PacketFactionMemberOperation(buf.readEnum(Action.class), buf.readUUID());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUUID(playerUUID);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketFactionMemberOperation msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            FactionStateManager manager = FactionStateManager.get(server);

            try {
                switch (msg.action()) {
                    case KICK -> manager.kickFromFaction(player, msg.playerUUID(), player.getServer());
                    case INVITE -> manager.invitePlayer(player, msg.playerUUID());
                    case PROMOTE -> manager.addOfficer(msg.playerUUID(), player, player.getServer());
                    case DEMOTE -> manager.removeOfficer(msg.playerUUID(), player, player.getServer());
                    case REVOKE_INVITE -> manager.revokeInvitation(player, msg.playerUUID());
                }
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                // Cannot easily construct open error popup since it's not converted here, but we can assume it will be
                // NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
    }
}
