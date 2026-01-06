package me.nakilex.levelplugin.server;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.listeners.StaticItemListener;
import me.nakilex.levelplugin.player.profile.ProfileEntryUtil;
import me.nakilex.levelplugin.utils.BetterHudUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.WorldExclusionUtil;
import me.nakilex.levelplugin.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;


public class ServerSelectionManager {
    private final Main plugin;
    private final WorldManager worldManager;
    private final ServerSelectorGUI selectorGUI;

    private String hubWorld;
    private String alphaWorld;
    private String buildWorld;
    private String buildPermission;
    private int buildMinWeight;

    public ServerSelectionManager(Main plugin) {
        this.plugin = plugin;
        this.worldManager = plugin.getWorldManager();
        this.selectorGUI = new ServerSelectorGUI(this);
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getCustomConfig();
        hubWorld = getConfigValue(config, "server.hub-world", "hub");
        alphaWorld = getConfigValue(config, "server.alpha-world", "world");
        buildWorld = getConfigValue(config, "server.build-world", "flatland");
        buildPermission = getConfigValue(config, "server.build-permission", "group.staff");
        buildMinWeight = config != null ? config.getInt("server.build-min-weight", 51) : 51;
    }

    public ServerSelectorGUI getSelectorGUI() {
        return selectorGUI;
    }

    public void handleJoin(Player player) {
        if (player == null) {
            return;
        }
        sendToHub(player, false);
    }

    public void openSelector(Player player) {
        selectorGUI.open(player);
    }

    public boolean sendToHub(Player player, boolean notify) {
        if (player == null) {
            return false;
        }
        if (!teleportToWorld(player, hubWorld)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Hub world is unavailable.");
            return false;
        }
        StaticItemListener.giveHubItems(player);
        if (notify) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Connected to the hub.");
        }
        return true;
    }

    public boolean sendToAlpha(Player player) {
        if (player == null) {
            return false;
        }
        if (!teleportToAlphaLobby(player)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Alpha world is unavailable.");
            return false;
        }
        if (WorldExclusionUtil.isExcluded(player)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Alpha is currently marked as excluded from LevelPlugin features.");
            return true;
        }
        ProfileEntryUtil.handleProfileEntry(player);
        return true;
    }

    public boolean sendToBuild(Player player) {
        if (player == null) {
            return false;
        }
        if (!canAccessBuild(player)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "You do not have access to the build server.");
            return false;
        }
        if (!teleportToWorld(player, buildWorld)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Build world is unavailable.");
            return false;
        }
        StaticItemListener.clearStaticItems(player);
        BetterHudUtil.removeHud(player);
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().removeBoard(player);
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Connected to the build server.");
        return true;
    }

    public boolean canAccessBuild(Player player) {
        if (player == null) {
            return false;
        }
        if (buildPermission != null && !buildPermission.isBlank()
                && player.hasPermission(buildPermission)) {
            return true;
        }
        Integer weight = LuckPermsWeightUtil.getWeight(player);
        return weight != null && weight >= buildMinWeight;
    }

    public boolean isHubWorld(World world) {
        return isWorld(world, hubWorld);
    }

    public boolean isAlphaWorld(World world) {
        return isWorld(world, alphaWorld);
    }

    public boolean isBuildWorld(World world) {
        return isWorld(world, buildWorld);
    }

    public String getHubWorldName() {
        return hubWorld;
    }

    public String getAlphaWorldName() {
        return alphaWorld;
    }

    public String getBuildWorldName() {
        return buildWorld;
    }

    public String getBuildPermission() {
        return buildPermission;
    }

    public int getBuildMinWeight() {
        return buildMinWeight;
    }

    private boolean teleportToWorld(Player player, String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        if (worldManager != null) {
            worldManager.ensureWorldsLoaded(worldName);
        }
        if (worldManager != null && worldManager.teleport(player, worldName)) {
            return true;
        }
        World fallback = Bukkit.getWorld(worldName);
        if (fallback == null) {
            return false;
        }
        player.teleport(fallback.getSpawnLocation());
        return true;
    }

    private boolean teleportToAlphaLobby(Player player) {
        if (player == null) {
            return false;
        }
        if (alphaWorld == null || alphaWorld.isBlank()) {
            return false;
        }
        if ("world".equalsIgnoreCase(alphaWorld)) {
            World lobbyWorld = Bukkit.getWorld(alphaWorld);
            if (lobbyWorld == null) {
                return false;
            }
            player.teleport(new Location(lobbyWorld, 217, 6, 80));
            return true;
        }
        return teleportToWorld(player, alphaWorld);
    }

    private boolean isWorld(World world, String target) {
        if (world == null || target == null) {
            return false;
        }
        return world.getName().equalsIgnoreCase(target);
    }

    private String getConfigValue(FileConfiguration config, String key, String fallback) {
        if (config == null) {
            return fallback;
        }
        String value = config.getString(key, fallback);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
