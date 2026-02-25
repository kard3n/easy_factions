package com.jpreiss.easy_factions.common;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;

public enum RelationshipStatus {
    FRIENDLY,
    NEUTRAL,
    HOSTILE;

    /**
     * Returns the more hostile of two relations
     */
    public static RelationshipStatus getLowestByPriority(RelationshipStatus status1, RelationshipStatus status2){
        if(status1 == RelationshipStatus.HOSTILE || status2 == RelationshipStatus.HOSTILE) return RelationshipStatus.HOSTILE;
        if(status1 == RelationshipStatus.NEUTRAL || status2 == RelationshipStatus.NEUTRAL) return RelationshipStatus.NEUTRAL;
        return RelationshipStatus.FRIENDLY;
    }

    /**
     * Suggests values from RelationshipStatus enum
     */
    public static final SuggestionProvider<CommandSourceStack> RELATIONSHIP_STATUS = (context, builder) -> {
        for (RelationshipStatus status : RelationshipStatus.values()) {
            builder.suggest(status.name());
        }
        return builder.buildFuture();
    };
}
