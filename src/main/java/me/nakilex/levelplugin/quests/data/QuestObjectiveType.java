package me.nakilex.levelplugin.quests.data;

public enum QuestObjectiveType {
    KILL,
    COLLECT,
    INTERACT,
    BUY,
    UPGRADE,
    CAST,
    CRAFT,
    DUEL,
    ESCORT,
    TALK,
    EXPLORE,
    SELECT_CLASS,
    /** Enchant an item via the enchant system */
    ENCHANT,
    /** Discover a fast travel region */
    DISCOVER,
    /** Drink a consumable potion */
    CONSUME_POTION,
    /** Accumulate play time in minutes */
    PLAY_TIME,
    /** Buy an item from the auction house */
    AUCTION_BUY,
    /** List an item on the auction house */
    AUCTION_LIST,
    /** Successfully sell an item on the auction house */
    AUCTION_SELL,
    /** Place a bid on an auction */
    AUCTION_BID,
    /** Upgrade a town or environment */
    TOWN_UPGRADE,
    /** Repair an item at the blacksmith */
    BLACKSMITH_REPAIR,
    /** Reroll item stats at the blacksmith */
    BLACKSMITH_REROLL,
    /** Salvage an item */
    SALVAGE,
    /** Unlock a waystone */
    WAYSTONE_UNLOCK,
    /** Use a waystone */
    WAYSTONE_USE,
    /** Cast a specific combo */
    CAST_COMBO,
    /** Equip a rune */
    RUNE_EQUIP,
    /** Unequip a rune */
    RUNE_UNEQUIP,
    /** Participate in a duel */
    DUEL_PARTICIPATE,
    /** Lose a duel */
    DUEL_LOSE
}
