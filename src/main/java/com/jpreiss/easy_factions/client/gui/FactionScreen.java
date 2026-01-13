package com.jpreiss.easy_factions.client.gui;


import com.jpreiss.easy_factions.common.MemberRank;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import com.jpreiss.easy_factions.network.NetworkHandler;
import com.jpreiss.easy_factions.network.packet.gui.PacketAllianceLeaveAction;
import com.jpreiss.easy_factions.network.packet.gui.PacketAllianceOperation;
import com.jpreiss.easy_factions.network.packet.gui.PacketFactionCreateAction;
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
    private enum MainTab {
        FACTION, ALLIANCE, SETTINGS
    }

    private enum FactionTab {
        MEMBERS, INVITES, RELATIONS, OPTIONS
    }
    private enum AllianceTab {
        MEMBERS, INVITES, RELATIONS, OPTIONS
    }

    private static MainTab currentMainTab = MainTab.FACTION;
    private static FactionTab currentFactionTab = FactionTab.MEMBERS;
    private static AllianceTab currentAllianceTab = AllianceTab.MEMBERS;


    // position and width of the window
    private int windowStartX;
    private int windowStartY;
    private final int imageWidth = 320;
    private final int imageHeight = 250;

    private final int headerButtonWidth = 60;
    private final int buttonHeight = 20;
    private final int spacing = 5;
    private int topHeaderEndY;

    private final int backgroundColor = 0xFF404040;

    // Scrollable list for factions
    private ScrollableFactionMemberList factionMemberList;
    private ScrollableFactionInviteList factionInviteList;
    private ScrollableFactionRelationsList factionRelationsList;

    // Scrollable list for alliances
    private ScrollableAllianceMemberList allianceMemberList;
    private ScrollableAllianceInviteList allianceInviteList;
    private ScrollableAllianceRelationsList allianceRelationsList;

    // Data
    private final String factionName;
    private final Map<UUID, MemberRank> memberRanks; // UUID -> MemberRank
    private final Map<UUID, String> playerNames; // UUID -> Name
    private final UUID clientPlayerUUID;
    private final List<UUID> factionInvites;
    private final Map<String, RelationshipStatus> outgoingFactionRelations;
    private final List<String> factionNames;
    private final String allianceName;
    private final List<String> allianceMembers;
    private final List<String> allianceInvites;
    private final List<String> allianceNames;
    private final Map<String, RelationshipStatus> outgoingAllianceRelations;


    public FactionScreen(
            String factionName,
            Map<UUID, MemberRank> memberRanks,
            Map<UUID, String> playerNames, List<UUID> factionInvites,
            Map<String, RelationshipStatus> outgoingFactionRelations,
            List<String> factionNames,
            String allianceName,
            List<String> allianceMembers,
            List<String> allianceInvites,
            List<String> allianceNames,
            Map<String, RelationshipStatus> outgoingAllianceRelations) {
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
    }

    @Override
    protected void init() {
        super.init();

        this.windowStartX = (this.width - this.imageWidth) / 2;
        this.windowStartY = (this.height - this.imageHeight) / 2;
        this.topHeaderEndY = this.windowStartY + spacing + buttonHeight;

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
            case ALLIANCE -> initAllianceTab();
            case SETTINGS -> initSettingsTab();
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

    private void switchAllianceTabs(AllianceTab allianceTab){
        if (currentAllianceTab != allianceTab) {
            currentAllianceTab = allianceTab;
            this.rebuildWidgets();
        }
    }


    private void initFactionTab() {

        int buttonStartX = (this.windowStartX + this.imageWidth / 2) - (this.headerButtonWidth * 4 - this.spacing * 2) / 2 - this.spacing; // Center buttons
        int headerStartY = this.topHeaderEndY + spacing;

        // Members button
        Button membersBtn = Button.builder(Component.literal("Members"), (btn) -> {
                    this.switchFactionTab(FactionTab.MEMBERS);
                })
                .bounds(buttonStartX, headerStartY, headerButtonWidth, buttonHeight).build();
        membersBtn.active = (currentFactionTab != FactionTab.MEMBERS);
        this.addRenderableWidget(membersBtn);

        // Invites button
        Button invitesBtn = Button.builder(Component.literal("Invites"), (btn) -> this.switchFactionTab(FactionTab.INVITES))
                .bounds(buttonStartX + headerButtonWidth + this.spacing, headerStartY, headerButtonWidth, buttonHeight).build();
        invitesBtn.active = (currentFactionTab != FactionTab.INVITES);
        this.addRenderableWidget(invitesBtn);

        // Relations button
        Button relationsBtn = Button.builder(Component.literal("Relations"), (btn) -> this.switchFactionTab(FactionTab.RELATIONS))
                .bounds(buttonStartX + (headerButtonWidth + this.spacing) * 2, headerStartY, headerButtonWidth, buttonHeight).build();
        relationsBtn.active = (currentFactionTab != FactionTab.RELATIONS);
        this.addRenderableWidget(relationsBtn);

        // Options button
        Button optionsBtn = Button.builder(Component.literal("Options"), (btn) -> this.switchFactionTab(FactionTab.OPTIONS))
                .bounds(buttonStartX + (headerButtonWidth + this.spacing) * 3, headerStartY, headerButtonWidth, buttonHeight).build();
        optionsBtn.active = (currentFactionTab != FactionTab.OPTIONS);
        this.addRenderableWidget(optionsBtn);

        // Set where the tab object should start and end
        int topY = headerStartY + this.buttonHeight + spacing * 2;
        int bottomY = this.windowStartY + this.imageHeight - spacing;
        switch (currentFactionTab) {
            case MEMBERS -> initFactionMembersTab(topY, bottomY);
            case INVITES -> initFactionInvitesTab(topY, bottomY);
            case RELATIONS -> initFactionRelationsTab(topY, bottomY);
            case OPTIONS -> initFactionOptionsTab(topY, bottomY);
        }

    }

    private void initFactionMembersTab(int topY, int bottomY) {
        if (this.factionMemberList == null) {
            this.factionMemberList = new ScrollableFactionMemberList(this.minecraft, this.width, this.height, topY, bottomY, 30);

            for (Map.Entry<UUID, MemberRank> member : memberRanks.entrySet()) {
                this.factionMemberList.addMember(playerNames.get(member.getKey()), member.getKey(), member.getValue(), memberRanks.get(clientPlayerUUID), clientPlayerUUID);
            }

        } else {
            // Resize in case it already exists and the scale has updated
            this.factionMemberList.updateSize(this.width, this.height, topY, bottomY);
        }

        this.addRenderableWidget(this.factionMemberList);
    }

    private void initFactionInvitesTab(int topY, int bottomY) {
        if (this.factionInviteList == null) {
            this.factionInviteList = new ScrollableFactionInviteList(this.minecraft, this.width, this.height, topY, bottomY, 30);

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
            // Resize in case it already exists and the scale has updated
            this.factionInviteList.updateSize(this.width, this.height, topY, bottomY);
        }

        this.addRenderableWidget(this.factionInviteList);
    }

    private void initFactionRelationsTab(int topY, int bottomY) {
        if (this.factionRelationsList == null) {
            this.factionRelationsList = new ScrollableFactionRelationsList(this.minecraft, this.width, this.height, topY, bottomY, 30);

            boolean playerIsOwnerOrOfficer = memberRanks.get(clientPlayerUUID) != MemberRank.MEMBER;

            for (Map.Entry<String, RelationshipStatus> entry : outgoingFactionRelations.entrySet()) {
                this.factionRelationsList.addFaction(entry.getKey(), entry.getValue(), playerIsOwnerOrOfficer);
            }

            for (String factionName : factionNames) {
                if (!outgoingFactionRelations.containsKey(factionName) && !this.factionName.equals(factionName)) {
                    this.factionRelationsList.addFaction(factionName, RelationshipStatus.NEUTRAL, playerIsOwnerOrOfficer);
                }
            }

        } else {
            // Resize in case it already exists and the scale has updated
            this.factionRelationsList.updateSize(this.width, this.height, topY, bottomY);
        }

        this.addRenderableWidget(this.factionRelationsList);
    }

    private void initFactionOptionsTab(int topY, int bottomY) {
        int buttonStartX = this.windowStartX + this.spacing;
        this.addRenderableWidget(Button.builder(Component.literal("Leave Faction"), (btn) -> {
            NetworkHandler.CHANNEL.sendToServer(new PacketFactionLeaveAction());
        }).bounds(buttonStartX, topY + spacing, headerButtonWidth, buttonHeight).build());
    }

    private void initAllianceTab() {
        int buttonStartX = (this.windowStartX + this.imageWidth / 2) - (this.headerButtonWidth * 4 - this.spacing * 2) / 2 - this.spacing; // Center buttons
        int headerStartY = this.topHeaderEndY + spacing;

        if(allianceName == null){
            EditBox nameField = new EditBox(this.font, this.windowStartX + this.imageWidth / 2, this.windowStartY + (this.imageHeight / 2) - 20, 160, 20, Component.literal("Name"));
            this.addRenderableWidget(nameField);

            this.addRenderableWidget(new Button.Builder(Component.literal("Confirm Create"), button -> {
                String name = nameField.getValue();
                if (!name.isEmpty()) {
                    NetworkHandler.CHANNEL.sendToServer(new PacketAllianceOperation(PacketAllianceOperation.Action.CREATE, name));
                    this.onClose();
                }
            }).bounds(this.windowStartX + this.imageWidth / 2, this.windowStartY + (this.imageHeight / 2) + 10, 80, 20).build());

        }
        else{
            // Members button
            Button membersBtn = Button.builder(Component.literal("Members"), (btn) -> {
                        this.switchAllianceTabs(AllianceTab.MEMBERS);
                    })
                    .bounds(buttonStartX, headerStartY, headerButtonWidth, buttonHeight).build();
            membersBtn.active = (currentAllianceTab != AllianceTab.MEMBERS);
            this.addRenderableWidget(membersBtn);

            // Invites button
            Button invitesBtn = Button.builder(Component.literal("Invites"), (btn) -> this.switchAllianceTabs(AllianceTab.INVITES))
                    .bounds(buttonStartX + headerButtonWidth + this.spacing, headerStartY, headerButtonWidth, buttonHeight).build();
            invitesBtn.active = (currentAllianceTab != AllianceTab.INVITES);
            this.addRenderableWidget(invitesBtn);

            // Relations button
            Button relationsBtn = Button.builder(Component.literal("Relations"), (btn) -> this.switchAllianceTabs(AllianceTab.RELATIONS))
                    .bounds(buttonStartX + (headerButtonWidth + this.spacing) * 2, headerStartY, headerButtonWidth, buttonHeight).build();
            relationsBtn.active = (currentAllianceTab != AllianceTab.RELATIONS);
            this.addRenderableWidget(relationsBtn);

            // Options button
            Button optionsBtn = Button.builder(Component.literal("Options"), (btn) -> this.switchAllianceTabs(AllianceTab.OPTIONS))
                    .bounds(buttonStartX + (headerButtonWidth + this.spacing) * 3, headerStartY, headerButtonWidth, buttonHeight).build();
            optionsBtn.active = (currentAllianceTab != AllianceTab.OPTIONS);
            this.addRenderableWidget(optionsBtn);

            // Set where the tab object should start and end
            int topY = headerStartY + this.buttonHeight + spacing * 2;
            int bottomY = this.windowStartY + this.imageHeight - spacing;
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
            this.allianceMemberList = new ScrollableAllianceMemberList(this.minecraft, this.width, this.height, topY, bottomY, 30);

            for (String member : allianceMembers) {
                this.allianceMemberList.addMember(member, Objects.equals(member, factionName));
            }

        } else {
            // Resize in case it already exists and the scale has updated
            this.allianceMemberList.updateSize(this.width, this.height, topY, bottomY);
        }

        this.addRenderableWidget(this.allianceMemberList);
    }

    private void initAllianceInvitesTab(int topY, int bottomY) {
        if (this.allianceInviteList == null) {
            this.allianceInviteList = new ScrollableAllianceInviteList(this.minecraft, this.width, this.height, topY, bottomY, 30);

            boolean localPlayerCanInvite = memberRanks.get(clientPlayerUUID) == MemberRank.OWNER;

            for (String invitedFaction: allianceInvites) {
                this.allianceInviteList.addInvite(invitedFaction, true, localPlayerCanInvite);
            }

            for (String invitedFaction: factionNames) {
                if(!allianceInvites.contains(invitedFaction) && !this.factionName.equals(invitedFaction)){
                    this.allianceInviteList.addInvite(invitedFaction, false, localPlayerCanInvite);
                }
            }

        } else {
            // Resize in case it already exists and the scale has updated
            this.allianceInviteList.updateSize(this.width, this.height, topY, bottomY);
        }

        this.addRenderableWidget(this.allianceInviteList);
    }

    private void initAllianceRelationsTab(int topY, int bottomY) {
        if (this.allianceRelationsList == null) {
            this.allianceRelationsList = new ScrollableAllianceRelationsList(this.minecraft, this.width, this.height, topY, bottomY, 30);

            boolean playerIsOwnerOrOfficer = memberRanks.get(clientPlayerUUID) == MemberRank.OWNER;

            for (Map.Entry<String, RelationshipStatus> entry : outgoingAllianceRelations.entrySet()) {
                this.allianceRelationsList.addAlliance(entry.getKey(), entry.getValue(), playerIsOwnerOrOfficer);
            }

            for (String currentAllianceName : allianceNames) {
                if (!outgoingAllianceRelations.containsKey(currentAllianceName) && !this.allianceName.equals(currentAllianceName)) {
                    this.allianceRelationsList.addAlliance(currentAllianceName, RelationshipStatus.NEUTRAL, playerIsOwnerOrOfficer);
                }
            }

        } else {
            // Resize in case it already exists and the scale has updated
            this.allianceRelationsList.updateSize(this.width, this.height, topY, bottomY);
        }

        this.addRenderableWidget(this.allianceRelationsList);
    }

    private void initAllianceOptionsTab(int topY, int bottomY) {
        int buttonStartX = this.windowStartX + this.spacing;
        this.addRenderableWidget(Button.builder(Component.literal("Leave Alliance"), (btn) -> {
            NetworkHandler.CHANNEL.sendToServer(new PacketAllianceLeaveAction());
        }).bounds(buttonStartX, topY + spacing, headerButtonWidth, buttonHeight).build());
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


    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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

        if(currentMainTab == MainTab.ALLIANCE && allianceName == null){
            guiGraphics.drawString(this.font, "Create alliance:", this.windowStartX + this.imageWidth / 2, this.windowStartY + (this.imageHeight / 2) - 35, 0x404040, false);
        }

        /*
        switch (currentMainTab) {
            case FACTION -> {
            }
            case ALLIANCE -> {
                guiGraphics.drawCenteredString(this.font, "Soon", this.width / 2, 120, 0xFF5555);
            }
            case SETTINGS -> {
                guiGraphics.drawCenteredString(this.font, "Soon", this.width / 2, 60, 0x55FF55);
            }
        }*/

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}