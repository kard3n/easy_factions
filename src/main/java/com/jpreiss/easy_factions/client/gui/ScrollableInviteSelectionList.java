package com.jpreiss.easy_factions.client.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.network.packet.gui.PacketFactionJoinAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ScrollableInviteSelectionList extends ObjectSelectionList<ScrollableInviteSelectionList.InviteEntry> {

    public ScrollableInviteSelectionList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
        this.centerListVertically = false;

        // Transparent background
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
    }

    public void addInvite(String factionName) {
        this.addEntry(new InviteEntry(factionName));
    }

    @Override
    public int getRowWidth() {
        return 220; // Slightly narrower than the full window
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getLeft() + this.getRowWidth() + 6;
    }

    public static class InviteEntry extends ObjectSelectionList.Entry<InviteEntry> {
        private final String factionName;
        private final Button joinButton;

        public InviteEntry(String factionName) {
            this.factionName = factionName;

            this.joinButton = Button.builder(Component.literal("Join"), (btn) -> {
                NetworkHandler.CHANNEL.sendToServer(new PacketFactionJoinAction(factionName));
                Minecraft.getInstance().setScreen(null); // Close screen on join
            }).bounds(0, 0, 60, 20).build();
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();

            // Draw Faction Name
            guiGraphics.drawString(mc.font, this.factionName, left + 5, top + 6, 0xFFFFFF);

            // Position and Render Join Button
            this.joinButton.setX(left + width - 65); // Align to right side of row
            this.joinButton.setY(top + 2);
            this.joinButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.joinButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal("Invite from " + this.factionName);
        }
    }
}