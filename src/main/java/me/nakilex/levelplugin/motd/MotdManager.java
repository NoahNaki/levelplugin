package me.nakilex.levelplugin.motd;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MotdManager implements Listener {
    private final Main plugin;
    private FileConfiguration config;
    private File configFile;

    private static final Pattern GRADIENT_PATTERN =
            Pattern.compile("<gradient:(#[0-9a-fA-F]{6}):(#[0-9a-fA-F]{6})>(.*?)</gradient>");

    public MotdManager(Main plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        configFile = new File(plugin.getDataFolder(), "motd.yml");
        if (!configFile.exists()) {
            plugin.saveResource("motd.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    @EventHandler
    public void onPing(ServerListPingEvent event) {
        event.setMotd(getLine1() + "\n" + getLine2());
    }

    public String getLine1() {
        return format(config.getString("line1", ""));
    }

    public String getLine2() {
        return format(config.getString("line2", ""));
    }

    private String format(String line) {
        if (line == null) return "";
        String replaced = line.replace("{version}", plugin.getDescription().getVersion());
        String colored = ChatColor.translateAlternateColorCodes('&', applyGradient(replaced));
        return center(colored);
    }

    private String center(String line) {
        int length = ChatColor.stripColor(line).length();
        int padding = Math.max(0, (60 - length) / 2); // rough centering
        return " ".repeat(padding) + line;
    }

    private String applyGradient(String input) {
        Matcher m = GRADIENT_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String start = m.group(1);
            String end = m.group(2);
            String text = m.group(3);
            m.appendReplacement(sb, Matcher.quoteReplacement(gradient(start, end, text)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String gradient(String startHex, String endHex, String text) {
        java.awt.Color start = java.awt.Color.decode(startHex);
        java.awt.Color end = java.awt.Color.decode(endHex);
        StringBuilder out = new StringBuilder();
        int len = text.length();
        for (int i = 0; i < len; i++) {
            float ratio = len == 1 ? 0 : (float) i / (len - 1);
            int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * ratio);
            int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * ratio);
            int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * ratio);
            String color = String.format("#%02X%02X%02X", r, g, b);
            out.append(ChatColor.of(color)).append(text.charAt(i));
        }
        return out.toString();
    }
}
