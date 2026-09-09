package me.nakilex.levelplugin.settings;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.mob.managers.ChatToggleManager;
import me.nakilex.levelplugin.settings.data.PlayerSettings;
import me.nakilex.levelplugin.settings.data.PlayerVisibility;
import me.nakilex.levelplugin.settings.managers.SettingsManager;
import me.nakilex.levelplugin.spells.input.SpellInputMode;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Applies a desired settings value and runs the side effects that belong to it.
 *
 * The inventory settings menu changes one setting per click while the native
 * settings dialogs submit a whole form at once. Both go through this class so
 * the side effects (manager sync, /toggle dispatch, profile save, chat feedback)
 * stay identical no matter which front-end changed the value.
 *
 * Every method is a no-op returning false when the value already matches, which
 * is what lets a dialog submit its untouched fields harmlessly.
 */
public final class SettingsActions {

    private final SettingsManager settingsManager;

    public SettingsActions(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
    }

    public boolean setDamageChat(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isDmgChatEnabled() == desired) return false;
        settings.toggleDmgChat();
        boolean enabled = settings.isDmgChatEnabled();
        ChatToggleManager.getInstance().setEnabled(player, enabled);
        ToggleFeedbackUtil.sendToggle(player, "Damage chat", enabled);
        return true;
    }

    public boolean setDamageNumbers(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isDmgNumberEnabled() == desired) return false;
        // PlayerToggleManager keeps its own state, so the stored setting and the
        // command are both flipped to keep the two aligned.
        settings.toggleDmgNumber();
        Bukkit.dispatchCommand(player, "dmgnumber");
        return true;
    }

    public boolean setDropDetails(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isDropDetailsEnabled() == desired) return false;
        // /toggle owns the flip for this one; it updates PlayerSettings itself.
        Bukkit.dispatchCommand(player, "toggle dropdetails");
        return true;
    }

    public boolean setDropDetailsChat(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isDropDetailsChatEnabled() == desired) return false;
        Bukkit.dispatchCommand(player, "toggle dropdetailschat");
        return true;
    }

    public boolean setPartyGlow(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isPartyGlowEnabled() == desired) return false;
        settings.togglePartyGlow();
        Bukkit.dispatchCommand(player, "partyglow");
        return true;
    }

    public boolean setFriendGlow(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isFriendGlowEnabled() == desired) return false;
        settings.toggleFriendGlow();
        Bukkit.dispatchCommand(player, "friendglow");
        return true;
    }

    public boolean setBalancePublic(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isBalancePublic() == desired) return false;
        settings.toggleBalancePublic();
        return true;
    }

    public boolean setAutoSkipCutscenes(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isAutoSkipCutscenes() == desired) return false;
        settings.toggleAutoSkipCutscenes();
        return true;
    }

    public boolean setAutoSkipSongs(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isAutoSkipSongs() == desired) return false;
        Bukkit.dispatchCommand(player, "toggle songskip");
        return true;
    }

    public boolean setNpcSoundEffects(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isNpcSoundEffectsEnabled() == desired) return false;
        settings.toggleNpcSoundEffects();
        settingsManager.saveActiveProfileSettings(player);
        ToggleFeedbackUtil.sendToggle(player, "NPC sound effects", settings.isNpcSoundEffectsEnabled());
        return true;
    }

    public boolean setAchievementSoundEffects(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isAchievementSoundEffectsEnabled() == desired) return false;
        settings.toggleAchievementSoundEffects();
        settingsManager.saveActiveProfileSettings(player);
        ToggleFeedbackUtil.sendToggle(player, "Achievement sound effects", settings.isAchievementSoundEffectsEnabled());
        return true;
    }

    public boolean setSkillPointReminder(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isSkillPointReminderEnabled() == desired) return false;
        settings.toggleSkillPointReminder();
        return true;
    }

    public boolean setFullInventoryTitle(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isFullInventoryTitleEnabled() == desired) return false;
        settings.toggleFullInventoryTitle();
        return true;
    }

    public boolean setTips(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isTipsEnabled() == desired) return false;
        settings.toggleTipsEnabled();
        ToggleFeedbackUtil.sendToggle(player, "Tips", settings.isTipsEnabled());
        return true;
    }

    public boolean setChatGames(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isChatGamesEnabled() == desired) return false;
        settings.toggleChatGamesEnabled();
        ToggleFeedbackUtil.sendToggle(player, "Chat games", settings.isChatGamesEnabled());
        return true;
    }

    public boolean setBoosterBossBar(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isBoosterBossBarEnabled() == desired) return false;
        settings.toggleBoosterBossBar();
        Main main = Main.getInstance();
        var boosterManager = main == null ? null : main.getBoosterManager();
        if (boosterManager != null) {
            boosterManager.refreshBossBar(player);
        }
        return true;
    }

    public boolean setQuestTrackingParticles(Player player, PlayerSettings settings, boolean desired) {
        if (settings.isQuestTrackingParticlesEnabled() == desired) return false;
        settings.toggleQuestTrackingParticles();
        return true;
    }

    public boolean setPlayerVisibility(Player player, PlayerSettings settings, PlayerVisibility desired) {
        if (desired == null || settings.getPlayerVisibility() == desired) return false;
        // PlayerSettings only exposes a cycle, so step until the target is reached.
        for (int i = 0; i < PlayerVisibility.values().length; i++) {
            settings.cyclePlayerVisibility();
            if (settings.getPlayerVisibility() == desired) break;
        }
        Main main = Main.getInstance();
        if (main != null && main.getPlayerVisibilityManager() != null) {
            main.getPlayerVisibilityManager().updatePlayer(player);
        }
        return true;
    }

    public boolean setLootPickupRarity(Player player, PlayerSettings settings, ItemRarity desired) {
        if (desired == null || settings.getLootPickupRarity() == desired) return false;
        ItemRarity[] rarities = settings.getLootPickupRarities();
        for (int i = 0; i < rarities.length; i++) {
            settings.cycleLootPickupRarity(true);
            if (settings.getLootPickupRarity() == desired) break;
        }
        return true;
    }

    public boolean setSpellInputMode(Player player, PlayerSettings settings, SpellInputMode desired) {
        if (desired == null || settings.getSpellInputMode() == desired) return false;
        settings.setSpellInputMode(desired);
        settingsManager.saveActiveProfileSettings(player);
        me.nakilex.levelplugin.spells.input.SpellInputHudManager.getInstance().sync(player);
        return true;
    }
}
