package com.jpreiss.easy_factions.client.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.network.packet.gui.PacketFactionGuiAction;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NoFactionScreen extends Screen {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/demo_background.png");

    private final List<String> invites;
    private EditBox nameField;
    private boolean isCreating = false;

    private int leftPos;
    private int topPos;
    private final int imageWidth = 248;
    private final int imageHeight = 166;

    public NoFactionScreen(List<String> invites) {
        super(Component.literal("Faction Menu"));
        this.invites = invites;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        // Window Center (Relative to screen)
        int centerX = this.leftPos + (this.imageWidth / 2);
        int centerY = this.topPos + (this.imageHeight / 2);

        if (isCreating) {
            // Create Faction View
            this.nameField = new EditBox(this.font, centerX - 80, centerY - 20, 160, 20, Component.literal("Name"));
            this.addRenderableWidget(nameField);

            this.addRenderableWidget(new Button.Builder(Component.literal("Confirm Create"), button -> {
                String name = nameField.getValue();
                if (!name.isEmpty()) {
                    NetworkHandler.CHANNEL.sendToServer(new PacketFactionGuiAction(PacketFactionGuiAction.Action.CREATE, name));
                    this.onClose();
                }
            }).bounds(centerX - 82, centerY + 10, 80, 20).build());

            this.addRenderableWidget(new Button.Builder(Component.literal("Back"), button -> {
                this.isCreating = false;
                this.rebuildWidgets();
            }).bounds(centerX + 2, centerY + 10, 80, 20).build());

        } else {
            // Landing View
            this.addRenderableWidget(new Button.Builder(Component.literal("Create New Faction"), button -> {
                this.isCreating = true;
                this.rebuildWidgets();
            }).bounds(centerX - 80, this.topPos + 30, 160, 20).build());

            // Invites List
            int y = this.topPos + 80;
            for (String invite : invites) {
                if (y > this.topPos + this.imageHeight - 20) break; // Boundary check

                this.addRenderableWidget(new Button.Builder(Component.literal("Join " + invite), button -> {
                    NetworkHandler.CHANNEL.sendToServer(new PacketFactionGuiAction(PacketFactionGuiAction.Action.JOIN, invite));
                    this.onClose();
                }).bounds(centerX - 80, y, 160, 20).build());
                y += 25;
            }
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics); // Darken world

        // Draw Window
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Draw Title
        guiGraphics.drawCenteredString(this.font, this.title, this.leftPos + (this.imageWidth / 2), this.topPos + 10, 0x404040);

        if (isCreating) {
            guiGraphics.drawString(this.font, "Enter Faction Name:", this.leftPos + (this.imageWidth / 2) - 80, this.topPos + (this.imageHeight / 2) - 35, 0x404040, false);
        } else {
            // Draw "Invites" Header only if there are invites or to indicate the section
            if (!invites.isEmpty()) {
                guiGraphics.drawCenteredString(this.font, "Pending Invites:", this.leftPos + (this.imageWidth / 2), this.topPos + 65, 0x404040);
            } else {
                guiGraphics.drawCenteredString(this.font, "No Pending Invites", this.leftPos + (this.imageWidth / 2), this.topPos + 65, 0x808080);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}