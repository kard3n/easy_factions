package com.jpreiss.easy_factions.network.packet.claims;


import com.jpreiss.easy_factions.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PacketChunkUnclaim {
    // Dim -> List<Chunk>
    private final Map<ResourceLocation, List<Long>> chunks;

    public PacketChunkUnclaim(Map<ResourceLocation, List<Long>> chunks) {
        this.chunks = chunks;
    }

    public Map<ResourceLocation, List<Long>> getChunks() {
        return chunks;
    }

    public static void encode(PacketChunkUnclaim msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.chunks.size());
        for (Map.Entry<ResourceLocation, List<Long>> entry : msg.chunks.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeInt(entry.getValue().size());
            for (Long chunk : entry.getValue()) {
                buf.writeLong(chunk);
            }
        }
    }

    public static PacketChunkUnclaim decode(FriendlyByteBuf buf) {
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

    public static void handle(PacketChunkUnclaim msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientPacketHandler.handleUnclaimChunk(msg);
        }));
        ctx.get().setPacketHandled(true);
    }
}
