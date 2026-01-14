package com.jpreiss.easy_factions.client;

import com.jpreiss.easy_factions.EasyFactions;
import com.jpreiss.easy_factions.client.data_store.ClientAllianceData;
import com.jpreiss.easy_factions.client.data_store.ClientFactionData;
import com.jpreiss.easy_factions.client.data_store.ClientRelationshipData;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.network.packet.gui.PacketOpenFactionGui;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import static com.jpreiss.easy_factions.client.Keybinds.OPEN_FACTION_GUI;

@Mod.EventBusSubscriber(modid = EasyFactions.MODID, value = Dist.CLIENT)
public class ClientEventHandler {
    private static final ChatFormatting alliedAllianceColor = ChatFormatting.DARK_PURPLE;
    private static final ChatFormatting alliedFactionColor = ChatFormatting.LIGHT_PURPLE;
    private static final ChatFormatting friendlyAllianceColor = ChatFormatting.DARK_GREEN;
    private static final ChatFormatting friendlyFactionColor = ChatFormatting.GREEN;
    private static final ChatFormatting neutralAllianceColor = ChatFormatting.BLUE;
    private static final ChatFormatting neutralFactionColor = ChatFormatting.DARK_AQUA;
    private static final ChatFormatting hostileAllianceColor = ChatFormatting.DARK_RED;
    private static final ChatFormatting hostileFactionColor = ChatFormatting.RED;


    /**
     * Adds alliance and faction info above the nametag
     */
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.isInvisible()) return;

        String factionName = ClientFactionData.getFaction(player.getUUID());
        if (factionName == null) return;

        String factionAbbreviation = ClientConfig.getShowFactionAbbreviation() ? ClientFactionData.getAbbreviation(factionName) : null;

        String allianceName = ClientAllianceData.getAlliance(factionName);
        String allianceAbbreviation = ClientConfig.getShowAllianceAbbreviation() ? ClientAllianceData.getAbbreviation(allianceName) : null;

        Player viewer = Minecraft.getInstance().player;
        String viewerFaction = null;
        String viewerAlliance = null;
        if (viewer != null) {
            viewerFaction = ClientFactionData.getFaction(viewer.getUUID());
            if (viewerFaction != null) {
                viewerAlliance = ClientAllianceData.getAlliance(viewerFaction);
            }
        }

        ChatFormatting allianceColor = determineColor(factionName, allianceName, viewerFaction, viewerAlliance, true);
        ChatFormatting factionColor = determineColor(factionName, allianceName, viewerFaction, viewerAlliance, false);

        MutableComponent displayText = Component.empty();

        if (allianceName != null) {
            displayText.append(Component.literal("[").withStyle(ChatFormatting.WHITE));

            displayText.append(Component.literal(allianceAbbreviation != null ? allianceAbbreviation : allianceName).withStyle(allianceColor));

            displayText.append(Component.literal("] ").withStyle(ChatFormatting.WHITE));
        }

        displayText.append(Component.literal("<").withStyle(ChatFormatting.WHITE));

        displayText.append(Component.literal(factionAbbreviation != null ? factionAbbreviation : factionName).withStyle(factionColor));

        displayText.append(Component.literal(">").withStyle(ChatFormatting.WHITE));

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // Position at the name tag height
        float heightOffset = player.getBbHeight() + 0.5F;
        if (player.isCrouching()) {
            heightOffset -= 0.25F;
            poseStack.translate(0.0D, 0.25D, 0.0D);
        }
        poseStack.translate(0.0D, heightOffset, 0.0D);

        // Rotate to face camera
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        // Scale
        float scale = 0.025F;
        poseStack.scale(-scale, -scale, scale);

        // Move above the vanilla name tag
        poseStack.translate(0.0D, -10.0D, 0.0D);

        // Render
        Matrix4f matrix4f = poseStack.last().pose();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        float xOffset = -font.width(displayText) / 2.0f;

        float backgroundOpacity = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int backgroundColor = (int) (backgroundOpacity * 255.0F) << 24;

        boolean isDiscrete = player.isDiscrete();
        int textColor = isDiscrete ? 0x20FFFFFF : 0xFFFFFFFF;
        Font.DisplayMode displayMode = isDiscrete ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL;

        font.drawInBatch(
                displayText,
                xOffset,
                0,
                textColor,
                false,
                matrix4f,
                event.getMultiBufferSource(),
                displayMode,
                backgroundColor,
                event.getPackedLight()
        );

        poseStack.popPose();
    }

    private static ChatFormatting determineColor(String renderedPlayerFaction, String renderedPlayerAlliance, String viewerFaction, String viewerAlliance, boolean isAllianceTag) {
        if (isAllianceTag) {
            if (viewerAlliance != null && viewerAlliance.equals(renderedPlayerAlliance)) {
                return alliedAllianceColor;
            }
        } else {
            if (viewerFaction != null && viewerFaction.equals(renderedPlayerFaction)) {
                return alliedFactionColor;
            }
            if (viewerAlliance != null && viewerAlliance.equals(renderedPlayerAlliance)) {
                // Same alliance, different faction
                return friendlyFactionColor;
            }
        }

        // The server sends flattened faction-to-faction relationships, even if derived from alliances.
        RelationshipStatus status = ClientRelationshipData.getRelationship(renderedPlayerFaction);

        if (status == RelationshipStatus.HOSTILE) {
            return isAllianceTag ? hostileAllianceColor : hostileFactionColor;
        }
        if (status == RelationshipStatus.FRIENDLY) {
            return isAllianceTag ? friendlyAllianceColor : friendlyFactionColor;
        }

        return isAllianceTag ? neutralAllianceColor : neutralFactionColor;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && OPEN_FACTION_GUI.consumeClick()) {
            // Instead of opening GUI directly, ask server for data
            NetworkHandler.CHANNEL.sendToServer(new PacketOpenFactionGui());
        }
    }
}