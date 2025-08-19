package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.storage.events.StorageEvents;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GuildVaultManager {
    private final Map<String, GuildVault> vaults = new HashMap<>();
    private final StorageEvents events;
    private final GuildMemberGUI memberGUI;

    public GuildVaultManager(StorageEvents events, GuildMemberGUI memberGUI) {
        this.events = events;
        this.memberGUI = memberGUI;
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
            Guild existing = GuildManager.getInstance().getGuildIgnoreCase(guild);
            String proper = existing != null ? existing.getName() : guild;
            vaults.put(guild.toLowerCase(), new GuildVault(proper, events, memberGUI));
        }
    }

    public GuildVault getVault(String guildName) {
        String key = guildName.toLowerCase();
        Guild existing = GuildManager.getInstance().getGuildIgnoreCase(guildName);
        String proper = existing != null ? existing.getName() : guildName;
        return vaults.compute(key, (k, v) -> {
            if (v == null || !v.getGuildName().equals(proper)) {
                return new GuildVault(proper, events, memberGUI);
            }
            return v;
        });
    }

    public void saveAll() {
        for (GuildVault v : vaults.values()) {
            v.save();
        }
    }
}
