package com.jpreiss.easy_factions.server.alliance;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

public class Alliance {
    private String name;
    private String abbreviation;
    private Set<String> members;
    private Set<String> invited = new HashSet<>();

    public Alliance(String name, String abbreviation, Set<String> members) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.members =  members;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void setMembers(Set<String> members) {
        this.members = members;
    }

    public Set<String> getInvited() {
        return invited;
    }

    public void setInvited(Set<String> invited) {
        this.invited = invited;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public static Alliance deserialize(CompoundTag allianceTag){
        String name = allianceTag.getString("Name");
        String abbreviation = allianceTag.contains("Abbreviation") ? allianceTag.getString("Abbreviation") : null; // Could not exist (null)
        if(abbreviation != null && abbreviation.isEmpty()){
            abbreviation = null;
        }
        Set<String> members = new HashSet<>();
        Set<String> invited = new HashSet<>();

        // Load Members
        ListTag membersTag = allianceTag.getList("Members", Tag.TAG_STRING);
        for (int j = 0; j < membersTag.size(); j++) {
            members.add(membersTag.getString(j));
        }

        // Load Invites
        ListTag invitedTag = allianceTag.getList("Invited", Tag.TAG_STRING);
        for (int j = 0; j < invitedTag.size(); j++) {
            invited.add(invitedTag.getString(j));
        }

        // Create Alliance Object
        Alliance alliance = new Alliance(name, abbreviation, members);
        alliance.getInvited().addAll(invited);

        return alliance;
    }

    public CompoundTag serialize(){
        CompoundTag allianceTag = new CompoundTag();
        allianceTag.putString("Name", this.getName());
        String abbreviation = this.getAbbreviation();

        if (abbreviation != null) {
            allianceTag.putString("Abbreviation", abbreviation);
        }

        // Save Members
        ListTag membersTag = new ListTag();
        for (String member : this.getMembers()) {
            membersTag.add(StringTag.valueOf(member));
        }
        allianceTag.put("Members", membersTag);

        // Save Invited
        ListTag invitedTag = new ListTag();
        for (String invitedFaction : this.getInvited()) {
            invitedTag.add(StringTag.valueOf(invitedFaction));
        }
        allianceTag.put("Invited", invitedTag);

        return allianceTag;
    }
}
