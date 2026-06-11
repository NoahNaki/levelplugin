package me.nakilex.levelplugin.settings.data;

import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.settings.data.PlayerVisibility;
import me.nakilex.levelplugin.spells.input.SpellInputMode;

public class PlayerSettings {

    private boolean dmgChat     = true;
    private boolean dmgNumber   = false;
    private boolean dropDetails = true;
    private boolean dropDetailsChatEnabled = false;
    private boolean partyGlow = true;
    private boolean friendGlow = true;
    private boolean balancePublic = true;
    private PlayerVisibility playerVisibility = PlayerVisibility.SHOW_ALL;
    private boolean autoSkipCutscenes = false;
    private boolean autoSkipSongs = false;
    private boolean skillPointReminder = true;
    private boolean fullInventoryTitle = true;
    private boolean boosterBossBarEnabled = true;
    private boolean questTrackingParticles = true;
    private boolean tipsEnabled = true;
    private boolean chatGamesEnabled = true;
    private boolean npcSoundEffects = true;
    private SpellInputMode spellInputMode = SpellInputMode.MOUSE_COMBO;
    private static final ItemRarity[] LOOT_PICKUP_RARITIES = {
            ItemRarity.COMMON,
            ItemRarity.UNCOMMON,
            ItemRarity.RARE,
            ItemRarity.EPIC,
            ItemRarity.LEGENDARY
    };

    private ItemRarity lootPickupRarity = ItemRarity.COMMON;

    public boolean isDmgChatEnabled() {
        return dmgChat;
    }

    public void toggleDmgChat() {
        this.dmgChat = !this.dmgChat;
    }

    public boolean isDmgNumberEnabled() {
        return dmgNumber;
    }

    public void toggleDmgNumber() {
        this.dmgNumber = !this.dmgNumber;
    }

    public boolean isDropDetailsEnabled() {
        return dropDetails;
    }

    public void toggleDropDetails() {
        this.dropDetails = !this.dropDetails;
    }

    public boolean isDropDetailsChatEnabled() {
        return dropDetailsChatEnabled;
    }

    public void toggleDropDetailsChat() {
        dropDetailsChatEnabled = !dropDetailsChatEnabled;
    }

    public boolean isPartyGlowEnabled() {
        return partyGlow;
    }

    public void togglePartyGlow() {
        partyGlow = !partyGlow;
    }

    public boolean isFriendGlowEnabled() {
        return friendGlow;
    }

    public void toggleFriendGlow() {
        friendGlow = !friendGlow;
    }

    public boolean isBalancePublic() {
        return balancePublic;
    }

    public void toggleBalancePublic() {
        balancePublic = !balancePublic;
    }

    public PlayerVisibility getPlayerVisibility() {
        return playerVisibility;
    }

    /** Cycle visibility setting through the three states. */
    public void cyclePlayerVisibility() {
        switch (playerVisibility) {
            case SHOW_ALL -> playerVisibility = PlayerVisibility.FRIENDS_ONLY;
            case FRIENDS_ONLY -> playerVisibility = PlayerVisibility.HIDE_ALL;
            case HIDE_ALL -> playerVisibility = PlayerVisibility.SHOW_ALL;
        }
    }

    public boolean isAutoSkipCutscenes() {
        return autoSkipCutscenes;
    }

    public void toggleAutoSkipCutscenes() {
        autoSkipCutscenes = !autoSkipCutscenes;
    }

    public boolean isAutoSkipSongs() {
        return autoSkipSongs;
    }

    public void toggleAutoSkipSongs() {
        autoSkipSongs = !autoSkipSongs;
    }

    public boolean isSkillPointReminderEnabled() {
        return skillPointReminder;
    }

    public void toggleSkillPointReminder() {
        skillPointReminder = !skillPointReminder;
    }

    public boolean isFullInventoryTitleEnabled() {
        return fullInventoryTitle;
    }

    public void toggleFullInventoryTitle() {
        fullInventoryTitle = !fullInventoryTitle;
    }

    public boolean isBoosterBossBarEnabled() {
        return boosterBossBarEnabled;
    }

    public void toggleBoosterBossBar() {
        boosterBossBarEnabled = !boosterBossBarEnabled;
    }

    public boolean isQuestTrackingParticlesEnabled() {
        return questTrackingParticles;
    }

    public void toggleQuestTrackingParticles() {
        questTrackingParticles = !questTrackingParticles;
    }

    public boolean isTipsEnabled() {
        return tipsEnabled;
    }

    public void toggleTipsEnabled() {
        tipsEnabled = !tipsEnabled;
    }

    public boolean isChatGamesEnabled() {
        return chatGamesEnabled;
    }

    public void toggleChatGamesEnabled() {
        chatGamesEnabled = !chatGamesEnabled;
    }

    public boolean isNpcSoundEffectsEnabled() {
        return npcSoundEffects;
    }

    public void toggleNpcSoundEffects() {
        npcSoundEffects = !npcSoundEffects;
    }

    public void setNpcSoundEffects(boolean npcSoundEffects) {
        this.npcSoundEffects = npcSoundEffects;
    }

    public SpellInputMode getSpellInputMode() {
        return spellInputMode;
    }

    public void cycleSpellInputMode() {
        spellInputMode = spellInputMode.next();
    }

    public void setSpellInputMode(SpellInputMode spellInputMode) {
        if (spellInputMode == null) {
            return;
        }
        this.spellInputMode = spellInputMode;
    }

    public ItemRarity getLootPickupRarity() {
        return lootPickupRarity;
    }

    public ItemRarity[] getLootPickupRarities() {
        return LOOT_PICKUP_RARITIES.clone();
    }

    public void cycleLootPickupRarity(boolean forward) {
        ItemRarity[] rarities = LOOT_PICKUP_RARITIES;
        int idx = 0;
        for (int i = 0; i < rarities.length; i++) {
            if (rarities[i] == lootPickupRarity) {
                idx = i;
                break;
            }
        }
        idx = forward ? idx + 1 : idx - 1;
        if (idx < 0) {
            idx = rarities.length - 1;
        } else if (idx >= rarities.length) {
            idx = 0;
        }
        lootPickupRarity = rarities[idx];
    }

    public boolean isLootPickupAllowed(ItemRarity rarity) {
        if (rarity == null || lootPickupRarity == null) {
            return true;
        }
        return rarity.ordinal() >= lootPickupRarity.ordinal();
    }
}
