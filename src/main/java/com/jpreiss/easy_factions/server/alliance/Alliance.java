package com.jpreiss.easy_factions.server.alliance;

import java.util.HashSet;
import java.util.Set;

public class Alliance {
    private String name;
    private Set<String> members;
    private Set<String> invited = new HashSet<>();

    public Alliance(String name, Set<String> members) {
        this.name = name;
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
}
