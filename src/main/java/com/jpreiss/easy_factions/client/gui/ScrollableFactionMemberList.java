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

public class ScrollableFactionMemberList extends ObjectSelectionList<ScrollableFactionMemberList.MemberEntry> {

    public ScrollableFactionMemberList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
        this.centerListVertically = false;

        // Disable background behind items
        this.setRenderBackground(false);

        // Disable dirt overlay
        this.setRenderTopAndBottom(false);
    }

    public void addMember(String name, UUID uuid, MemberRank rank, MemberRank localPlayerRank, UUID localPlayerUUID) {
        this.addEntry(new MemberEntry(name, uuid, rank, localPlayerRank, localPlayerUUID));
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
    public static class MemberEntry extends ObjectSelectionList.Entry<MemberEntry> {
        private final String shownPlayerName;
        private final UUID shownPlayerUuid;
        private final MemberRank shownPlayerRank;
        private final MemberRank clientPlayerRank; // Rank of the person seeing this
        private final UUID clientPlayerUUID;
        private final Button buttonKick;
        private final Button buttonChangeRank;

        /**
         * @param shownPlayerName Name of the player shown in the list
         * @param shownPlayerUuid UUID of the player shown in the list
         * @param shownPlayerRank Rank of the player shown in the list
         * @param clientPlayerRank Rank of the player seeing this
         * @param clientPlayerUUID UUID of the player seeing this
         */
        public MemberEntry(String shownPlayerName, UUID shownPlayerUuid, MemberRank shownPlayerRank, MemberRank clientPlayerRank, UUID clientPlayerUUID) {
            this.shownPlayerName = shownPlayerName;
            this.shownPlayerRank = shownPlayerRank;
            this.shownPlayerUuid = shownPlayerUuid;
            this.clientPlayerRank = clientPlayerRank;
            this.clientPlayerUUID = clientPlayerUUID;

            this.buttonKick = Button.builder(Component.literal("Kick"), (btn) -> {
                NetworkHandler.CHANNEL.sendToServer(new PacketFactionMemberOperation(PacketFactionMemberOperation.Action.KICK, this.shownPlayerUuid));
                // TODO: update list
            }).bounds(0, 0, 70, 20).build();
            this.buttonKick.active = (shownPlayerRank == MemberRank.MEMBER) && (clientPlayerRank != MemberRank.MEMBER);

            if (shownPlayerRank == MemberRank.MEMBER) {
                this.buttonChangeRank = Button.builder(Component.literal("Promote"), (btn) -> {
                    NetworkHandler.CHANNEL.sendToServer(new PacketFactionMemberOperation(PacketFactionMemberOperation.Action.PROMOTE, this.shownPlayerUuid));
                    // TODO: update list
                }).bounds(0, 0, 70, 20).build();
                this.buttonChangeRank.active = clientPlayerRank == MemberRank.OWNER;
            } else if (shownPlayerRank == MemberRank.OFFICER) {
                this.buttonChangeRank = Button.builder(Component.literal("Demote"), (btn) -> {
                            NetworkHandler.CHANNEL.sendToServer(new PacketFactionMemberOperation(PacketFactionMemberOperation.Action.DEMOTE, this.shownPlayerUuid));
                        })
                        .bounds(0, 0, 70, 20).build();
                this.buttonChangeRank.active = clientPlayerRank == MemberRank.OWNER;
            }
            else{
                this.buttonChangeRank = Button.builder(Component.literal("OWNER"), (btn) -> {
                        })
                        .bounds(0, 0, 70, 20).build();
                this.buttonChangeRank.active = false;
            }

        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();

            // name
            guiGraphics.drawString(mc.font, this.shownPlayerName, left + 4, top + 8, 0xFFFFFF);

            // rank
            Color statusColor;
            switch (this.shownPlayerRank) {
                case OWNER -> statusColor = Color.MAGENTA;
                case OFFICER -> statusColor = Color.GREEN;
                case MEMBER -> statusColor = Color.WHITE;
                default -> statusColor = Color.GRAY;
            }
            guiGraphics.drawString(mc.font, this.shownPlayerRank.toString(), left + 80, top + 8, statusColor.getRGB());

            this.buttonKick.setX(left + 120);
            this.buttonKick.setY(top + 2);

            this.buttonChangeRank.setX(left + 205);
            this.buttonChangeRank.setY(top + 2);

            // Render buttons
            this.buttonKick.render(guiGraphics, mouseX, mouseY, partialTick);
            this.buttonChangeRank.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        /**
         * Forward mouse clicks to the buttons
         */
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.buttonKick.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (this.buttonChangeRank.mouseClicked(mouseX, mouseY, button)) {
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
