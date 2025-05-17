package me.nakilex.levelplugin.settings.data;

public class PlayerSettings {

    private boolean dmgChat     = false;
    private boolean dmgNumber   = false;
    private boolean dropDetails = true;   // default ON
    private boolean dropDetailsChatEnabled = false;

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

    /** New: drop‐details holograms */
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
}
