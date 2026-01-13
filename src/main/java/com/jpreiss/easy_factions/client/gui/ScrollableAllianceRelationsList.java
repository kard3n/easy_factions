package com.jpreiss.easy_factions.client.gui;

import com.jpreiss.easy_factions.client.data_store.ClientRelationshipData;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.network.packet.gui.PacketAllianceSetRelationAction;
import com.jpreiss.easy_factions.network.packet.gui.PacketFactionSetRelationAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

import static com.jpreiss.easy_factions.common.RelationshipStatus.FRIENDLY;

public class ScrollableAllianceRelationsList extends ObjectSelectionList<ScrollableAllianceRelationsList.RelationEntry> {

    public ScrollableAllianceRelationsList(Minecraft minecraft, int width, int height, int top, int bottom, int itemHeight) {
        super(minecraft, width, height, top, bottom, itemHeight);
        this.centerListVertically = false;

        // Disable background behind items
        this.setRenderBackground(false);

        // Disable dirt overlay
        this.setRenderTopAndBottom(false);
    }

    public void addAlliance(String allianceName, RelationshipStatus outgoingStatus, boolean playerCanEdit) {
        this.addEntry(new RelationEntry(allianceName, outgoingStatus, playerCanEdit));
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
    public static class RelationEntry extends Entry<RelationEntry> {
        private final String allianceName;
        private final Button buttonFriendly;
        private final Button buttonNeutral;
        private final Button buttonHostile;

        /**
         * @param allianceName Name of the player shown in the list
         * @param outgoingAllianceStatus Status this alliance has set the other to
         * @param playerCanEdit If the player seeing this should be able to change the relationship
         */
        public RelationEntry(String allianceName, RelationshipStatus outgoingAllianceStatus, boolean playerCanEdit) {
            this.allianceName = allianceName;

            this.buttonFriendly = Button.builder(Component.literal("Friendly"), (btn) -> {
                NetworkHandler.CHANNEL.sendToServer(new PacketAllianceSetRelationAction(allianceName, FRIENDLY));
            }).bounds(0, 0, 60, 20).build();
            this.buttonFriendly.active = (outgoingAllianceStatus != FRIENDLY) && playerCanEdit;

            this.buttonNeutral = Button.builder(Component.literal("Neutral"), (btn) -> {
                NetworkHandler.CHANNEL.sendToServer(new PacketAllianceSetRelationAction(allianceName, RelationshipStatus.NEUTRAL));
            }).bounds(0, 0, 60, 20).build();
            this.buttonNeutral.active = (outgoingAllianceStatus != RelationshipStatus.NEUTRAL) && playerCanEdit;

            this.buttonHostile = Button.builder(Component.literal("Hostile"), (btn) -> {
                NetworkHandler.CHANNEL.sendToServer(new PacketAllianceSetRelationAction(allianceName, RelationshipStatus.HOSTILE));
            }).bounds(0, 0, 60, 20).build();
            this.buttonHostile.active = (outgoingAllianceStatus != RelationshipStatus.HOSTILE) && playerCanEdit;

        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick) {
            Minecraft mc = Minecraft.getInstance();

            // name
            guiGraphics.drawString(mc.font, this.allianceName, left + 5, top + 6, 0xFFFFFF);

            // relationship
            RelationshipStatus status = ClientRelationshipData.getRelationship(this.allianceName);
            Color statusColor;
            switch (status) {
                case FRIENDLY -> statusColor = Color.GREEN;
                case HOSTILE -> statusColor = Color.RED;
                default -> statusColor = Color.BLUE;
            }
            guiGraphics.drawString(mc.font, status.toString(), left + 80, top + 6, statusColor.getRGB());

            this.buttonFriendly.setX(left + 130);
            this.buttonFriendly.setY(top + 2);

            this.buttonNeutral.setX(left + 195);
            this.buttonNeutral.setY(top + 2);

            this.buttonHostile.setX(left + 260);
            this.buttonHostile.setY(top + 2);


            // Render buttons
            this.buttonFriendly.render(guiGraphics, mouseX, mouseY, partialTick);
            this.buttonNeutral.render(guiGraphics, mouseX, mouseY, partialTick);
            this.buttonHostile.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        /**
         * Forward mouse clicks to the buttons
         */
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (this.buttonFriendly.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (this.buttonNeutral.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (this.buttonHostile.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public @NotNull Component getNarration() {
            return Component.literal(this.allianceName);
        }
    }
}
