package com.jpreiss.easy_factions.faction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Faction {
    private String name;
    private UUID owner;
    private Set<UUID> members = new HashSet<>();
    private Set<UUID> officers = new HashSet<>();
    private Set<UUID> invited = new HashSet<>();
    private boolean friendlyFire = false;

    public Faction(String name, UUID owner) {
        this.name = name;
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
}
