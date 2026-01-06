package com.jpreiss.easy_factions.client;

import com.jpreiss.easy_factions.EasyFactions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting; // Import this for colors
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = EasyFactions.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    /**
     * Adds alliance and faction info above the nametag
     */
    @SubscribeEvent
    public static void onRenderNameTag(RenderNameTagEvent event) {
        if (event.getResult() == net.minecraftforge.eventbus.api.Event.Result.DENY) return;
        if (!(event.getEntity() instanceof Player player)) return;

        String factionName = ClientFactionData.getFaction(player.getUUID());
        if (factionName == null) return;

        String allianceName = ClientFactionData.getAlliance(factionName);


        MutableComponent displayText = Component.empty();

        if (allianceName != null) {
            displayText.append(Component.literal("[")
                    .withStyle(ChatFormatting.WHITE));

            displayText.append(Component.literal(allianceName)
                    .withStyle(ChatFormatting.LIGHT_PURPLE)); // Color for Alliance

            displayText.append(Component.literal("] ")
                    .withStyle(ChatFormatting.WHITE));
        }

        displayText.append(Component.literal("<")
                .withStyle(ChatFormatting.WHITE));

        displayText.append(Component.literal(factionName)
                .withStyle(ChatFormatting.AQUA)); // Color for Faction

        displayText.append(Component.literal(">")
                .withStyle(ChatFormatting.WHITE));


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
        int backgroundColor = (int)(backgroundOpacity * 255.0F) << 24;

        font.drawInBatch(
                displayText,
                xOffset,
                0,
                0xFFFFFFFF,
                false,
                matrix4f,
                event.getMultiBufferSource(),
                Font.DisplayMode.NORMAL,
                backgroundColor,
                0xF000F0
        );

        poseStack.popPose();
    }
}