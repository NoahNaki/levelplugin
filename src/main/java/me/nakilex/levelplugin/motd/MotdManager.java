package me.nakilex.levelplugin.motd;

import me.nakilex.levelplugin.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MotdManager implements Listener {
    private final Main plugin;
    private FileConfiguration config;
    private File configFile;

    private static final Pattern GRADIENT_PATTERN =
            Pattern.compile("<gradient:((?:#[0-9a-fA-F]{6}:?)+)>(.*?)</gradient>");

    private static final Pattern HEX_COLOR_PATTERN =
            Pattern.compile("<#([0-9a-fA-F]{6})>");

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
        me.nakilex.levelplugin.maintenance.MaintenanceManager mm = plugin.getMaintenanceManager();
        if (mm != null && mm.isEnabled()) {
            String reason = mm.getReason();
            String line = ChatColor.RED + "Maintenance";
            if (!reason.isEmpty()) {
                line += ChatColor.GRAY + ": " + ChatColor.WHITE + reason;
            }
            event.setMotd(line);
        } else {
            event.setMotd(getLine1() + "\n" + getLine2());
        }
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
        replaced = replaced.replace("{online}", String.valueOf(plugin.getServer().getOnlinePlayers().size()));
        replaced = replaced.replace("{max_players}", String.valueOf(plugin.getServer().getMaxPlayers()));

        // allow using the § symbol directly in config
        replaced = replaced.replace('§', '&');

        replaced = replaced.replace("<bold>", ChatColor.BOLD.toString())
                           .replace("</bold>", ChatColor.RESET.toString());

        String withGradients = applyGradients(replaced);
        String withHex = applyHexColors(withGradients);

        String colored = ChatColor.translateAlternateColorCodes('&', withHex);
        return center(colored);
    }

    private String center(String line) {
        int length = ChatColor.stripColor(line).length();
        int padding = Math.max(0, (60 - length) / 2); // rough centering
        return " ".repeat(padding) + line;
    }

    private String applyGradients(String input) {
        Matcher m = GRADIENT_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String[] colors = m.group(1).split(":");
            String text = m.group(2);
            m.appendReplacement(sb, Matcher.quoteReplacement(applyGradient(colors, text)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String applyHexColors(String input) {
        Matcher m = HEX_COLOR_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String color = m.group(1);
            m.appendReplacement(sb, Matcher.quoteReplacement(ChatColor.valueOf("#" + color).toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String applyGradient(String[] colors, String text) {
        if (colors.length < 2) return text;
        StringBuilder out = new StringBuilder();
        int total = text.length();
        int segments = colors.length - 1;
        int index = 0;
        for (int s = 0; s < segments; s++) {
            java.awt.Color start = java.awt.Color.decode(colors[s]);
            java.awt.Color end = java.awt.Color.decode(colors[s + 1]);

            int segLength = total / segments + (s < total % segments ? 1 : 0);
            for (int i = 0; i < segLength && index < total; i++, index++) {
                float ratio = segLength == 1 ? 0f : (float) i / (segLength - 1);
                int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * ratio);
                int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * ratio);
                int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * ratio);
                String hex = String.format("#%02X%02X%02X", r, g, b);
                out.append(ChatColor.valueOf(hex)).append(text.charAt(index));
            }
        }
        // append leftover characters without gradient
        if (index < total) {
            out.append(text.substring(index));
        }
        return out.toString();
    }
}
