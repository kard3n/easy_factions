package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.client.ClientPacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketOpenErrorPopup(String errorMessage) implements CustomPacketPayload {
    public static final Type<PacketOpenErrorPopup> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_open_error_popup"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketOpenErrorPopup> STREAM_CODEC =
        StreamCodec.ofMember(PacketOpenErrorPopup::write, PacketOpenErrorPopup::read);

    private static PacketOpenErrorPopup read(RegistryFriendlyByteBuf buf) {
        return new PacketOpenErrorPopup(buf.readUtf());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(errorMessage);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketOpenErrorPopup msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientPacketHandler.handleOpenErrorPopup(msg);
        });
    }

    public String getErrorMessage() {
        return errorMessage();
    }
}
