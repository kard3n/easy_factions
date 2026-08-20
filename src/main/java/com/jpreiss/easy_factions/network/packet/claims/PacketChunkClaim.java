package com.jpreiss.easy_factions.network.packet.claims;

import com.jpreiss.easy_factions.client.ClientPacketHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record PacketChunkClaim(Map<ResourceLocation, HashMap<Long, Integer>> chunks) implements CustomPacketPayload {
    public static final Type<PacketChunkClaim> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_chunk_claim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketChunkClaim> STREAM_CODEC =
        StreamCodec.ofMember(PacketChunkClaim::write, PacketChunkClaim::read);

    private static PacketChunkClaim read(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        HashMap<ResourceLocation, HashMap<Long, Integer>> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation dim = buf.readResourceLocation();
            int dimSize = buf.readInt();
            HashMap<Long, Integer> dimMap = new HashMap<>();
            for (int j = 0; j < dimSize; j++) {
                dimMap.put(buf.readLong(), buf.readInt());
            }
            map.put(dim, dimMap);
        }
        return new PacketChunkClaim(map);
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(chunks.size());
        for (Map.Entry<ResourceLocation, HashMap<Long, Integer>> dimEntry : chunks.entrySet()) {
            buf.writeResourceLocation(dimEntry.getKey());
            buf.writeInt(dimEntry.getValue().size());
            for (Map.Entry<Long, Integer> entry : dimEntry.getValue().entrySet()) {
                buf.writeLong(entry.getKey());
                buf.writeInt(entry.getValue());
            }
        }
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketChunkClaim msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            LogUtils.getLogger().atWarn().log("Handling claim chunk packet.");
            ClientPacketHandler.handleClaimChunk(msg);
        });
    }
}
