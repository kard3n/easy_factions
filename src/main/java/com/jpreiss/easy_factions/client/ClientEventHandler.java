package com.jpreiss.easy_factions.client;

import com.jpreiss.easy_factions.EasyFactions;
import com.jpreiss.easy_factions.client.data_store.ClientAllianceData;
import com.jpreiss.easy_factions.client.data_store.ClientFactionData;
import com.jpreiss.easy_factions.client.data_store.ClientRelationshipData;
import com.jpreiss.easy_factions.common.RelationshipStatus;

import com.jpreiss.easy_factions.network.packet.gui.PacketOpenFactionGui;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;

import java.util.Objects;

import static com.jpreiss.easy_factions.client.Keybinds.OPEN_FACTION_GUI;

@EventBusSubscriber(modid = EasyFactions.MODID, value = Dist.CLIENT)
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
        if (event.canRender() == TriState.FALSE) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.isInvisible()) return;

        String factionName = ClientFactionData.getFaction(player.getUUID());
        if (factionName == null) return;

        String factionAbbreviation = ClientConfig.SHOW_FACTION_ABBREVIATION.get() ? ClientFactionData.getAbbreviation(factionName) : null;

        String allianceName = ClientAllianceData.getAlliance(factionName);
        String allianceAbbreviation = ClientConfig.SHOW_ALLIANCE_ABBREVIATION.get() ? ClientAllianceData.getAbbreviation(allianceName) : null;

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

        // Gert nametag pos through the attachment system
        Vec3 nameTagPos = player.getAttachments().getNullable(EntityAttachment.NAME_TAG, 0, player.getYRot());

        if (nameTagPos != null) {
            poseStack.translate(nameTagPos.x(), nameTagPos.y(), nameTagPos.z());
        } else {
            // Safe fallback just in case the attachment is missing
            poseStack.translate(0.0D, player.getBbHeight() + 0.5D, 0.0D);
        }

        // Rotate to face the camera
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        //Scale down text to vanilla size
        poseStack.scale(0.025F, -0.025F, 0.025F);

        // Move the text up to appear above the normal nametag
        poseStack.translate(0.0D, -30.0D, 0.0D);

        // Render
        Matrix4f matrix4f = poseStack.last().pose();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        float xOffset = -font.width(displayText) / 2.0f;

        float backgroundOpacity = mc.options.getBackgroundOpacity(0.25F);
        int backgroundColor = (int) (backgroundOpacity * 255.0F) << 24;

        boolean isDiscrete = player.isDiscrete();
        int textColor = isDiscrete ? 0x20FFFFFF : 0xFFFFFFFF;

        // Make nametags see-through while sneaking
        Font.DisplayMode displayMode = isDiscrete ? Font.DisplayMode.NORMAL : Font.DisplayMode.SEE_THROUGH;

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
    public static void onClientTick(ClientTickEvent.Post event) {
        if (OPEN_FACTION_GUI.consumeClick()) {
            // Instead of opening GUI directly, ask server for data
            PacketDistributor.sendToServer(new PacketOpenFactionGui());
        }
    }
}