package com.jpreiss.easy_factions.network.packet;

import com.jpreiss.easy_factions.client.ClientAllianceData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketUpdateAllianceAbbreviation {
    private final String allianceName;
    private final String abbreviation;


    public PacketUpdateAllianceAbbreviation(String allianceName, String abbreviation) {
        this.allianceName = allianceName;
        this.abbreviation = abbreviation;
    }

    public static void encode(PacketUpdateAllianceAbbreviation msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.allianceName);
        buf.writeUtf(msg.abbreviation);
    }

    public static PacketUpdateAllianceAbbreviation decode(FriendlyByteBuf buf) {
        return new PacketUpdateAllianceAbbreviation(buf.readUtf(), buf.readUtf());
    }

    public static void handle(PacketUpdateAllianceAbbreviation msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientAllianceData.update(msg.allianceName, msg.abbreviation);
        }));
        ctx.get().setPacketHandled(true);
    }
}
