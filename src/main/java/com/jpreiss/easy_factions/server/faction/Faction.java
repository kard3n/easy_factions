package com.jpreiss.easy_factions.server.faction;

import com.jpreiss.easy_factions.common.RelationshipSerializer;
import com.jpreiss.easy_factions.common.RelationshipStatus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Faction {
    private String name;
    private String abbreviation;
    private UUID owner;
    private Set<UUID> members = new HashSet<>();
    private Set<UUID> officers = new HashSet<>();
    private Set<UUID> invited = new HashSet<>();
    private boolean friendlyFire = false;
    // Outgoing relations (what this faction set the status of others too)
    // Faction name, status
    private HashMap<String, RelationshipStatus> outgoingRelations = new HashMap<>();
    // Incoming relations (what others set the status of this faction too)
    // Faction name, status
    private HashMap<String, RelationshipStatus> incomingRelations = new HashMap<>();

    public Faction(String name, String abbreviation, UUID owner) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.owner = owner;
        this.members.add(owner);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Set<UUID> getMembers() {
        return members;
    }

    public void setMembers(Set<UUID> members) {
        this.members = members;
    }

    public Set<UUID> getInvited() {
        return invited;
    }

    public void setInvited(Set<UUID> invited) {
        this.invited = invited;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public boolean getFriendlyFire() {
        return friendlyFire;
    }

    public Set<UUID> getOfficers() {
        return officers;
    }

    public void setOfficers(Set<UUID> officers) {
        this.officers = officers;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public HashMap<String, RelationshipStatus> getIncomingRelations() {
        return incomingRelations;
    }

    public void setIncomingRelations(HashMap<String, RelationshipStatus> incomingRelations) {
        this.incomingRelations = incomingRelations;
    }

    public HashMap<String, RelationshipStatus> getOutgoingRelations() {
        return outgoingRelations;
    }

    public void setOutgoingRelations(HashMap<String, RelationshipStatus> outgoingRelations) {
        this.outgoingRelations = outgoingRelations;
    }

    public static Faction deserialize(CompoundTag fTag) {
        String name = fTag.getString("Name");
        String abbreviation = fTag.contains("Abbreviation") ? fTag.getString("Abbreviation") : null; // Could not exist (null)
        if (abbreviation != null && abbreviation.isEmpty()) {
            abbreviation = null;
        }
        UUID owner = fTag.getUUID("Owner");
        boolean friendlyFire = fTag.getBoolean("FriendlyFire");

        // Reconstruct Faction object
        Faction faction = new Faction(name, abbreviation, owner);
        faction.setFriendlyFire(friendlyFire); // Assuming setter exists

        // Load Members
        ListTag membersTag = fTag.getList("Members", Tag.TAG_INT_ARRAY); // UUIDs are saved as IntArrays in NBT
        for (Tag value : membersTag) {
            faction.getMembers().add(NbtUtils.loadUUID(value));
        }

        // Load Invites
        ListTag invitedTag = fTag.getList("Invited", Tag.TAG_INT_ARRAY);
        for (Tag value : invitedTag) {
            faction.getInvited().add(NbtUtils.loadUUID(value));
        }

        ListTag officersTag = fTag.getList("Officers", Tag.TAG_INT_ARRAY);
        for (Tag value : officersTag) {
            faction.getOfficers().add(NbtUtils.loadUUID(value));
        }

        // Add outgoing relatioships
        ListTag outgoingRelationsTag = fTag.getList("OutgoingRelations", Tag.TAG_COMPOUND);
        faction.setOutgoingRelations(RelationshipSerializer.deserialize(outgoingRelationsTag));

        return faction;
    }

    public CompoundTag serialize() {
        CompoundTag fTag = new CompoundTag();

        String abbreviation = this.getAbbreviation();

        fTag.putString("Name", this.getName());
        if (abbreviation != null) { // Could be null
            fTag.putString("Abbreviation", abbreviation);
        }
        fTag.putUUID("Owner", this.getOwner());
        fTag.putBoolean("FriendlyFire", this.isFriendlyFire()); // Assuming getter exists

        // Save Members
        ListTag membersTag = new ListTag();
        for (UUID member : this.getMembers()) {
            membersTag.add(NbtUtils.createUUID(member));
        }
        fTag.put("Members", membersTag);

        // Save Invites
        ListTag invitedTag = new ListTag();
        for (UUID invited : this.getInvited()) {
            invitedTag.add(NbtUtils.createUUID(invited));
        }
        fTag.put("Invited", invitedTag);

        // Save officers
        ListTag officersTag = new ListTag();
        for (UUID officer : this.getOfficers()) {
            officersTag.add(NbtUtils.createUUID(officer));
        }
        fTag.put("Officers", officersTag);

        // Save outgoing relations
        fTag.put("OutgoingRelations", RelationshipSerializer.serialize(this.getOutgoingRelations()));

        return fTag;
    }
}
