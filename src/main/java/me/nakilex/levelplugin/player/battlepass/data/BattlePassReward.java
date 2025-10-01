package me.nakilex.levelplugin.player.battlepass.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes the state of a single reward within a battle pass tier.
 * The record keeps lightweight metadata that the GUI can render without
 * depending on how rewards are granted internally.
 */
public record BattlePassReward(
        String title,
        List<String> description,
        boolean claimed,
        boolean claimable
) {
    public BattlePassReward {
        title = title == null ? "" : title;
        if (description == null || description.isEmpty()) {
            description = Collections.emptyList();
        } else {
            description = Collections.unmodifiableList(new ArrayList<>(description));
        }
    }
}
