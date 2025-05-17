package me.nakilex.levelplugin.settings.data;

public class PlayerSettings {

    private boolean dmgChat     = false;
    private boolean dmgNumber   = false;
    private boolean dropDetails = true;
    private boolean dropDetailsChatEnabled = true;

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
}
