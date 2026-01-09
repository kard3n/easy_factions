package com.jpreiss.easy_factions.common;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;

public enum RelationshipStatus {
    FRIENDLY,
    NEUTRAL,
    HOSTILE;

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
