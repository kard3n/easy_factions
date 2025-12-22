package com.jpreiss.easy_factions.faction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Faction {
    public String name;
    public UUID owner;
    public Set<UUID> members = new HashSet<>();
    public Set<UUID> invited = new HashSet<>();
    public boolean friendlyFire = false;

    public Faction(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        this.members.add(owner);
    }
}
