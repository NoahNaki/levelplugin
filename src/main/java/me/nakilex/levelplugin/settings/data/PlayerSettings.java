package me.nakilex.levelplugin.settings.data;

import me.nakilex.levelplugin.settings.data.PlayerVisibility;
import org.bukkit.configuration.ConfigurationSection;

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
    private boolean tipsEnabled = true;

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

    public boolean isTipsEnabled() {
        return tipsEnabled;
    }

    public void toggleTipsEnabled() {
        tipsEnabled = !tipsEnabled;
    }

    public void loadFromConfig(ConfigurationSection section) {
        if (section == null) {
            return;
        }

        dmgChat = section.getBoolean("damage_chat", dmgChat);
        dmgNumber = section.getBoolean("damage_number", dmgNumber);
        dropDetails = section.getBoolean("drop_details", dropDetails);
        dropDetailsChatEnabled = section.getBoolean("drop_details_chat", dropDetailsChatEnabled);
        partyGlow = section.getBoolean("party_glow", partyGlow);
        friendGlow = section.getBoolean("friend_glow", friendGlow);
        balancePublic = section.getBoolean("balance_public", balancePublic);
        autoSkipCutscenes = section.getBoolean("auto_skip_cutscenes", autoSkipCutscenes);
        autoSkipSongs = section.getBoolean("auto_skip_songs", autoSkipSongs);
        skillPointReminder = section.getBoolean("skill_point_reminder", skillPointReminder);
        tipsEnabled = section.getBoolean("tips_enabled", tipsEnabled);

        String vis = section.getString("player_visibility");
        if (vis != null) {
            for (PlayerVisibility option : PlayerVisibility.values()) {
                if (option.name().equalsIgnoreCase(vis)) {
                    playerVisibility = option;
                    break;
                }
            }
        }
    }

    public void saveToConfig(ConfigurationSection section) {
        if (section == null) {
            return;
        }

        section.set("damage_chat", dmgChat);
        section.set("damage_number", dmgNumber);
        section.set("drop_details", dropDetails);
        section.set("drop_details_chat", dropDetailsChatEnabled);
        section.set("party_glow", partyGlow);
        section.set("friend_glow", friendGlow);
        section.set("balance_public", balancePublic);
        section.set("player_visibility", playerVisibility.name());
        section.set("auto_skip_cutscenes", autoSkipCutscenes);
        section.set("auto_skip_songs", autoSkipSongs);
        section.set("skill_point_reminder", skillPointReminder);
        section.set("tips_enabled", tipsEnabled);
    }
}

