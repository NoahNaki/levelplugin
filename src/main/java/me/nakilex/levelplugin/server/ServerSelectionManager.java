package me.nakilex.levelplugin.server;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.listeners.StaticItemListener;
import me.nakilex.levelplugin.player.profile.ProfileEntryUtil;
import me.nakilex.levelplugin.utils.BetterHudUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.EntityTextDisplay;
import me.nakilex.levelplugin.utils.WorldExclusionUtil;
import me.nakilex.levelplugin.world.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import me.nakilex.levelplugin.npc.system.NpcApi;
import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.trait.SkinTrait;
import me.nakilex.levelplugin.npc.system.trait.LookCloseTrait;

import java.util.HashMap;
import java.util.Map;


public class ServerSelectionManager {
    private static final String SELECTOR_DATA_KEY = "server_selector";
    private static final double HOLOGRAM_TOP_OFFSET = 0.95;
    private static final double HOLOGRAM_MIDDLE_OFFSET = 0.7;
    private static final double HOLOGRAM_BOTTOM_OFFSET = 0.45;
    private static final SkinData ALPHA_SKIN = new SkinData(
            "alpha_selector",
            "ewogICJ0aW1lc3RhbXAiIDogMTczNjYzNTQ3NjUwOSwKICAicHJvZmlsZUlkIiA6ICI2NDU4Mjc0MjEyNDg0MDY0YTRkMDBlNDdjZWM4ZjcyZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJUaDNtMXMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNWY0NThjNTMxNzY4ZDhmM2VjZDljNDNkOWU0YTMyMGE0Y2E5ZmQ2ZmI3ODdhYmUzNGY1YmNhOTdjNTI4M2E4OSIKICAgIH0KICB9Cn0=",
            "MKkxhnGY0TsM20zEj2C8BhGoUfD8CelEgv9j+PctofPh4FmQ6CzfQIZ8b+N0noNPhV3Zz+kKDEaurlsWek/0sqvNs3lqPhI+6zlXGx0AAuKf0XnFEFRFpzRkHBk9RiCJ4aHenpu81bDAptkA+DGZBKddUMOSIQwPtZnDp1dG51C8iSIHg9Y8ka+y7czfUYZN0S84T8Mzew64u7ms0r4oS8bBuY1erXnCiaY3RZURkl0b2yY+JZeCXeiRFEwL5opxVrONktJ72dTqBK73Y4L+wVRJ4SeufVuRUTDCVoVpv4Ky9nkeOgpyWcxdJDODAs5tVsVObRJoZla5FAjKwHw3ta5C3D3JWnIiEXRG9L631TK6EiOvWlSTgRo7rxhNvVzBJ2SXrWDi5YVg3Io479C8oMBY4HLasK4grJ8UjfV93B7rO9IzCrqp+C4/E13jdJfcslD5i2ehbOl+1iuhOjBetYUa+WX8qH+lg6m2rMcNt6J0Jken00CQgnTlbLR9g8ljOnlbu3pfp5wVuVg7KJwZvKHoY2YQzPL7EswfzGFRN1/Pi9s8a6iX1rHo+xF1hCIEE669hdyMJdKM8hiqGLZy7dNTSwEc4iIHxENkbHVjaAyK+XeZrW2o02YNJcOXpisGxygItdxL/NkwvrcmBBwmTKMNumXlOi2QUdc+3ksGEAA="
    );
    private static final SkinData BUILD_SKIN = new SkinData(
            "build_selector",
            "ewogICJ0aW1lc3RhbXAiIDogMTc2NjYzNjMyMTUwMSwKICAicHJvZmlsZUlkIiA6ICJkYjZiYWRlN2NjMzI0MjM4YjU3OTQ4NzMxNTBkNjA1MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJRdWFudHVtQmxvY2tlciIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS85ZGRmOWFlODI4YTQ3ZTM2M2VmMzJiMTY3YWI5OWFkZjFhMjQ5ZDhhMGQ1MzI5ZDY2NzU0MjUwMTY2NzExMGE4IgogICAgfQogIH0KfQ==",
            "f+UDzGc+cgLQyW5c0N/q1R3OyBx7G4n3jorlgBN36TxcaPjazLJ/TnBBhIKkf2XwGsquTqhjHCnKIC402JQNZrxwZllz98b3EVEtbDvpBLDK1FIgoUvjs77kC3x2LjfwebdhokWinc4LOO6lNxh1QCd5+cTXflB/x7EZ8lfaoePeX4zAHmrJf/VFd3j+886Z2elO4s1xsGbbgGTWy1vI+JggdJIhRULJI/5A8BTzhoAW+ECFaVYP4FyzX6Ev1Bv4YOrVHCejwv78UwtoQ+Pxpn+IqkHdUhuNpm77m4Y2CwM10vJDVWedtUL6NhqbVvbTUFFEG6pD+sDWZZNPT3pCcZmF1nqXT1oNIakVArVKG8l+0/EXJ3oBmD3QTBf8YxfuHhB/U+VtnXsmm9y23t/lfdqgHfPdRRzCa7ta95+pNjNC3ZV9/Ww2Axrykt1s7DO6ZLY6IyNLtsUUC9eE52Y9YcxUynTydVLN4RIHsHI3q+TH6GQz2KOW6m8928FrmbJuZMfcoQEzG0VpyANTXlI4eX7vjcXACt+wKll5Vphf1jBPowk3RP/JcuMuw3IMEOpo0pA4B1Gsv9WFsSlGD1JqAmyS3Y70MN40KczicTBL84QhQRfXASa5tvHA19L2lqtbi0huUq4JsWsgan+8IhdzRPyuf9EyQXrRLlB+ZstqVNw="
    );

    private final Main plugin;
    private final WorldManager worldManager;
    private final ServerSelectorGUI selectorGUI;
    private final Map<String, SelectorNpc> selectorNpcs = new HashMap<>();
    private BukkitTask selectorTask;

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
        spawnHubSelectors();
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
        ProfileEntryUtil.saveActiveProfile(player);
        ProfileEntryUtil.clearActiveSlot(player);
        if (!teleportToWorld(player, hubWorld)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Hub world is unavailable.");
            return false;
        }
        ProfileEntryUtil.clearInventory(player);
        StaticItemListener.giveHubItems(player);
        BetterHudUtil.removeHud(player);
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
        ProfileEntryUtil.saveActiveProfile(player);
        ProfileEntryUtil.clearActiveSlot(player);
        if (!teleportToWorld(player, buildWorld)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Build world is unavailable.");
            return false;
        }
        ProfileEntryUtil.clearInventory(player);
        StaticItemListener.clearStaticItems(player);
        BetterHudUtil.removeHud(player);
        if (plugin.getScoreboardManager() != null) {
            plugin.getScoreboardManager().removeBoard(player);
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Connected to the build server.");
        return true;
    }

    public boolean handleSelectorClick(Player player, NPC npc) {
        if (player == null || npc == null) {
            return false;
        }
        String target = npc.data().get(SELECTOR_DATA_KEY);
        if (target == null || target.isBlank()) {
            return false;
        }
        if ("alpha".equalsIgnoreCase(target)) {
            sendToAlpha(player);
            return true;
        }
        if ("build".equalsIgnoreCase(target)) {
            sendToBuild(player);
            return true;
        }
        return false;
    }

    public void shutdown() {
        if (selectorTask != null) {
            selectorTask.cancel();
            selectorTask = null;
        }
        selectorNpcs.values().forEach(SelectorNpc::destroy);
        selectorNpcs.clear();
    }

    public boolean canAccessBuild(Player player) {
        if (player == null) {
            return false;
        }
        boolean hasPermission = buildPermission != null && !buildPermission.isBlank()
                && player.hasPermission(buildPermission);
        Integer weight = LuckPermsWeightUtil.getWeight(player);
        boolean hasWeight = weight != null && weight >= buildMinWeight;
        return hasPermission || hasWeight;
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

    private void spawnHubSelectors() {
        shutdown();
        for (NPC npc : NpcApi.getRegistry()) {
            String value = npc.data().get(SELECTOR_DATA_KEY);
            if (value != null && !value.isBlank()) {
                npc.destroy();
            }
        }
        worldManager.ensureWorldsLoaded(hubWorld);
        World world = Bukkit.getWorld(hubWorld);
        if (world == null) {
            plugin.getLogger().warning("[ServerSelection] Hub world '" + hubWorld + "' not loaded.");
            return;
        }
        selectorNpcs.put("alpha", createSelectorNpc("alpha", "Alpha Test",
                centeredLocation(world, -35, 67, 3)));
        selectorNpcs.put("build", createSelectorNpc("build", "Development Server",
                centeredLocation(world, -45, 67, 3)));
        updateSelectorHolograms();
        selectorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateSelectorHolograms, 20L, 40L);
    }

    private SelectorNpc createSelectorNpc(String key, String label, Location location) {
        NPC npc = NpcApi.getRegistry().createNPC(EntityType.PLAYER, "");
        npc.data().set(SELECTOR_DATA_KEY, key);
        applySelectorSkin(npc, key);
        npc.spawn(location);
        if (npc.getEntity() != null) {
            npc.getEntity().setCustomNameVisible(false);
        }
        LookCloseTrait lookClose = npc.getOrAddTrait(LookCloseTrait.class);
        lookClose.lookClose(true);
        SelectorNpc selector = new SelectorNpc(npc);
        selector.updateTop(org.bukkit.ChatColor.YELLOW + "CLICK TO JOIN");
        selector.updateMiddle(org.bukkit.ChatColor.AQUA + label);
        selector.updateBottom(org.bukkit.ChatColor.GRAY + "0 playing");
        return selector;
    }

    private void updateSelectorHolograms() {
        selectorNpcs.forEach((key, selector) -> {
            int count = "alpha".equalsIgnoreCase(key)
                    ? countPlayers(alphaWorld)
                    : countPlayers(buildWorld);
            selector.updateTop(org.bukkit.ChatColor.YELLOW + "CLICK TO JOIN");
            selector.updateMiddle(selector.getLabel());
            selector.updateBottom(String.valueOf(org.bukkit.ChatColor.GRAY) + count + " playing");
        });
    }

    private int countPlayers(String worldName) {
        if (worldName == null) {
            return 0;
        }
        return (int) Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getWorld() != null
                        && worldName.equalsIgnoreCase(player.getWorld().getName()))
                .count();
    }

    private boolean isWorld(World world, String target) {
        if (world == null || target == null) {
            return false;
        }
        return world.getName().equalsIgnoreCase(target);
    }

    private Location centeredLocation(World world, double x, double y, double z) {
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    private void applySelectorSkin(NPC npc, String key) {
        if (npc == null) {
            return;
        }
        SkinData skin = "alpha".equalsIgnoreCase(key) ? ALPHA_SKIN : BUILD_SKIN;
        SkinTrait trait = npc.getOrAddTrait(SkinTrait.class);
        trait.setSkinPersistent(skin.name(), skin.signature(), skin.value());
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

    private static final class SelectorNpc {
        private final NPC npc;
        private EntityTextDisplay top;
        private EntityTextDisplay middle;
        private EntityTextDisplay bottom;
        private String label = "";

        private SelectorNpc(NPC npc) {
            this.npc = npc;
        }

        private void updateTop(String text) {
            if (npc == null || npc.getEntity() == null) {
                return;
            }
            if (top == null) {
                top = new EntityTextDisplay(Main.getInstance(),
                        (org.bukkit.entity.LivingEntity) npc.getEntity(),
                        HOLOGRAM_TOP_OFFSET);
            }
            top.update(text);
        }

        private void updateBottom(String text) {
            if (npc == null || npc.getEntity() == null) {
                return;
            }
            if (bottom == null) {
                bottom = new EntityTextDisplay(Main.getInstance(),
                        (org.bukkit.entity.LivingEntity) npc.getEntity(),
                        HOLOGRAM_BOTTOM_OFFSET);
            }
            bottom.update(text);
        }

        private void updateMiddle(String text) {
            if (npc == null || npc.getEntity() == null) {
                return;
            }
            label = text;
            if (middle == null) {
                middle = new EntityTextDisplay(Main.getInstance(),
                        (org.bukkit.entity.LivingEntity) npc.getEntity(),
                        HOLOGRAM_MIDDLE_OFFSET);
            }
            middle.update(text);
        }

        private String getLabel() {
            return label;
        }

        private void destroy() {
            if (top != null) {
                top.remove();
                top = null;
            }
            if (middle != null) {
                middle.remove();
                middle = null;
            }
            if (bottom != null) {
                bottom.remove();
                bottom = null;
            }
            if (npc != null) {
                npc.destroy();
            }
        }
    }

    private record SkinData(String name, String value, String signature) {}
}
