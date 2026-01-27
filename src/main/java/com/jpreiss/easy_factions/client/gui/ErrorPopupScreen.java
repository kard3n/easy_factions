package com.jpreiss.easy_factions.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ErrorPopupScreen extends Screen {
    private final Screen parentScreen;
    private final String errorMessage;
    private int windowStartX, windowStartY;
    private final int imageWidth = 200;
    private final int imageHeight = 100;

    public ErrorPopupScreen(Screen parentScreen, String errorMessage) {
        super(Component.literal("Error"));
        this.parentScreen = parentScreen;
        this.errorMessage = errorMessage;
    }

    @Override
    protected void init() {
        this.windowStartX = (this.width - this.imageWidth) / 2;
        this.windowStartY = (this.height - this.imageHeight) / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Close"), (btn) -> this.onClose()).bounds(this.windowStartX + (this.imageWidth / 2) - 40, this.windowStartY + this.imageHeight - 30, 80, 20).build());
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render the parent screen first (behind everything)
        if (this.parentScreen != null) {
            // Pass -1, -1 for mouse coordinates
            // Prevents clicks and highlighting
            this.parentScreen.render(guiGraphics, -1, -1, partialTick);
        } else {
            this.renderBackground(guiGraphics);
        }

        // Push the PoseStack and move forward on the Z-axis
        // 500 should be above everything else
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500.0F);

        // Render the dimming background
        if (this.parentScreen != null) {
            guiGraphics.fill(0, 0, this.width, this.height, 0xA0000000);
        }

        // Draw popup window
        guiGraphics.fill(windowStartX, windowStartY, windowStartX + imageWidth, windowStartY + imageHeight, 0xFF202020);
        guiGraphics.renderOutline(windowStartX, windowStartY, imageWidth, imageHeight, 0xFFFFFFFF);


        guiGraphics.drawCenteredString(this.font, this.errorMessage, this.width / 2, this.windowStartY + 30, 0xFFFF5555);

        // Draw Close button
        // super.render inherits the Z=500
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Restore the PoseStack to avoid messing up subsequent render calls
        guiGraphics.pose().popPose();
    }

    @Override
    public void onClose() {
        if(this.minecraft == null) return;
        this.minecraft.setScreen(this.parentScreen);
    }
}
