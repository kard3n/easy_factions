package com.jpreiss.easy_factions.network.packet.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.server.ServerConfig;
import com.jpreiss.easy_factions.server.alliance.Alliance;
import com.jpreiss.easy_factions.server.alliance.AllianceStateManager;
import com.jpreiss.easy_factions.server.faction.Faction;
import com.jpreiss.easy_factions.server.faction.FactionStateManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketSetAbbreviation(String abbreviation, boolean isAlliance) implements CustomPacketPayload {
    public static final Type<PacketSetAbbreviation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("easy_factions", "packet_set_abbreviation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSetAbbreviation> STREAM_CODEC =
        StreamCodec.ofMember(PacketSetAbbreviation::write, PacketSetAbbreviation::read);

    private static PacketSetAbbreviation read(RegistryFriendlyByteBuf buf) {
        return new PacketSetAbbreviation(buf.readUtf(), buf.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(abbreviation);
        buf.writeBoolean(isAlliance);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(PacketSetAbbreviation msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            MinecraftServer server = player.getServer();
            if (server == null) return;

            FactionStateManager factionManager = FactionStateManager.get(server);
            AllianceStateManager allianceManager = AllianceStateManager.get(server);

            try {
                factionManager.playerOwnsFaction(player.getUUID());
                if (!msg.isAlliance()) {
                    Faction faction = factionManager.getFactionByPlayer(player.getUUID());
                    boolean factionAllowAbbreviationChange = ServerConfig.ENABLE_ABBREVIATION.get() && (ServerConfig.ALLOW_ABBREVIATION_CHANGE.get() || faction.getAbbreviation() == null);
                    if (!factionAllowAbbreviationChange) {
                        throw new RuntimeException("FORBIDDEN");
                    }

                    factionManager.setAbbreviation(faction.getName(), msg.abbreviation(), player, server);
                } else {
                    Faction playerFaction = factionManager.getFactionByPlayer(player.getUUID());
                    Alliance alliance = allianceManager.getAllianceByFaction(playerFaction.getName());

                    if (alliance == null) {
                        throw new RuntimeException("You are not in an alliance.");
                    }
                    boolean allianceAllowAbbreviationChange = ServerConfig.ENABLE_ABBREVIATION.get() && (ServerConfig.ALLOW_ABBREVIATION_CHANGE.get() || alliance.getAbbreviation() == null);
                    if (!allianceAllowAbbreviationChange) {
                        throw new RuntimeException("FORBIDDEN");
                    }
                    allianceManager.setAbbreviation(alliance.getName(), msg.abbreviation(), player, server);
                }
                // If successful, re-open/refresh the GUI
                PacketOpenFactionGui.handle(new PacketOpenFactionGui(), ctx);
            } catch (Exception e) {
                NetworkHandler.sendToPlayer(new PacketOpenErrorPopup(e.getMessage()), player);
            }
        });
    }
}
