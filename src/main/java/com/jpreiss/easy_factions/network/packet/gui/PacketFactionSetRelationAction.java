package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server packet for setting the relation with another faction
 */
public class PacketFactionSetRelationAction {

    private final String factionName;
    private final RelationshipStatus relationshipStatus;

    public PacketFactionSetRelationAction(String factionName, RelationshipStatus relationshipStatus) {
        this.factionName = factionName;
        this.relationshipStatus = relationshipStatus;
    }

    public static void encode(PacketFactionSetRelationAction msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.factionName);
        buf.writeEnum(msg.relationshipStatus);
    }

    public static PacketFactionSetRelationAction decode(FriendlyByteBuf buf) {
        return new PacketFactionSetRelationAction(buf.readUtf(), buf.readEnum(RelationshipStatus.class));
    }

    public static void handle(PacketFactionSetRelationAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            FactionStateManager manager = FactionStateManager.get(player.getServer());

            try {
                manager.setRelation( msg.factionName, player, msg.relationshipStatus);
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
