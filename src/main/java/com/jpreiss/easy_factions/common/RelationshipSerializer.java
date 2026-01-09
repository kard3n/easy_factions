package com.jpreiss.easy_factions.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;

public class RelationshipSerializer {
    public static HashMap<String, RelationshipStatus> deserialize(ListTag relationshipTag) {
        HashMap<String, RelationshipStatus> result = new HashMap<>();
        for (Tag t : relationshipTag) {
            CompoundTag relationTag = (CompoundTag) t;
            String targetFaction = relationTag.getString("Target");
            try {
                RelationshipStatus status = RelationshipStatus.valueOf(relationTag.getString("Status"));
                result.put(targetFaction, status);
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }
        return result;
    }

    public static ListTag serialize(HashMap<String, RelationshipStatus> relationships) {
        ListTag relationsTag = new ListTag();
        for (String target : relationships.keySet()) {
            CompoundTag relationTag = new CompoundTag();
            relationTag.putString("Target", target);
            relationTag.putString("Status", relationships.get(target).name());
            relationsTag.add(relationTag);
        }
        return relationsTag;
    }
}
