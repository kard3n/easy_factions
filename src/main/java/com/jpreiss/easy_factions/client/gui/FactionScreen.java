package com.jpreiss.easy_factions.client.gui;

import com.jpreiss.easy_factions.client.ClientConfig;
import com.jpreiss.easy_factions.common.MemberRank;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.network.packet.gui.PacketAllianceLeaveAction;
import com.jpreiss.easy_factions.network.packet.gui.PacketAllianceOperation;
import com.jpreiss.easy_factions.network.packet.gui.PacketFactionFriendlyFireToggle;
import com.jpreiss.easy_factions.network.packet.gui.PacketFactionLeaveAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class FactionScreen extends Screen {
    private enum MainTab {FACTION, ALLIANCE, SETTINGS}

    private enum FactionTab {MEMBERS, INVITES, RELATIONS, OPTIONS}

    private enum AllianceTab {MEMBERS, INVITES, RELATIONS, OPTIONS}

    private static MainTab currentMainTab = MainTab.FACTION;
    private static FactionTab currentFactionTab = FactionTab.MEMBERS;
    private static AllianceTab currentAllianceTab = AllianceTab.MEMBERS;

    // Dimensions
    private int windowStartX, windowStartY;
    private final int imageWidth = 340; // Slightly wider for better spacing
    private final int imageHeight = 260;

    // Layout Constants
    private final int headerHeight = 50; // Height of the top section
    private final int tabHeaderHeight = 30; // Height of the sub-tab section
    private final int buttonHeight = 20;
    private final int spacing = 5;

    // Helpers
    private int contentStartX;
    private int contentStartY;

    // Colors (ARGB)
    private static final int COL_WINDOW_BG = 0xF0404040; // Dark semi-transparent
    private static final int COL_HEADER_BG = 0xFF202020; // Slightly lighter header
    private static final int COL_BORDER = 0xFF505050; // Border color
    private static final int COL_LIST_BG = 0xFF323232; // Black background for lists

    // Lists
    private ScrollableFactionMemberList factionMemberList;
    private ScrollableFactionInviteList factionInviteList;
    private ScrollableFactionRelationsList factionRelationsList;
    private ScrollableAllianceMemberList allianceMemberList;
    private ScrollableAllianceInviteList allianceInviteList;
    private ScrollableAllianceRelationsList allianceRelationsList;

    // Data
    private final String factionName;
    private final Map<UUID, MemberRank> memberRanks;
    private final Map<UUID, String> playerNames;
    private final UUID clientPlayerUUID;
    private final List<UUID> factionInvites;
    private final Map<String, RelationshipStatus> outgoingFactionRelations;
    private final List<String> factionNames;
    private final String allianceName;
    private final List<String> allianceMembers;
    private final List<String> allianceInvites;
    private final List<String> allianceNames;
    private final Map<String, RelationshipStatus> outgoingAllianceRelations;
    private final Map<String, RelationshipStatus> incomingAllianceRelations;
    private final boolean friendlyFire;

    public FactionScreen(String factionName, Map<UUID, MemberRank> memberRanks, Map<UUID, String> playerNames, List<UUID> factionInvites, Map<String, RelationshipStatus> outgoingFactionRelations, List<String> factionNames, String allianceName, List<String> allianceMembers, List<String> allianceInvites, List<String> allianceNames, Map<String, RelationshipStatus> outgoingAllianceRelations, Map<String, RelationshipStatus> incomingAllianceRelations, boolean friendlyFire) {
        super(Component.literal("Easy Factions"));
        this.factionName = factionName;
        this.memberRanks = memberRanks;
        this.playerNames = playerNames;
        assert Minecraft.getInstance().player != null;
        this.clientPlayerUUID = Minecraft.getInstance().player.getUUID();
        this.factionInvites = factionInvites;
        this.outgoingFactionRelations = outgoingFactionRelations;
        this.factionNames = factionNames;
        this.allianceName = allianceName;
        this.allianceMembers = allianceMembers;
        this.allianceInvites = allianceInvites;
        this.allianceNames = allianceNames;
        this.outgoingAllianceRelations = outgoingAllianceRelations;
        this.incomingAllianceRelations = incomingAllianceRelations;
        this.friendlyFire = friendlyFire;
    }

    @Override
    protected void init() {
        super.init();
        this.windowStartX = (this.width - this.imageWidth) / 2;
        this.windowStartY = (this.height - this.imageHeight) / 2;
        this.contentStartX = this.windowStartX + spacing;
        this.contentStartY = this.windowStartY + headerHeight + spacing;

        int mainTabY = this.windowStartY + (headerHeight / 2) - (buttonHeight / 2);
        // Align Main Tabs to the Right of the header
        int headerButtonWidth = 80;
        int mainTabStartX = this.windowStartX + this.imageWidth - (headerButtonWidth * 3) - (spacing * 3);

        // Header buttons
        Button mainBtn = Button.builder(Component.literal("Faction"), (btn) -> switchTab(MainTab.FACTION))
                .bounds(mainTabStartX, mainTabY, headerButtonWidth, buttonHeight).build();
        mainBtn.active = (currentMainTab != MainTab.FACTION);
        this.addRenderableWidget(mainBtn);

        Button allianceBtn = Button.builder(Component.literal("Alliance"), (btn) -> switchTab(MainTab.ALLIANCE))
                .bounds(mainTabStartX + headerButtonWidth + spacing, mainTabY, headerButtonWidth, buttonHeight).build();
        allianceBtn.active = (currentMainTab != MainTab.ALLIANCE);
        this.addRenderableWidget(allianceBtn);

        Button settingsBtn = Button.builder(Component.literal("Settings"), (btn) -> switchTab(MainTab.SETTINGS))
                .bounds(mainTabStartX + (headerButtonWidth + spacing) * 2, mainTabY, headerButtonWidth, buttonHeight).build();
        settingsBtn.active = (currentMainTab != MainTab.SETTINGS);
        this.addRenderableWidget(settingsBtn);

        switch (currentMainTab) {
            case FACTION -> initFactionTab();
            case ALLIANCE -> initAllianceTab();
            case SETTINGS -> initSettingsTab();
        }
    }

    // Helper to calculate content area
    private int getContentTopY() {
        return this.windowStartY + headerHeight + spacing + tabHeaderHeight + spacing;
    }

    private int getContentBottomY() {
        return this.windowStartY + this.imageHeight - spacing;
    }

    private void initFactionTab() {
        int subButtonWidth = 70;
        // Center the sub-tabs relative to the window width
        int totalWidth = (subButtonWidth * 4) + (spacing * 3);
        int startX = this.windowStartX + (this.imageWidth - totalWidth) / 2;
        int startY = this.windowStartY + headerHeight + spacing;

        // Sub Tabs
        addRenderableWidget(createTabButton("Members", startX, startY, subButtonWidth, FactionTab.MEMBERS, currentFactionTab));
        addRenderableWidget(createTabButton("Invites", startX + subButtonWidth + spacing, startY, subButtonWidth, FactionTab.INVITES, currentFactionTab));
        addRenderableWidget(createTabButton("Relations", startX + (subButtonWidth + spacing) * 2, startY, subButtonWidth, FactionTab.RELATIONS, currentFactionTab));
        addRenderableWidget(createTabButton("Options", startX + (subButtonWidth + spacing) * 3, startY, subButtonWidth, FactionTab.OPTIONS, currentFactionTab));

        int topY = getContentTopY();
        int bottomY = getContentBottomY();

        switch (currentFactionTab) {
            case MEMBERS -> initFactionMembersTab(topY, bottomY);
            case INVITES -> initFactionInvitesTab(topY, bottomY);
            case RELATIONS -> initFactionRelationsTab(topY, bottomY);
            case OPTIONS -> initFactionOptionsTab(topY, bottomY);
        }
    }

    private Button createTabButton(String label, int x, int y, int width, Enum<?> targetTab, Enum<?> currentTab) {
        Button btn = Button.builder(Component.literal(label), (b) -> {
            if (targetTab instanceof FactionTab) switchFactionTab((FactionTab) targetTab);
            if (targetTab instanceof AllianceTab) switchAllianceTabs((AllianceTab) targetTab);
        }).bounds(x, y, width, buttonHeight).build();
        btn.active = (targetTab != currentTab);
        return btn;
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

    private void switchAllianceTabs(AllianceTab allianceTab) {
        if (currentAllianceTab != allianceTab) {
            currentAllianceTab = allianceTab;
            this.rebuildWidgets();
        }
    }

    private void initFactionMembersTab(int topY, int bottomY) {
        if (this.factionMemberList == null) {
            this.factionMemberList = new ScrollableFactionMemberList(this.minecraft, this.imageWidth - 20, this.height, topY, bottomY, 30);
            this.factionMemberList.setLeftPos(this.windowStartX + 10); // Center horizontally within window
            for (Map.Entry<UUID, MemberRank> member : memberRanks.entrySet()) {
                this.factionMemberList.addMember(playerNames.get(member.getKey()), member.getKey(), member.getValue(), memberRanks.get(clientPlayerUUID), clientPlayerUUID);
            }
        } else {
            this.factionMemberList.updateSize(this.imageWidth - 20, this.height, topY, bottomY);
            this.factionMemberList.setLeftPos(this.windowStartX + 10);
        }
        this.addRenderableWidget(this.factionMemberList);
    }

    private void initFactionInvitesTab(int topY, int bottomY) {
        if (this.factionInviteList == null) {
            this.factionInviteList = new ScrollableFactionInviteList(this.minecraft, this.imageWidth - 20, this.height, topY, bottomY, 30);
            this.factionInviteList.setLeftPos(this.windowStartX + 10);
            boolean localPlayerCanInvite = memberRanks.get(clientPlayerUUID) != MemberRank.MEMBER;
            for (UUID invitedUser : factionInvites) {
                this.factionInviteList.addInvite(playerNames.get(invitedUser), invitedUser, true, localPlayerCanInvite);
            }
            for (UUID playerUUID : playerNames.keySet()) {
                if (!factionInvites.contains(playerUUID) && !memberRanks.containsKey(playerUUID)) {
                    this.factionInviteList.addInvite(playerNames.get(playerUUID), playerUUID, false, localPlayerCanInvite);
                }
            }
        } else {
            this.factionInviteList.updateSize(this.imageWidth - 20, this.height, topY, bottomY);
            this.factionInviteList.setLeftPos(this.windowStartX + 10);
        }

        this.addRenderableWidget(this.factionInviteList);
    }

    private void initFactionRelationsTab(int topY, int bottomY) {
        if (this.factionRelationsList == null) {
            this.factionRelationsList = new ScrollableFactionRelationsList(this.minecraft, this.imageWidth - 20, this.height, topY, bottomY, 30);
            this.factionRelationsList.setLeftPos(this.windowStartX + 10);
            boolean playerIsOwnerOrOfficer = memberRanks.get(clientPlayerUUID) != MemberRank.MEMBER;
            for (Map.Entry<String, RelationshipStatus> entry : outgoingFactionRelations.entrySet()) {
                this.factionRelationsList.addFaction(entry.getKey(), entry.getValue(), playerIsOwnerOrOfficer);
            }
            for (String fName : factionNames) {
                if (!outgoingFactionRelations.containsKey(fName) && !this.factionName.equals(fName)) {
                    this.factionRelationsList.addFaction(fName, RelationshipStatus.NEUTRAL, playerIsOwnerOrOfficer);
                }
            }
        } else {
            this.factionRelationsList.updateSize(this.imageWidth - 20, this.height, topY, bottomY);
            this.factionRelationsList.setLeftPos(this.windowStartX + 10);
        }

        this.addRenderableWidget(this.factionRelationsList);
    }

    private void initFactionOptionsTab(int topY, int bottomY) {

        Button friendlyFireOn = Button.builder(Component.literal("True"), (btn) -> {
            NetworkHandler.CHANNEL.sendToServer(new PacketFactionFriendlyFireToggle(true));
        }).bounds(contentStartX + 170, getContentTopY(), 50, 20).build();
        friendlyFireOn.active = !this.friendlyFire && memberRanks.get(clientPlayerUUID) != MemberRank.MEMBER;
        this.addRenderableWidget(friendlyFireOn);

        Button friendlyFireOff = Button.builder(Component.literal("False"), (btn) -> {
            NetworkHandler.CHANNEL.sendToServer(new PacketFactionFriendlyFireToggle(true));
        }).bounds(contentStartX + 225, getContentTopY(), 50, 20).build();
        friendlyFireOff.active = this.friendlyFire && memberRanks.get(clientPlayerUUID) != MemberRank.MEMBER;
        this.addRenderableWidget(friendlyFireOff);


        // Center the button in the content area
        this.addRenderableWidget(Button.builder(Component.literal("Leave Faction"), (btn) -> {
            NetworkHandler.CHANNEL.sendToServer(new PacketFactionLeaveAction());
        }).bounds(this.windowStartX + this.imageWidth - this.spacing - 100, this.getContentBottomY() - buttonHeight, 100, 20).build());
    }

    private void initAllianceTab() {
        if (allianceName == null) {
            // Create Alliance View
            int centerY = this.windowStartY + (this.imageHeight / 2);
            EditBox nameField = new EditBox(this.font, this.windowStartX + (this.imageWidth / 2) - 80, centerY - 20, 160, 20, Component.literal("Name"));
            this.addRenderableWidget(nameField);
            this.addRenderableWidget(new Button.Builder(Component.literal("Confirm Create"), button -> {
                String name = nameField.getValue();
                if (!name.isEmpty()) {
                    NetworkHandler.CHANNEL.sendToServer(new PacketAllianceOperation(PacketAllianceOperation.Action.CREATE, name));
                    this.onClose();
                }
            }).bounds(this.windowStartX + (this.imageWidth / 2) - 40, centerY + 10, 80, 20).build());
        } else {
            int subButtonWidth = 70;
            int totalWidth = (subButtonWidth * 4) + (spacing * 3);
            int startX = this.windowStartX + (this.imageWidth - totalWidth) / 2;
            int startY = this.windowStartY + headerHeight + spacing;

            addRenderableWidget(createTabButton("Members", startX, startY, subButtonWidth, AllianceTab.MEMBERS, currentAllianceTab));
            addRenderableWidget(createTabButton("Invites", startX + subButtonWidth + spacing, startY, subButtonWidth, AllianceTab.INVITES, currentAllianceTab));
            addRenderableWidget(createTabButton("Relations", startX + (subButtonWidth + spacing) * 2, startY, subButtonWidth, AllianceTab.RELATIONS, currentAllianceTab));
            addRenderableWidget(createTabButton("Options", startX + (subButtonWidth + spacing) * 3, startY, subButtonWidth, AllianceTab.OPTIONS, currentAllianceTab));

            int topY = getContentTopY();
            int bottomY = getContentBottomY();

            switch (currentAllianceTab) {
                case MEMBERS -> initAllianceMembersTab(topY, bottomY);
                case INVITES -> initAllianceInvitesTab(topY, bottomY);
                case RELATIONS -> initAllianceRelationsTab(topY, bottomY);
                case OPTIONS -> initAllianceOptionsTab(topY, bottomY);
            }
        }
    }

    private void initAllianceMembersTab(int topY, int bottomY) {
        if (this.allianceMemberList == null) {
            this.allianceMemberList = new ScrollableAllianceMemberList(this.minecraft, this.imageWidth - 20, this.height, topY, bottomY, 30);
            this.allianceMemberList.setLeftPos(this.windowStartX + 10);
            for (String member : allianceMembers) {
                this.allianceMemberList.addMember(member, Objects.equals(member, factionName));
            }
        } else {
            this.allianceMemberList.updateSize(this.imageWidth - 20, this.height, topY, bottomY);
            this.allianceMemberList.setLeftPos(this.windowStartX + 10);
        }

        this.addRenderableWidget(this.allianceMemberList);
    }

    private void initAllianceInvitesTab(int topY, int bottomY) {
        if (this.allianceInviteList == null) {
            this.allianceInviteList = new ScrollableAllianceInviteList(this.minecraft, this.imageWidth - 20, this.height, topY, bottomY, 30);
            this.allianceInviteList.setLeftPos(this.windowStartX + 10);
            boolean localPlayerCanInvite = memberRanks.get(clientPlayerUUID) == MemberRank.OWNER;

            for (String invitedFaction : allianceInvites) {
                this.allianceInviteList.addInvite(invitedFaction, true, localPlayerCanInvite);
            }

            for (String invitedFaction : factionNames) {
                if (!allianceInvites.contains(invitedFaction) && !this.factionName.equals(invitedFaction)) {
                    this.allianceInviteList.addInvite(invitedFaction, false, localPlayerCanInvite);
                }
            }
        } else {
            this.allianceInviteList.updateSize(this.imageWidth - 20, this.height, topY, bottomY);
            this.allianceInviteList.setLeftPos(this.windowStartX + 10);
        }

        this.addRenderableWidget(this.allianceInviteList);
    }

    private void initAllianceRelationsTab(int topY, int bottomY) {
        if (this.allianceRelationsList == null) {
            this.allianceRelationsList = new ScrollableAllianceRelationsList(this.minecraft, this.imageWidth - 20, this.height, topY, bottomY, 30);
            this.allianceRelationsList.setLeftPos(this.windowStartX + 10);
            boolean playerIsOwnerOrOfficer = memberRanks.get(clientPlayerUUID) == MemberRank.OWNER;

            RelationshipStatus incomingRelationship;
            for (Map.Entry<String, RelationshipStatus> entry : outgoingAllianceRelations.entrySet()) {
                incomingRelationship = this.incomingAllianceRelations.get(entry.getKey());
                if (incomingRelationship == null) incomingRelationship = RelationshipStatus.NEUTRAL;
                this.allianceRelationsList.addAlliance(entry.getKey(), entry.getValue(), incomingRelationship, playerIsOwnerOrOfficer);
            }

            for (String currentAllianceName : allianceNames) {
                if (!outgoingAllianceRelations.containsKey(currentAllianceName) && !this.allianceName.equals(currentAllianceName)) {
                    incomingRelationship = this.incomingAllianceRelations.get(currentAllianceName);
                    if (incomingRelationship == null) incomingRelationship = RelationshipStatus.NEUTRAL;

                    this.allianceRelationsList.addAlliance(currentAllianceName, RelationshipStatus.NEUTRAL, incomingRelationship, playerIsOwnerOrOfficer);
                }
            }
        } else {
            this.allianceRelationsList.updateSize(this.imageWidth - 20, this.height, topY, bottomY);
            this.allianceRelationsList.setLeftPos(this.windowStartX + 10);
        }
        this.addRenderableWidget(this.allianceRelationsList);
    }

    private void initAllianceOptionsTab(int topY, int bottomY) {
        this.addRenderableWidget(Button.builder(Component.literal("Leave Alliance"), (btn) -> {
            NetworkHandler.CHANNEL.sendToServer(new PacketAllianceLeaveAction());
        }).bounds(this.windowStartX + (this.imageWidth / 2) - 50, topY + 20, 100, 20).build());
    }

    private void initSettingsTab() {
        int gap = 25;

        // Faction Abbreviation
        Button factionTrue = Button.builder(Component.literal("True"), (btn) -> {
            ClientConfig.setShowFactionAbbreviation(true);
            this.rebuildWidgets();
        }).bounds(contentStartX + 170, contentStartY, 50, 20).build();
        factionTrue.active = !ClientConfig.showFactionAbbreviation;
        this.addRenderableWidget(factionTrue);

        Button factionFalse = Button.builder(Component.literal("False"), (btn) -> {
            ClientConfig.setShowFactionAbbreviation(false);
            this.rebuildWidgets();
        }).bounds(contentStartX + 225, contentStartY, 50, 20).build();
        factionFalse.active = ClientConfig.showFactionAbbreviation;
        this.addRenderableWidget(factionFalse);

        // Alliance Abbreviation
        int y2 = contentStartY + gap;

        Button allianceTrue = Button.builder(Component.literal("True"), (btn) -> {
            ClientConfig.setShowAllianceAbbreviation(true);
            this.rebuildWidgets();
        }).bounds(contentStartX + 170, y2, 50, 20).build();
        allianceTrue.active = !ClientConfig.showAllianceAbbreviation;
        this.addRenderableWidget(allianceTrue);

        Button allianceFalse = Button.builder(Component.literal("False"), (btn) -> {
            ClientConfig.setShowAllianceAbbreviation(false);
            this.rebuildWidgets();
        }).bounds(contentStartX + 225, y2, 50, 20).build();
        allianceFalse.active = ClientConfig.showAllianceAbbreviation;
        this.addRenderableWidget(allianceFalse);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics); // Dims the world behind

        // Main Window Border
        guiGraphics.fill(windowStartX - 1, windowStartY - 1, windowStartX + imageWidth + 1, windowStartY + imageHeight + 1, COL_BORDER);

        // Header Background
        guiGraphics.fill(windowStartX, windowStartY, windowStartX + imageWidth, windowStartY + headerHeight, COL_HEADER_BG);

        // Body Background
        guiGraphics.fill(windowStartX, windowStartY + headerHeight, windowStartX + imageWidth, windowStartY + imageHeight, COL_WINDOW_BG);

        // Separator Line
        guiGraphics.hLine(windowStartX, windowStartX + imageWidth - 1, windowStartY + headerHeight, COL_BORDER);

        // Title (Top Left inside window)
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(windowStartX + 10, windowStartY + 3, 0);
        guiGraphics.pose().scale(1.1f, 1.1f, 1.1f); // Make the title bigger
        guiGraphics.drawString(this.font, this.title, 0, 0, 0xFFFFFF);
        guiGraphics.pose().popPose();

        // Draw Subtitle (e.g., faction name)
        if (currentMainTab == MainTab.FACTION) {
            guiGraphics.drawString(this.font, "Faction: " + this.factionName, windowStartX + 10, windowStartY + 37, 0xAAAAAA);
        } else if (currentMainTab == MainTab.ALLIANCE && allianceName != null) {
            guiGraphics.drawString(this.font, "Alliance: " + this.allianceName, windowStartX + 10, windowStartY + 37, 0xAAAAAA);
        }

        if (currentMainTab == MainTab.SETTINGS) {
            int gap = 25;
            guiGraphics.drawString(this.font, "Show Faction Abbreviation", contentStartX, contentStartY + 8, 0xFFFFFF, false);
            guiGraphics.drawString(this.font, "Show Alliance Abbreviation", contentStartX, contentStartY + gap + 8, 0xFFFFFF, false);
        }
        else if( currentMainTab == MainTab.FACTION && currentFactionTab == FactionTab.OPTIONS){
            guiGraphics.drawString(this.font, "Enable Friendly Fire", contentStartX + spacing, this.getContentTopY() + 8, 0xFFFFFF, false);
        }

        // List Background (Inset look)
        if (isListActive()) {
            int topY = getContentTopY();
            int bottomY = getContentBottomY();
            // Draw a black box behind the list items
            guiGraphics.fill(windowStartX + 10, topY, windowStartX + imageWidth - 10, bottomY, COL_LIST_BG);
            // Draw a border around the list
            guiGraphics.renderOutline(windowStartX + 10, topY, imageWidth - 20, bottomY - topY, 0xFF444444);
        }

        if (currentMainTab == MainTab.ALLIANCE && allianceName == null) {
            guiGraphics.drawCenteredString(this.font, "Create a new Alliance:", this.windowStartX + (this.imageWidth / 2), this.windowStartY + (this.imageHeight / 2) - 35, 0xFFFFFF);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private boolean isListActive() {
        if (currentMainTab == MainTab.FACTION) return currentFactionTab != FactionTab.OPTIONS;
        if (currentMainTab == MainTab.ALLIANCE && allianceName != null)
            return currentAllianceTab != AllianceTab.OPTIONS;
        return false;
    }
}