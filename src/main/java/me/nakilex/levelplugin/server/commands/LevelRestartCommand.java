package me.nakilex.levelplugin.server.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Console/RCON-safe command that performs a guarded Paper/Spigot restart.
 */
public class LevelRestartCommand implements CommandExecutor {

    public static final String PERMISSION = "levelplugin.admin.restart";

    private static final String PLAYER_KICK_MESSAGE = "Server restarting for plugin update. Rejoin in a moment.";
    private static final String RESTART_WARNING = ChatColor.RED + "Server restarting for plugin update...";
    private static final long SAVE_DELAY_TICKS = 60L;
    private static final long KICK_DELAY_TICKS = 40L;
    private static final long RESTART_DELAY_TICKS = 40L;

    private final Main plugin;
    private final AtomicBoolean restartInProgress = new AtomicBoolean(false);

    public LevelRestartCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "You do not have permission to use this command.");
            return true;
        }

        String trigger = describeSender(sender);
        plugin.getLogger().info("levelrestart triggered by " + trigger + ".");

        RestartSupport support = checkRestartSupport();
        if (!support.supported()) {
            plugin.getLogger().severe("levelrestart aborted safely: " + support.reason());
            ChatMessageUtil.send(sender, MessageType.ERROR,
                    "Restart is not supported safely on this server: " + support.reason());
            ChatMessageUtil.send(sender, MessageType.WARNING,
                    "No players were kicked and the server was not stopped. Configure settings.restart-script in spigot.yml.");
            return true;
        }

        if (!restartInProgress.compareAndSet(false, true)) {
            ChatMessageUtil.send(sender, MessageType.WARNING, "A LevelPlugin restart is already in progress.");
            plugin.getLogger().warning("Duplicate levelrestart request ignored. Triggered by " + trigger + ".");
            return true;
        }

        plugin.getLogger().info("Using Paper/Spigot restart path with restart-script: " + support.restartScript());
        ChatMessageUtil.send(sender, MessageType.SUCCESS, "Starting safe restart...");
        Bukkit.broadcastMessage(RESTART_WARNING);

        Bukkit.getScheduler().runTaskLater(plugin, () -> saveAndKickPlayers(sender), SAVE_DELAY_TICKS);
        return true;
    }

    private void saveAndKickPlayers(CommandSender sender) {
        plugin.getLogger().info("levelrestart: saving player data and world data before restart.");
        Bukkit.savePlayers();
        for (World world : Bukkit.getWorlds()) {
            world.save();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(PLAYER_KICK_MESSAGE);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> flushWorldSave(sender), KICK_DELAY_TICKS);
    }

    private void flushWorldSave(CommandSender sender) {
        plugin.getLogger().info("levelrestart: dispatching save-all flush before restart.");
        boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all flush");
        if (!dispatched) {
            plugin.getLogger().warning("levelrestart: save-all flush command was not accepted; continuing after direct world saves.");
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> restartServer(sender), RESTART_DELAY_TICKS);
    }

    private void restartServer(CommandSender sender) {
        try {
            plugin.getLogger().info("levelrestart: invoking Bukkit.spigot().restart().");
            Bukkit.spigot().restart();
        } catch (Throwable throwable) {
            restartInProgress.set(false);
            plugin.getLogger().log(Level.SEVERE,
                    "levelrestart: Paper/Spigot restart failed. Failing safely without /stop or fallback shutdown.",
                    throwable);
            ChatMessageUtil.send(sender, MessageType.ERROR,
                    "Restart failed; the server was left running instead of using an unsafe fallback shutdown.");
        }
    }

    private RestartSupport checkRestartSupport() {
        try {
            Bukkit.spigot().getClass().getMethod("restart");
        } catch (NoSuchMethodException exception) {
            return RestartSupport.unsupported("Bukkit.spigot().restart() is unavailable on this server implementation.");
        } catch (SecurityException exception) {
            return RestartSupport.unsupported("Access to Bukkit.spigot().restart() is blocked by the runtime.");
        }

        File spigotConfig = new File("spigot.yml");
        if (!spigotConfig.isFile()) {
            return RestartSupport.unsupported("spigot.yml was not found in the server working directory.");
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(spigotConfig);
        String restartScript = config.getString("settings.restart-script", "").trim();
        if (restartScript.isEmpty()) {
            return RestartSupport.unsupported("settings.restart-script is not configured in spigot.yml.");
        }

        String normalized = restartScript.toLowerCase(Locale.ROOT);
        if (normalized.equals("none") || normalized.equals("false") || normalized.equals("disabled")) {
            return RestartSupport.unsupported("settings.restart-script is disabled in spigot.yml.");
        }

        File scriptFile = new File(restartScript);
        if (!scriptFile.isAbsolute()) {
            scriptFile = new File(spigotConfig.getAbsoluteFile().getParentFile(), restartScript);
        }
        if (!scriptFile.isFile()) {
            return RestartSupport.unsupported("configured restart script does not exist: " + restartScript);
        }
        if (!isWindows() && !scriptFile.canExecute()) {
            return RestartSupport.unsupported("configured restart script is not executable: " + restartScript);
        }

        return RestartSupport.supported(restartScript);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private String describeSender(CommandSender sender) {
        return sender.getName() + " (" + sender.getClass().getSimpleName() + ")";
    }

    private record RestartSupport(boolean supported, String restartScript, String reason) {
        private static RestartSupport supported(String restartScript) {
            return new RestartSupport(true, restartScript, "");
        }

        private static RestartSupport unsupported(String reason) {
            return new RestartSupport(false, "", reason);
        }
    }
}
