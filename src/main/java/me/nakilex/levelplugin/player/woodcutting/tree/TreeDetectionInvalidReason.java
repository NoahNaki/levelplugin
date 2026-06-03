package me.nakilex.levelplugin.player.woodcutting.tree;

public enum TreeDetectionInvalidReason {
    NONE,
    UNKNOWN_TREE_TYPE,
    CLICKED_TOO_HIGH,
    TOO_FEW_LOGS,
    TOO_FEW_LEAVES,
    TOO_MANY_LOGS,
    TOO_MANY_LEAVES,
    FAILED_NATURAL_VALIDATION,
    PLAYER_PLACED_REJECTED
}
