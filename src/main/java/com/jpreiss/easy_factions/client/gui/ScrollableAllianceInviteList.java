package com.jpreiss.easy_factions.client.gui;

import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.network.packet.gui.PacketAllianceOperation;
import com.jpreiss.easy_factions.network.packet.gui.PacketFactionMemberOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ScrollableAllianceInviteList extends ObjectSelectionList<ScrollableAllianceInviteList.InviteEntry> {

    public ScrollableAllianceInviteList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
        this.centerListVertically = false;

        // Disable background behind items
        this.setRenderBackground(false);

        // Disable dirt overlay
        this.setRenderTopAndBottom(false);
    }

    public void addInvite(String name, boolean isInvited, boolean localPlayerCanInvite) {
        this.addEntry(new InviteEntry(name, isInvited, localPlayerCanInvite));
    }

    @Override
    public int getRowWidth() {
        return 320;
    }

    @Override
    protected int getScrollbarPosition() {
        // Position the scrollbar to the right of the row
        return this.width / 2 + this.getRowWidth() / 2 + 4;
    }

    // Entry class
    public static class InviteEntry extends Entry<InviteEntry> {
        private final String shownFactionName;
        private final Button buttonInvite;



        /**
         * @param shownFactionName Name of the player shown in the list
         * @param isInvited If the player is invited
         */
        public InviteEntry(String shownFactionName,  boolean isInvited, boolean localPlayerCanInvite) {
            this.shownFactionName = shownFactionName;

            if (isInvited){
                this.buttonInvite = Button.builder(Component.literal("Revoke Invitation"), (btn) -> {
                    NetworkHandler.CHANNEL.sendToServer(new PacketAllianceOperation(PacketAllianceOperation.Action.REVOKE_INVITE, this.shownFactionName));
                    // TODO: update list
                }).bounds(0, 0, 70, 20).build();
            }
            else {
                this.buttonInvite = Button.builder(Component.literal("Invite"), (btn) -> {
                    NetworkHandler.CHANNEL.sendToServer(new PacketAllianceOperation(PacketAllianceOperation.Action.INVITE, this.shownFactionName));
                    // TODO: update list
                }).bounds(0, 0, 70, 20).build();
            }

        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();

            // name
            guiGraphics.drawString(mc.font, this.shownFactionName, left + 5, top + 6, 0xFFFFFF);


            this.buttonInvite.setX(left + 120);
            this.buttonInvite.setY(top + 2);

            this.buttonInvite.render(guiGraphics, mouseX, mouseY, partialTick);

        }

        /**
         * Forward mouse clicks to the buttons
         */
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.buttonInvite.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(this.shownFactionName);
        }
    }
}
