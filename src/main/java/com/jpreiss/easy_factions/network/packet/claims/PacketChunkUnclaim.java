package com.jpreiss.easy_factions.network.packet.claims;

import com.jpreiss.easy_factions.client.ClientPacketHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record PacketChunkUnclaim(Map<ResourceLocation, List<Long>> chunks) implements CustomPacketPayload {
    public static final Type<PacketChunkUnclaim> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_chunk_unclaim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketChunkUnclaim> STREAM_CODEC =
        StreamCodec.ofMember(PacketChunkUnclaim::write, PacketChunkUnclaim::read);

    private static PacketChunkUnclaim read(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<ResourceLocation, List<Long>> chunks = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation dim = buf.readResourceLocation();
            int listSize = buf.readInt();
            List<Long> list = new ArrayList<>();
            for (int j = 0; j < listSize; j++) {
                list.add(buf.readLong());
            }
            chunks.put(dim, list);
        }
        return new PacketChunkUnclaim(chunks);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(chunks.size());
        for (Map.Entry<ResourceLocation, List<Long>> entry : chunks.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeInt(entry.getValue().size());
            for (Long chunk : entry.getValue()) {
                buf.writeLong(chunk);
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketChunkUnclaim msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ClientPacketHandler.handleUnclaimChunk(msg);
        });
    }
}
