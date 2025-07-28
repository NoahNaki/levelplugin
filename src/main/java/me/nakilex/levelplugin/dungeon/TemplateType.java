package me.nakilex.levelplugin.dungeon;

/** Identifies the exact room template used in a layout. */
public enum TemplateType {
    NONE,
    ENTRANCE,
    DEAD_END,
    STRAIGHT,
    CORNER_LEFT,
    CORNER_RIGHT,
    TJUNCTION,
    TJUNCTION_LEFT,
    TJUNCTION_RIGHT,
    CROSSROAD,
    HALLWAY,
    TREASURE_LEFT,
    TREASURE_T_RIGHT,
    DECOR_STONE,
    DECOR_CHEST,
    BOSS,
    COMBAT_LEFT,
    COMBAT_RIGHT,
    LIBRARY,
    EXIT;
}
