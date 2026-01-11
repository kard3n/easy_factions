package com.jpreiss.easy_factions.network.packet.data_sync;

import com.jpreiss.easy_factions.client.data_store.ClientAllianceData;
import com.jpreiss.easy_factions.client.data_store.ClientFactionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketRemoveFactionData {
    private final String factionName;

    public PacketRemoveFactionData(String factionName) {
        this.factionName = factionName;
    }

    public static void encode(PacketRemoveFactionData msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.factionName);
    }

    public static PacketRemoveFactionData decode(FriendlyByteBuf buf) {
        return new PacketRemoveFactionData(buf.readUtf());
    }

    public static void handle(PacketRemoveFactionData msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientFactionData.removeFaction(msg.factionName);
            ClientAllianceData.removeFactionInformation(msg.factionName);
        }));
        ctx.get().setPacketHandled(true);
    }
}
