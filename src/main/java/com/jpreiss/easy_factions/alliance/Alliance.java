package com.jpreiss.easy_factions.alliance;

import java.util.HashSet;
import java.util.Set;

public class Alliance {
    public String name;
    public Set<String> members;
    public Set<String> invited = new HashSet<>();

    public Alliance(String name, Set<String> members) {
        this.name = name;
        this.members =  members;
    }
}
