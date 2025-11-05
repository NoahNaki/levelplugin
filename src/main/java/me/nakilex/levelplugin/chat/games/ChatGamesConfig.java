package me.nakilex.levelplugin.chat.games;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import me.nakilex.levelplugin.Main;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/** Loads the chat_games.yml resource and exposes typed accessors. */
public class ChatGamesConfig {

    private final Main plugin;
    private final File file;
    private FileConfiguration config;

    public ChatGamesConfig(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "chat_games.yml");
        ensureExists();
        reload();
    }

    private void ensureExists() {
        if (!file.exists()) {
            try {
                plugin.saveResource("chat_games.yml", false);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().log(Level.SEVERE, "chat_games.yml missing from jar resources", ex);
                try {
                    if (file.createNewFile()) {
                        plugin.getLogger().warning("Created empty chat_games.yml; games may not function until filled.");
                    }
                } catch (IOException ioException) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to create chat_games.yml", ioException);
                }
            }
        }
    }

    public void reload() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public List<String> getScrambleWords() {
        return config != null ? config.getStringList("word-scramble.words") : Collections.emptyList();
    }

    public List<String> getTypeRacerPhrases() {
        return config != null ? config.getStringList("type-racer.phrases") : Collections.emptyList();
    }

    public int getMathMinimum() {
        return config != null ? config.getInt("math-challenge.min", 10) : 10;
    }

    public int getMathMaximum() {
        return config != null ? config.getInt("math-challenge.max", 99) : 99;
    }

    public List<String> getMathOperations() {
        return config != null ? config.getStringList("math-challenge.operations") : Collections.emptyList();
    }
}
