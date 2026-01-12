package com.jpreiss.easy_factions.client.gui;


import com.jpreiss.easy_factions.client.data_store.ClientFactionData;
import com.jpreiss.easy_factions.common.MemberRank;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.Logging;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FactionScreen extends Screen {
    private enum MainTab {
        FACTION, ALLIANCE, SETTINGS
    }

    private enum FactionTab {
        MEMBERS, INVITES, RELATIONS
    }

    // position and width of the window
    private int windowStartX;
    private int windowStartY;
    private final int imageWidth = 360;
    private final int imageHeight = 400;

    private final int headerButtonWidth = 80;
    private final int buttonHeight = 20;
    private final int spacing = 5;

    private final int backgroundColor = 0xFF404040;

    private ScrollableFactionMemberList memberList;
    private ScrollableFactionInviteList inviteList;
    private ScrollableFactionRelationsList relationsList;

    private static MainTab currentMainTab = MainTab.FACTION;
    private static FactionTab currentFactionTab = FactionTab.MEMBERS;

    // Data
    private final String factionName;
    private final Map<UUID, MemberRank> memberRanks; // UUID -> MemberRank
    private final Map<UUID, String> playerNames; // UUID -> Name
    private final UUID clientPlayerUUID;
    private final List<UUID> factionInvites;
    private final Map<String, RelationshipStatus> outgoingFactionRelations;
    private final List<String> factionNames;


    public FactionScreen(String factionName, Map<UUID, MemberRank> memberRanks, Map<UUID, String> playerNames, List<UUID> factionInvites, Map<String, RelationshipStatus> outgoingFactionRelations, List<String> factionNames) {
        super(Component.literal("Easy Factions"));
        this.factionName = factionName;
        this.memberRanks = memberRanks;
        this.playerNames = playerNames;
        assert Minecraft.getInstance().player != null;
        this.clientPlayerUUID = Minecraft.getInstance().player.getUUID();
        this.factionInvites = factionInvites;
        this.outgoingFactionRelations = outgoingFactionRelations;
        this.factionNames = factionNames;
    }

    @Override
    protected void init() {
        super.init();

        this.windowStartX = (this.width - this.imageWidth) / 2;
        this.windowStartY = (this.height - this.imageHeight) / 2;

        int headerStartY = this.windowStartY + spacing;

        int headerButtonStartX = (this.windowStartX + this.imageWidth / 2) - (this.headerButtonWidth * 3 - this.spacing * 2) / 2 - this.spacing; // Center buttons

        // Faction button
        Button mainBtn = Button.builder(Component.literal("Faction"), (btn) -> switchTab(MainTab.FACTION))
                .bounds(headerButtonStartX, headerStartY, headerButtonWidth, buttonHeight).build();
        mainBtn.active = (currentMainTab != MainTab.FACTION);
        this.addRenderableWidget(mainBtn);

        // Alliance button
        Button settingsBtn = Button.builder(Component.literal("Alliance"), (btn) -> switchTab(MainTab.ALLIANCE))
                .bounds(headerButtonStartX + headerButtonWidth + this.spacing, headerStartY, headerButtonWidth, buttonHeight).build();
        settingsBtn.active = (currentMainTab != MainTab.ALLIANCE);
        this.addRenderableWidget(settingsBtn);

        // Settings button
        Button infoBtn = Button.builder(Component.literal("Settings"), (btn) -> switchTab(MainTab.SETTINGS))
                .bounds(headerButtonStartX + (headerButtonWidth + this.spacing) * 2, headerStartY, headerButtonWidth, buttonHeight).build();
        infoBtn.active = (currentMainTab != MainTab.SETTINGS);
        this.addRenderableWidget(infoBtn);

        // Initialize Content based on the current tab
        switch (currentMainTab) {
            case FACTION -> initFactionTab();
            case ALLIANCE -> initSettingsTab();
            case SETTINGS -> initInfoTab();
        }
    }

    private void switchTab(MainTab mainTab) {
        if (currentMainTab != mainTab) {
            currentMainTab = mainTab;
            this.rebuildWidgets();
        }
    }

    private void switchFactionTab(FactionTab factionTab) {
        if (currentFactionTab != factionTab) {
            currentFactionTab = factionTab;
            this.rebuildWidgets();
        }
    }


    private void initFactionTab() {
        // Buttons

        int buttonStartX = (this.windowStartX + this.imageWidth / 2) - (this.headerButtonWidth * 3 - this.spacing * 2) / 2 - this.spacing; // Center buttons
        int headerStartY = this.windowStartY + spacing * 2 + buttonHeight;

        // Members button
        Button membersBtn = Button.builder(Component.literal("Members"), (btn) -> {
                    this.switchFactionTab(FactionTab.MEMBERS);
                })
                .bounds(buttonStartX, headerStartY, headerButtonWidth, buttonHeight).build();
        membersBtn.active = (currentFactionTab != FactionTab.MEMBERS);
        this.addRenderableWidget(membersBtn);

        // Invites button
        Button invitesBtn = Button.builder(Component.literal("Invites"), (btn) -> {
                    this.switchFactionTab(FactionTab.INVITES);
                })
                .bounds(buttonStartX + headerButtonWidth + this.spacing, headerStartY, headerButtonWidth, buttonHeight).build();
        invitesBtn.active = (currentFactionTab != FactionTab.INVITES);
        this.addRenderableWidget(invitesBtn);

        // Relations button
        Button relationsBtn = Button.builder(Component.literal("Relations"), (btn) -> {
                    this.switchFactionTab(FactionTab.RELATIONS);
                })
                .bounds(buttonStartX + (headerButtonWidth + this.spacing) * 2, headerStartY, headerButtonWidth, buttonHeight).build();
        relationsBtn.active = (currentFactionTab != FactionTab.RELATIONS);
        this.addRenderableWidget(relationsBtn);

        // Set where the tab object should start and end
        int topY = headerStartY + this.buttonHeight + spacing * 2;
        int bottomY = this.windowStartY + this.imageHeight - spacing;
        switch (currentFactionTab){
            case MEMBERS -> initFactionMembersTab(topY, bottomY);
            case INVITES -> initFactionInvitesTab(topY, bottomY);
            case RELATIONS -> initFactionRelationsTab(topY, bottomY);
        }


    }

    private void initFactionMembersTab(int topY, int bottomY){
        if (this.memberList == null) {
            this.memberList = new ScrollableFactionMemberList(this.minecraft, this.width, this.height, topY, bottomY, 30);

            for (Map.Entry<UUID, MemberRank> member : memberRanks.entrySet()) {
                this.memberList.addMember(playerNames.get(member.getKey()), member.getKey(), member.getValue(), memberRanks.get(clientPlayerUUID), clientPlayerUUID);
            }

        } else {
            // Resize in case it already exists and the scale has updated
            this.memberList.updateSize(this.width, this.height, topY, bottomY);
        }

        this.addRenderableWidget(this.memberList);
    }

    private void initFactionInvitesTab(int topY, int bottomY){
        if (this.inviteList == null) {
            this.inviteList = new ScrollableFactionInviteList(this.minecraft, this.width, this.height, topY, bottomY, 30);

            boolean localPlayerCanInvite = memberRanks.get(clientPlayerUUID) != MemberRank.MEMBER;

            for (UUID invitedUser: factionInvites) {
                this.inviteList.addInvite(playerNames.get(invitedUser), invitedUser, true, localPlayerCanInvite);
            }

            for (UUID playerUUID: playerNames.keySet()){
                if (!factionInvites.contains(playerUUID) && !memberRanks.containsKey(playerUUID)){
                    this.inviteList.addInvite(playerNames.get(playerUUID), playerUUID, false, localPlayerCanInvite);
                }
            }

        } else {
            // Resize in case it already exists and the scale has updated
            this.inviteList.updateSize(this.width, this.height, topY, bottomY);
        }

        this.addRenderableWidget(this.inviteList);
    }

    private void initFactionRelationsTab(int topY, int bottomY){
        if (this.relationsList == null) {
            this.relationsList = new ScrollableFactionRelationsList(this.minecraft, this.width, this.height, topY, bottomY, 30);

            boolean playerIsOwnerOrOfficer = memberRanks.get(clientPlayerUUID) != MemberRank.MEMBER;

            for (Map.Entry<String, RelationshipStatus> entry : outgoingFactionRelations.entrySet()) {
                this.relationsList.addFaction(entry.getKey(), entry.getValue(), playerIsOwnerOrOfficer);
            }

            for(String factionName: factionNames){
                if (!outgoingFactionRelations.containsKey(factionName) && !this.factionName.equals(factionName)){
                    this.relationsList.addFaction(factionName, RelationshipStatus.NEUTRAL, playerIsOwnerOrOfficer);
                }
            }

        } else {
            // Resize in case it already exists and the scale has updated
            this.relationsList.updateSize(this.width, this.height, topY, bottomY);
        }

        this.addRenderableWidget(this.relationsList);
    }


    private void initSettingsTab() {
        // Example: Add a toggle button for Settings
        this.addRenderableWidget(Button.builder(Component.literal("Toggle Feature"), (btn) -> {
            System.out.println("Toggled a setting");
        }).bounds(this.width / 2 - 50, 60, 100, 20).build());

        // Add another widget specific to settings
        this.addRenderableWidget(Button.builder(Component.literal("Reset"), (btn) -> {
            // Reset logic
        }).bounds(this.width / 2 - 50, 85, 100, 20).build());
    }

    private void initInfoTab() {
        // The info tab might just be text, so we might not add widgets here.
        // We will handle the text rendering in the render method.
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Draw the standard background (dark dirt texture)
        this.renderBackground(guiGraphics);

        // Render background
        guiGraphics.fill(
                this.windowStartX,
                this.windowStartY,
                this.windowStartX + this.imageWidth,
                this.windowStartY + this.imageHeight,
                this.backgroundColor
        );

        // 2. Draw the title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFFFFF);

        switch (currentMainTab) {
            case FACTION -> {
            }
            case ALLIANCE -> {
                guiGraphics.drawCenteredString(this.font, "Soon", this.width / 2, 120, 0xFF5555);
            }
            case SETTINGS -> {
                guiGraphics.drawCenteredString(this.font, "Soon", this.width / 2, 60, 0x55FF55);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}