package com.jpreiss.easy_factions.network.packet.data_sync;

import com.jpreiss.easy_factions.client.data_store.ClientAllianceData;
import com.jpreiss.easy_factions.client.data_store.ClientFactionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketRemovePlayerData {
    private final UUID playerUUID;

    public PacketRemovePlayerData(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public static void encode(PacketRemovePlayerData msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerUUID);
    }

    public static PacketRemovePlayerData decode(FriendlyByteBuf buf) {
        return new PacketRemovePlayerData(buf.readUUID());
    }

    public static void handle(PacketRemovePlayerData msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            String playerFaction = ClientFactionData.removePlayer(msg.playerUUID);
            if (playerFaction != null && ClientFactionData.getFactionMemberCount(playerFaction) < 1) {
                ClientAllianceData.removeFactionInformation(playerFaction);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
