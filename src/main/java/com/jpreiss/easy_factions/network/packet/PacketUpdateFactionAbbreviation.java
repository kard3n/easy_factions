package com.jpreiss.easy_factions.network.packet;

import com.jpreiss.easy_factions.client.ClientFactionData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketUpdateFactionAbbreviation {
    private final String factionName;
    private final String abbreviation;


    public PacketUpdateFactionAbbreviation(String factionName, String abbreviation) {
        this.factionName = factionName;
        this.abbreviation = abbreviation;
    }

    public static void encode(PacketUpdateFactionAbbreviation msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.factionName);
        buf.writeUtf(msg.abbreviation);
    }

    public static PacketUpdateFactionAbbreviation decode(FriendlyByteBuf buf) {
        return new PacketUpdateFactionAbbreviation(buf.readUtf(), buf.readUtf());
    }

    public static void handle(PacketUpdateFactionAbbreviation msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientFactionData.update(msg.factionName, msg.abbreviation);
        }));
        ctx.get().setPacketHandled(true);
    }
}
