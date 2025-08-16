package me.nakilex.levelplugin.settings.data;

import me.nakilex.levelplugin.settings.data.PlayerVisibility;

public class PlayerSettings {

    private boolean dmgChat     = false;
    private boolean dmgNumber   = false;
    private boolean dropDetails = true;
    private boolean dropDetailsChatEnabled = true;
    private boolean partyGlow = true;
    private boolean friendGlow = true;
    private boolean balancePublic = true;
    private PlayerVisibility playerVisibility = PlayerVisibility.SHOW_ALL;
    private boolean autoSkipCutscenes = false;

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

}

