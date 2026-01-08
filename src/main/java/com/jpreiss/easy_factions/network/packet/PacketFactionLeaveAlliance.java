package com.jpreiss.easy_factions.network.packet;

import com.jpreiss.easy_factions.client.ClientFactionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketFactionLeaveAlliance {
    private final String factionName;

    public PacketFactionLeaveAlliance(String factionName) {
        this.factionName = factionName;
    }

    public static void encode(PacketFactionLeaveAlliance msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.factionName);
    }

    public static PacketFactionLeaveAlliance decode(FriendlyByteBuf buf) {
        return new PacketFactionLeaveAlliance(buf.readUtf());
    }

    public static void handle(PacketFactionLeaveAlliance msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFactionData.leaveAlliance(msg.factionName)));
        ctx.get().setPacketHandled(true);
    }
}
