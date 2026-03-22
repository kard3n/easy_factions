package com.jpreiss.easy_factions.network.packet.claims;


import com.jpreiss.easy_factions.client.ClientPacketHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PacketChunkClaim {
    // Dim -> (Chunk, Color)
    private final Map<ResourceLocation, HashMap<Long, Integer>> chunks;

    public PacketChunkClaim(Map<ResourceLocation, HashMap<Long, Integer>> map) {
        this.chunks = map;
    }

    public Map<ResourceLocation, HashMap<Long, Integer>> getChunks() {
        return chunks;
    }

    public static void encode(PacketChunkClaim msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.chunks.size());
        for (Map.Entry<ResourceLocation, HashMap<Long, Integer>> dimEntry : msg.chunks.entrySet()) {
            buf.writeResourceLocation(dimEntry.getKey());
            buf.writeInt(dimEntry.getValue().size());
            for (Map.Entry<Long, Integer> entry : dimEntry.getValue().entrySet()) {
                buf.writeLong(entry.getKey());
                buf.writeInt(entry.getValue());
            }
        }
    }

    public static PacketChunkClaim decode(FriendlyByteBuf buf) {
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

    public static void handle(PacketChunkClaim msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            LogUtils.getLogger().atWarn().log("Handling claim chunk packet.");
            ClientPacketHandler.handleClaimChunk(msg);
        }));
        ctx.get().setPacketHandled(true);
    }
}
