package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Client -> Server packet fort GUI button clicks
 */
public class PacketFactionGuiAction {
    public enum Action { CREATE, JOIN, LEAVE, KICK, INVITE }

    private final Action action;
    private final String argument; // Can be faction name or player Name

    public PacketFactionGuiAction(Action action, String argument) {
        this.action = action;
        this.argument = argument;
    }

    public static void encode(PacketFactionGuiAction msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeUtf(msg.argument);
    }

    public static PacketFactionGuiAction decode(FriendlyByteBuf buf) {
        return new PacketFactionGuiAction(buf.readEnum(Action.class), buf.readUtf());
    }

    public static void handle(PacketFactionGuiAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            FactionStateManager manager = FactionStateManager.get(player.getServer());

            try {
                switch (msg.action) {
                    case CREATE -> manager.createFaction(msg.argument, null, player, player.getServer());
                    case JOIN -> manager.joinFaction(player, msg.argument, player.getServer());
                    case LEAVE -> manager.leaveFaction(player, player.getServer());
                    case KICK -> manager.kickFromFaction(player, msg.argument, player.getServer());
                    // For invite, we need to find the player entity by name
                    case INVITE -> {
                        ServerPlayer target = player.getServer().getPlayerList().getPlayerByName(msg.argument);
                        if (target != null) manager.invitePlayer(player, target);
                    }
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
