package me.nakilex.levelplugin.cursormenu.display;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Creates simple text displays based on configuration.
 */
public class TextDisplayManager extends AbstractDisplayManager<TextDisplay> {
    private final Map<String, String> texts = new HashMap<>();
    private final File configFile;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public TextDisplayManager(JavaPlugin plugin, File configFile) {
        super(plugin);
        this.configFile = configFile;
        reloadConfig();
    }

    public void reloadConfig() {
        texts.clear();
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        if (cfg.getConfigurationSection("texts") == null) return;
        for (String id : cfg.getConfigurationSection("texts").getKeys(false)) {
            String raw = cfg.getString("texts." + id + ".text", "");
            texts.put(id, raw);
        }
    }

    public void showText(Player player, String id, Location loc) {
        String raw = texts.get(id);
        if (raw == null) return;
        hide(player);
        TextDisplay display = loc.getWorld().spawn(loc, TextDisplay.class, d -> {
            d.text(miniMessage.deserialize(raw));
        });
        playerDisplays.put(player.getUniqueId(), display);
    }

    public void hideText(Player player) {
        hide(player);
    }

    public Set<String> getAllTextIds() {
        return texts.keySet();
    }
}
