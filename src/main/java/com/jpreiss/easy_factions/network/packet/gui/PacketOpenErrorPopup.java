package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketOpenErrorPopup {
    private final String errorMessage;

    public PacketOpenErrorPopup(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public static void encode(PacketOpenErrorPopup msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.errorMessage);
    }

    public static PacketOpenErrorPopup decode(FriendlyByteBuf buf) {
        return new PacketOpenErrorPopup(buf.readUtf());
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static void handle(PacketOpenErrorPopup msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientPacketHandler.handleOpenErrorPopup(msg);
        }));
        ctx.get().setPacketHandled(true);
    }
}
