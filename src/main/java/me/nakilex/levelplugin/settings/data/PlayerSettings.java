package me.nakilex.levelplugin.settings.data;

public class PlayerSettings {

    private boolean dmgChat = false;
    private boolean dmgNumber = false;

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
}
