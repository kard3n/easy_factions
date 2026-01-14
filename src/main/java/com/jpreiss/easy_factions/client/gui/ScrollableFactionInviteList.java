package com.jpreiss.easy_factions.client.gui;

import com.jpreiss.easy_factions.common.MemberRank;
import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.network.packet.gui.PacketFactionMemberOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.UUID;

public class ScrollableFactionInviteList extends ObjectSelectionList<ScrollableFactionInviteList.MemberEntry> {

    public ScrollableFactionInviteList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
        this.centerListVertically = false;

        // Disable background behind items
        this.setRenderBackground(false);

        // Disable dirt overlay
        this.setRenderTopAndBottom(false);
    }

    public void addInvite(String name, UUID uuid, boolean isInvited, boolean localPlayerCanInvite) {
        this.addEntry(new MemberEntry(name, uuid, isInvited, localPlayerCanInvite));
    }

    @Override
    public int getRowWidth() {
        return 300;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getLeft() + this.getRowWidth() + 6;
    }

    // Entry class
    public static class MemberEntry extends Entry<MemberEntry> {
        private final String shownPlayerName;
        private final UUID shownPlayerUuid;
        private final Button buttonInvite;



        /**
         * @param shownPlayerName Name of the player shown in the list
         * @param shownPlayerUuid UUID of the player shown in the list
         * @param isInvited If the player is invited
         */
        public MemberEntry(String shownPlayerName, UUID shownPlayerUuid, boolean isInvited, boolean localPlayerCanInvite) {
            this.shownPlayerName = shownPlayerName;
            this.shownPlayerUuid = shownPlayerUuid;

            if (isInvited){
                this.buttonInvite = Button.builder(Component.literal("Revoke Invitation"), (btn) -> {
                    NetworkHandler.CHANNEL.sendToServer(new PacketFactionMemberOperation(PacketFactionMemberOperation.Action.REVOKE_INVITE, this.shownPlayerUuid));
                    // TODO: update list
                }).bounds(0, 0, 70, 20).build();
            }
            else {
                this.buttonInvite = Button.builder(Component.literal("Invite"), (btn) -> {
                    NetworkHandler.CHANNEL.sendToServer(new PacketFactionMemberOperation(PacketFactionMemberOperation.Action.INVITE, this.shownPlayerUuid));
                    // TODO: update list
                }).bounds(0, 0, 70, 20).build();
            }

        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();

            // name
            guiGraphics.drawString(mc.font, this.shownPlayerName, left + 4, top + 8, 0xFFFFFF);


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
            return Component.literal(this.shownPlayerName);
        }
    }
}
