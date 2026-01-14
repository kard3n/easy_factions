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

public class ScrollableAllianceMemberList extends ObjectSelectionList<ScrollableAllianceMemberList.MemberEntry> {

    public ScrollableAllianceMemberList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
        this.centerListVertically = false;

        // Disable background behind items
        this.setRenderBackground(false);

        // Disable dirt overlay
        this.setRenderTopAndBottom(false);
    }

    public void addMember(String name, boolean isClientsFactions) {
        this.addEntry(new MemberEntry(name, isClientsFactions));
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
        private final String shownFactionName;
        private final boolean isClientsFactions;


        /**
         * @param shownFactionName Name of the player shown in the list
         */
        public MemberEntry(String shownFactionName, boolean isClientsFactions) {
            this.shownFactionName = shownFactionName;
            this.isClientsFactions = isClientsFactions;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();

            // name
            guiGraphics.drawString(mc.font, this.shownFactionName, left + 4, top + 8, 0xFFFFFF);

            // If it is the current one
            if (this.isClientsFactions) {
                guiGraphics.drawString(mc.font, "(You)", left + 105, top + 8, Color.GREEN.getRGB());
            }

        }

        /**
         * Forward mouse clicks to the buttons
         */
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(this.shownFactionName);
        }
    }
}
