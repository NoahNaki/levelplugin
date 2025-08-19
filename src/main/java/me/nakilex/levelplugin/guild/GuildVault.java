package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.storage.events.StorageEvents;
import org.bukkit.entity.Player;

public class GuildVault {
    private final String guildName;
    private final GuildVaultGUI storageGUI;

    public GuildVault(String guildName, StorageEvents events) {
        this.guildName = guildName;
        this.storageGUI = new GuildVaultGUI(guildName, events);
        this.storageGUI.loadFromDisk();
    }

    public void open(Player player) {
        storageGUI.open(player);
    }

    public void save() {
        storageGUI.saveToDisk();
    }

    public String getGuildName() {
        return guildName;
    }

    public GuildVaultGUI getStorageGUI() {
        return storageGUI;
    }
}
