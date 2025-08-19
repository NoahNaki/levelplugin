package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.storage.events.StorageEvents;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GuildVaultManager {
    private final Map<String, GuildVault> vaults = new HashMap<>();
    private final StorageEvents events;

    public GuildVaultManager(StorageEvents events) {
        this.events = events;
        loadExisting();
    }

    private void loadExisting() {
        File folder = new File(Bukkit.getPluginManager().getPlugin("LevelPlugin").getDataFolder(), "guildvault");
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            String name = file.getName();
            if (!name.startsWith("guild_") || !name.endsWith(".yml")) continue;
            String guild = name.substring("guild_".length(), name.length() - 4);
            vaults.put(guild.toLowerCase(), new GuildVault(guild, events));
        }
    }

    public GuildVault getVault(String guildName) {
        return vaults.computeIfAbsent(guildName.toLowerCase(), g -> new GuildVault(g, events));
    }

    public void saveAll() {
        for (GuildVault v : vaults.values()) {
            v.save();
        }
    }
}
