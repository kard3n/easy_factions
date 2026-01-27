package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server packet for member operations
 */
public class PacketAllianceOperation {
    public enum Action {INVITE, REVOKE_INVITE, CREATE}

    private final Action action;
    private final String factionName; // The faction affected by this

    public PacketAllianceOperation(Action action, String factionName) {
        this.action = action;
        this.factionName = factionName;
    }

    public static void encode(PacketAllianceOperation msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeUtf(msg.factionName);
    }

    public static PacketAllianceOperation decode(FriendlyByteBuf buf) {
        return new PacketAllianceOperation(buf.readEnum(Action.class), buf.readUtf());
    }

    public static void handle(PacketAllianceOperation msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            AllianceStateManager manager = AllianceStateManager.get(server);
            FactionStateManager factionStateManager = FactionStateManager.get(server);
            if(!factionStateManager.playerOwnsFaction(player.getUUID()))  return;

            try {
                switch (msg.action) {
                    case INVITE -> manager.inviteFaction(player, msg.factionName, server);
                    case REVOKE_INVITE -> manager.revokeInvitation(player, msg.factionName, server);
                    case CREATE -> manager.createAlliance(msg.factionName, player, server);
                }
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
