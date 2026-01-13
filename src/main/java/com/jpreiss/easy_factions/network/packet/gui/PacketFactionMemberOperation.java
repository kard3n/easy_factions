package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Client -> Server packet for member operations
 */
public class PacketFactionMemberOperation {
    public enum Action { KICK, INVITE, REVOKE_INVITE, PROMOTE, DEMOTE }

    private final Action action;
    private final UUID playerUUID; // The player that is affected (NOT necessarily the one who executed this)

    public PacketFactionMemberOperation(Action action, UUID playerUUID) {
        this.action = action;
        this.playerUUID = playerUUID;
    }

    public static void encode(PacketFactionMemberOperation msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeUUID(msg.playerUUID);
    }

    public static PacketFactionMemberOperation decode(FriendlyByteBuf buf) {
        return new PacketFactionMemberOperation(buf.readEnum(Action.class), buf.readUUID());
    }

    public static void handle(PacketFactionMemberOperation msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            FactionStateManager manager = FactionStateManager.get(server);

            try {
                switch (msg.action) {
                    case KICK -> manager.kickFromFaction(player, msg.playerUUID, player.getServer());
                    case INVITE -> manager.invitePlayer(player, msg.playerUUID);
                    case PROMOTE -> manager.addOfficer(msg.playerUUID, player, player.getServer());
                    case DEMOTE ->  manager.removeOfficer(msg.playerUUID, player, player.getServer());
                    case REVOKE_INVITE -> manager.revokeInvitation(player, msg.playerUUID);
                }
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                // Ideally send an error message packet back to display in GUI
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
