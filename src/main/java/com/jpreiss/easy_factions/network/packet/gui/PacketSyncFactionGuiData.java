package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.client.gui.FactionScreen;
import com.jpreiss.easy_factions.client.gui.NoFactionScreen;
import com.jpreiss.easy_factions.common.MemberRank;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class PacketSyncFactionGuiData {
    private final boolean inFaction;
    private final String factionName;
    private final Map<UUID, MemberRank> memberRanks; // UUID -> MemberRank
    private final Map<UUID, String> playerNames; // UUID -> Name for all currently online server players, those invited and those who are members
    private final List<UUID> factionInvites; // UUIDs of all players invited to the player's faction
    private final List<String> playerInvites; // Names of the factions the player is invited to

    // Constructor for when player IS in a faction
    public PacketSyncFactionGuiData(String factionName, Map<UUID, MemberRank> memberRanks, Map<UUID, String> playerNames, List<UUID> factionInvites) {
        this.inFaction = true;
        this.factionName = factionName;
        this.memberRanks = memberRanks;
        this.playerNames = playerNames;
        this.factionInvites = factionInvites;
        this.playerInvites = new ArrayList<>();
    }

    // Constructor for when player is NOT in a faction
    public PacketSyncFactionGuiData(List<String> playerInvites) {
        this.inFaction = false;
        this.factionName = "";
        this.memberRanks = new HashMap<>();
        this.playerNames = new HashMap<>();
        this.factionInvites = new ArrayList<>();
        this.playerInvites = playerInvites;
    }

    // Internal constructor for decoding
    public PacketSyncFactionGuiData(boolean inFaction, String factionName, Map<UUID, MemberRank> memberRanks, Map<UUID, String> playerNames, List<String> playerInvites, List<UUID> factionInvites) {
        this.inFaction = inFaction;
        this.factionName = factionName;
        this.memberRanks = memberRanks;
        this.playerNames = playerNames;
        this.playerInvites = playerInvites;
        this.factionInvites = factionInvites;

    }

    public static void encode(PacketSyncFactionGuiData msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.inFaction);
        buf.writeUtf(msg.factionName);
        buf.writeMap(msg.memberRanks, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeEnum);
        buf.writeMap(msg.playerNames, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeUtf);
        buf.writeCollection(msg.playerInvites, FriendlyByteBuf::writeUtf);
        buf.writeCollection(msg.factionInvites, FriendlyByteBuf::writeUUID);
    }

    public static PacketSyncFactionGuiData decode(FriendlyByteBuf buf) {
        return new PacketSyncFactionGuiData(
                buf.readBoolean(),
                buf.readUtf(),
                buf.readMap(FriendlyByteBuf::readUUID, b -> b.readEnum(MemberRank.class)),
                buf.readMap(FriendlyByteBuf::readUUID, FriendlyByteBuf::readUtf),
                buf.readList(FriendlyByteBuf::readUtf),
                buf.readList(FriendlyByteBuf::readUUID)
        );
    }

    public static void handle(PacketSyncFactionGuiData msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (msg.inFaction) {
                Minecraft.getInstance().setScreen(new FactionScreen(msg.factionName, msg.memberRanks, msg.playerNames, msg.factionInvites));
            } else {
                Minecraft.getInstance().setScreen(new NoFactionScreen(msg.playerInvites));
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}