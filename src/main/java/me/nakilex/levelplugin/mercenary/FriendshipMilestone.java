package me.nakilex.levelplugin.mercenary;

import java.util.List;

/**
 * Immutable description of a collection-style friendship milestone. These are unlocked by reaching
 * a total friendship level across all mercenaries.
 */
public record FriendshipMilestone(int requiredTotalLevel, String label, FriendshipReward reward,
                                  List<String> flavor) {
}
