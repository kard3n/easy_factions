package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server packet for setting the relation with another faction
 */
public class PacketAllianceSetRelationAction {

    private final String allianceName;
    private final RelationshipStatus relationshipStatus;

    public PacketAllianceSetRelationAction(String allianceName, RelationshipStatus relationshipStatus) {
        this.allianceName = allianceName;
        this.relationshipStatus = relationshipStatus;
    }

    public static void encode(PacketAllianceSetRelationAction msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.allianceName);
        buf.writeEnum(msg.relationshipStatus);
    }

    public static PacketAllianceSetRelationAction decode(FriendlyByteBuf buf) {
        return new PacketAllianceSetRelationAction(buf.readUtf(), buf.readEnum(RelationshipStatus.class));
    }

    public static void handle(PacketAllianceSetRelationAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            AllianceStateManager allianceManager = AllianceStateManager.get(server);
            FactionStateManager factionStateManager = FactionStateManager.get(server);

            try {
                if (!factionStateManager.playerOwnsFaction(player.getUUID())){
                    throw new RuntimeException("Player is not owner of a faction");
                }
                allianceManager.setRelation( msg.allianceName, player, msg.relationshipStatus);
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                // Ideally send an error message packet back to display in GUI
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
