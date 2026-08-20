package com.jpreiss.easy_factions.client.gui;

import com.jpreiss.easy_factions.network.packet.gui.PacketFactionCreateAction;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class NoFactionScreen extends Screen {

    private final List<String> invites;
    private EditBox nameField;
    private ScrollableInviteSelectionList inviteList; // The new list
    private boolean isCreating = false;

    private int leftPos;
    private int topPos;
    private final int imageWidth = 260;
    private final int imageHeight = 200; // Made slightly taller to fit list better

    // Styling Colors
    private final int COL_BG = 0xF0101010;
    private final int COL_BORDER = 0xFF505050;
    private final int COL_LIST_BG = 0xFF000000;

    public NoFactionScreen(List<String> invites) {
        super(Component.literal("Faction Menu"));
        this.invites = invites;
    }

    @Override
    protected void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;

        int centerX = this.leftPos + (this.imageWidth / 2);
        int centerY = this.topPos + (this.imageHeight / 2);

        if (isCreating) {
            // Create Faction View
            this.nameField = new EditBox(this.font, centerX - 80, centerY - 20, 160, 20, Component.literal("Name"));
            this.addRenderableWidget(nameField);

            this.addRenderableWidget(new Button.Builder(Component.literal("Confirm Create"), button -> {
                String name = nameField.getValue();
                if (!name.isEmpty()) {
                    PacketDistributor.sendToServer(new PacketFactionCreateAction(name));
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
            }).bounds(centerX - 80, this.topPos + 35, 160, 20).build());

            // Invite list
            // Area: Starts below the "Pending Invites" text, ends before bottom of window
            int listTop = this.topPos + 80;
            int listBottom = this.topPos + this.imageHeight - 10;

            this.inviteList = new ScrollableInviteSelectionList(this.minecraft, this.imageWidth - 20, listBottom - listTop, listTop, 30);
            this.inviteList.setX(this.leftPos + 10); // Center horizontally

            for (String invite : invites) {
                this.inviteList.addInvite(invite);
            }

            this.addRenderableWidget(this.inviteList);
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Overrides the renderBackground method to prevent the blur
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Called here so that the game is still blurred
        // GUI Elements are not blurred due to the renderBackground override above
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        // Window Frame and Background
        guiGraphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, COL_BORDER);
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, COL_BG);

        // Title
        guiGraphics.drawCenteredString(this.font, this.title, this.leftPos + (this.imageWidth / 2), this.topPos + 10, 0xFFFFFF);

        // Separator Line
        guiGraphics.hLine(leftPos + 10, leftPos + imageWidth - 10, topPos + 25, 0xFF555555);

        if (isCreating) {
            guiGraphics.drawString(this.font, "Enter Faction Name:", this.leftPos + (this.imageWidth / 2) - 80, this.topPos + (this.imageHeight / 2) - 35, 0xAAAAAA, false);
        } else {
            if (!invites.isEmpty()) {
                guiGraphics.drawCenteredString(this.font, "Pending Invites:", this.leftPos + (this.imageWidth / 2), this.topPos + 65, 0xAAAAAA);

                // Draw background box for the list
                if (this.inviteList != null) {
                    guiGraphics.fill(leftPos + 10, inviteList.getY(), leftPos + imageWidth - 10, inviteList.getY() + inviteList.getHeight(), COL_LIST_BG);
                    guiGraphics.renderOutline(leftPos + 10, inviteList.getY(), imageWidth - 20, inviteList.getHeight(), 0xFF444444);
                }
            } else {
                guiGraphics.drawCenteredString(this.font, "No Pending Invites", this.leftPos + (this.imageWidth / 2), this.topPos + 80, 0x555555);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}