package me.nakilex.levelplugin.player.woodcutting.listeners;

import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.events.custom_block.NexoBlockBreakEvent;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.ToolDiscipline;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.items.tools.WoodcuttingToolEnchant;
import me.nakilex.levelplugin.player.woodcutting.config.WoodcuttingConfig;
import me.nakilex.levelplugin.player.woodcutting.managers.WoodcuttingManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.FullInventoryListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles woodcutting on configured Nexo custom blocks by temporarily hiding
 * nodes and restoring them after a cooldown. Hidden node state is persisted so
 * restart/chunk-unload scenarios are safe.
 */
public class WoodcuttingNodeListener implements Listener {

    private final Main plugin;
    private final WoodcuttingManager woodcuttingManager;
    private final WoodcuttingConfig config;
    private final File stateFile;
    private org.bukkit.configuration.file.FileConfiguration state;
    private static final int CLEAVING_ADJACENT_LIMIT = 3;

    private final Map<String, HiddenNodeState> hiddenNodes = new HashMap<>();
    private final Map<UUID, Map<String, NodeProgressState>> progressByPlayer = new HashMap<>();
    private final Map<String, BukkitTask> respawnTasks = new HashMap<>();
    private BukkitTask heartbeatTask;

    public WoodcuttingNodeListener(Main plugin,
                                   WoodcuttingManager woodcuttingManager,
                                   WoodcuttingConfig config) {
        this.plugin = plugin;
        this.woodcuttingManager = woodcuttingManager;
        this.config = config;
        this.stateFile = new File(plugin.getDataFolder(), "woodcutting_state.yml");
        this.state = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(stateFile);
        loadState();
        startHeartbeat();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onNexoBlockBreak(NexoBlockBreakEvent event) {
        if (event.getPlayer() == null || event.getBlock() == null || event.getMechanic() == null) {
            return;
        }
        event.setCancelled(true);
        handleNodeHit(event.getPlayer(), event.getBlock().getLocation(), event.getMechanic().getItemID());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onNodeInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null || !NexoBlocks.isCustomBlock(event.getClickedBlock())) {
            return;
        }
        var mechanic = NexoBlocks.customBlockMechanic(event.getClickedBlock());
        if (mechanic == null) {
            return;
        }
        event.setCancelled(true);
        handleNodeHit(event.getPlayer(), event.getClickedBlock().getLocation(), mechanic.getItemID());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        String world = event.getWorld().getName();
        int cx = event.getChunk().getX();
        int cz = event.getChunk().getZ();
        long now = System.currentTimeMillis();
        for (HiddenNodeState hidden : hiddenNodes.values()) {
            if (!hidden.worldName.equalsIgnoreCase(world)) continue;
            if ((hidden.x >> 4) != cx || (hidden.z >> 4) != cz) continue;
            if (hidden.respawnAtMillis > now) continue;
            tryRespawn(hidden);
        }
    }

    public void shutdown() {
        for (BukkitTask task : respawnTasks.values()) {
            task.cancel();
        }
        respawnTasks.clear();
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
            heartbeatTask = null;
        }
        for (UUID playerId : progressByPlayer.keySet().toArray(new UUID[0])) {
            Map<String, NodeProgressState> states = progressByPlayer.get(playerId);
            if (states == null) continue;
            for (String nodeKey : states.keySet().toArray(new String[0])) {
                clearNodeProgress(playerId, nodeKey);
            }
        }
        saveState();
    }

    private void handleNodeHit(Player player, Location location, String blockId) {
        if (player == null || location == null || blockId == null || blockId.isBlank()) {
            return;
        }
        String normalizedId = blockId.toLowerCase(Locale.ROOT);
        if (!config.getNodeIds().contains(normalizedId)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    ChatColor.RED + "That custom block is not a configured woodcutting node.");
            return;
        }

        String nodeKey = key(location);
        if (hiddenNodes.containsKey(nodeKey)) {
            return;
        }

        CustomTool tool = ToolManager.getInstance().getTool(player.getInventory().getItemInMainHand());
        if (!isValidTool(player, tool)) {
            return;
        }

        int durability = config.getNodeDurability(normalizedId);
        double toolDamage = Math.max(1.0D, tool.getTier().getHarvestYield());
        NodeProgressState progress = progressByPlayer
                .computeIfAbsent(player.getUniqueId(), id -> new HashMap<>())
                .computeIfAbsent(nodeKey, k -> new NodeProgressState(
                        location.getWorld() != null ? location.getWorld().getName() : "world",
                        location.getBlockX(),
                        location.getBlockY(),
                        location.getBlockZ(),
                        durability));

        progress.damage += toolDamage;
        updateProgressDisplay(player, progress);

        if (progress.damage < progress.maxDurability) {
            return;
        }

        clearNodeProgress(player.getUniqueId(), nodeKey);
        harvestNode(player, location, normalizedId, true, new HashSet<>());
    }

    private boolean isValidTool(Player player, CustomTool tool) {
        if (tool == null || tool.getDiscipline() != ToolDiscipline.WOODCUTTING) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    ChatColor.RED + "You need an axe to cut this node.");
            return false;
        }
        if (!ToolManager.getInstance().meetsLevelRequirement(player, tool)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    ChatColor.RED + "You need Woodcutting level " + tool.getTier().getLevelRequirement() + " to use this axe.");
            return false;
        }
        return true;
    }

    private void harvestNode(Player player,
                             Location location,
                             String normalizedId,
                             boolean allowCleaving,
                             Set<String> visited) {
        String nodeKey = key(location);
        if (hiddenNodes.containsKey(nodeKey) || visited.contains(nodeKey)) {
            return;
        }
        visited.add(nodeKey);

        NexoBlocks.remove(location, player, false);

        WoodcuttingToolEnchant enchant = ToolManager.getInstance().getWoodcuttingEnchant(player.getInventory().getItemInMainHand());
        int xp = config.getBaseXp();
        if (enchant == WoodcuttingToolEnchant.WISDOM && ThreadLocalRandom.current().nextDouble() < 0.20D) {
            xp = (int) Math.ceil(xp * 1.5D);
        }
        woodcuttingManager.addXP(player, xp);
        giveDrops(player, enchant);

        long respawnAt = System.currentTimeMillis() + (config.getRespawnSeconds() * 1000L);
        HiddenNodeState hidden = new HiddenNodeState(normalizedId,
                location.getWorld() != null ? location.getWorld().getName() : "world",
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                respawnAt);
        hiddenNodes.put(nodeKey, hidden);
        clearNodeProgressForAllPlayers(nodeKey);
        saveState();
        scheduleRespawn(hidden);

        if (allowCleaving && enchant == WoodcuttingToolEnchant.CLEAVING) {
            harvestAdjacent(player, location, normalizedId, visited);
        }
    }

    private void harvestAdjacent(Player player, Location origin, String nodeId, Set<String> visited) {
        int harvested = 0;
        World world = origin.getWorld();
        if (world == null) {
            return;
        }

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    if (harvested >= CLEAVING_ADJACENT_LIMIT) return;

                    Location target = origin.clone().add(x, y, z);
                    if (!NexoBlocks.isCustomBlock(target.getBlock())) continue;
                    var mechanic = NexoBlocks.customBlockMechanic(target.getBlock());
                    if (mechanic == null || !nodeId.equalsIgnoreCase(mechanic.getItemID())) continue;
                    if (hiddenNodes.containsKey(key(target))) continue;

                    harvestNode(player, target, nodeId, false, visited);
                    harvested++;
                }
            }
        }
    }

    private void updateProgressDisplay(Player player, NodeProgressState progress) {
        World world = Bukkit.getWorld(progress.worldName);
        if (world == null) {
            return;
        }
        if (progress.display == null || progress.display.isDead()) {
            Location displayLocation = new Location(world, progress.x + 0.5, progress.y + 1.35, progress.z + 0.5);
            progress.display = (TextDisplay) world.spawnEntity(displayLocation, EntityType.TEXT_DISPLAY);
            progress.display.setBillboard(Display.Billboard.CENTER);
            progress.display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            progress.display.setShadowRadius(0f);
            progress.display.setShadowStrength(0f);
            progress.display.setVisibleByDefault(false);
            player.showEntity(plugin, progress.display);
        }
        progress.display.setText(buildDurabilityBar(progress));
    }

    private String buildDurabilityBar(NodeProgressState progress) {
        int segments = config.getHpBarSegments();
        double remaining = Math.max(0.0D, progress.maxDurability - progress.damage);
        double ratio = progress.maxDurability <= 0 ? 0.0D : (remaining / progress.maxDurability);
        int filled = (int) Math.ceil(ratio * segments);
        filled = Math.max(0, Math.min(segments, filled));
        int empty = segments - filled;
        String color = getDurabilityColor(ratio);
        return color + "│".repeat(filled) + ChatColor.GRAY + "│".repeat(empty);
    }

    private String getDurabilityColor(double ratio) {
        if (ratio >= 1.0D) {
            return ChatColor.GREEN.toString();
        }
        if (ratio >= 0.75D) {
            return ChatColor.YELLOW.toString();
        }
        if (ratio >= 0.50D) {
            return ChatColor.GOLD.toString();
        }
        return ChatColor.RED.toString();
    }

    private void clearNodeProgress(UUID playerId, String nodeKey) {
        Map<String, NodeProgressState> states = progressByPlayer.get(playerId);
        if (states == null) {
            return;
        }
        NodeProgressState progress = states.remove(nodeKey);
        if (states.isEmpty()) {
            progressByPlayer.remove(playerId);
        }
        if (progress == null || progress.display == null || progress.display.isDead()) {
            return;
        }
        progress.display.remove();
    }

    private void clearNodeProgressForAllPlayers(String nodeKey) {
        for (UUID playerId : progressByPlayer.keySet().toArray(new UUID[0])) {
            clearNodeProgress(playerId, nodeKey);
        }
    }

    private void giveDrops(Player player, WoodcuttingToolEnchant enchant) {
        int min = config.getDropAmountMin();
        int max = config.getDropAmountMax();
        int amount = max > min ? ThreadLocalRandom.current().nextInt(min, max + 1) : min;
        if (enchant == WoodcuttingToolEnchant.IRONWOOD && ThreadLocalRandom.current().nextDouble() < 0.25D) {
            amount += 1;
        }

        ItemStack drop = new ItemStack(config.getDropMaterial(), amount);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
        if (!overflow.isEmpty()) {
            FullInventoryListener.sendFullInventoryTitle(player, Main.getInstance().getSettingsManager());
            overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }

    private void scheduleRespawn(HiddenNodeState hidden) {
        String key = hidden.key();
        BukkitTask existing = respawnTasks.remove(key);
        if (existing != null) {
            existing.cancel();
        }

        long delayMs = Math.max(1L, hidden.respawnAtMillis - System.currentTimeMillis());
        long delayTicks = Math.max(1L, delayMs / 50L);
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            respawnTasks.remove(key);
            tryRespawn(hidden);
        }, delayTicks);
        respawnTasks.put(key, task);
    }

    private void startHeartbeat() {
        heartbeatTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (HiddenNodeState hidden : hiddenNodes.values()) {
                if (hidden.respawnAtMillis <= now) {
                    tryRespawn(hidden);
                }
            }
        }, 20L * 30, 20L * 30);
    }

    private void tryRespawn(HiddenNodeState hidden) {
        World world = Bukkit.getWorld(hidden.worldName);
        if (world == null) {
            return;
        }

        int chunkX = hidden.x >> 4;
        int chunkZ = hidden.z >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }

        Location location = new Location(world, hidden.x, hidden.y, hidden.z);
        if (NexoBlocks.isCustomBlock(location.getBlock())) {
            com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic mechanic = NexoBlocks.customBlockMechanic(location.getBlock());
            if (mechanic != null && hidden.blockId.equalsIgnoreCase(mechanic.getItemID())) {
                hiddenNodes.remove(hidden.key());
                respawnTasks.remove(hidden.key());
                clearNodeProgressForAllPlayers(hidden.key());
                saveState();
                return;
            }
        }

        NexoBlocks.place(hidden.blockId, location);
        hiddenNodes.remove(hidden.key());
        respawnTasks.remove(hidden.key());
        clearNodeProgressForAllPlayers(hidden.key());
        saveState();
    }

    private void loadState() {
        hiddenNodes.clear();
        if (!state.isConfigurationSection("hidden")) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String key : state.getConfigurationSection("hidden").getKeys(false)) {
            String base = "hidden." + key;
            String blockId = state.getString(base + ".id", "");
            String world = state.getString(base + ".world", "world");
            int x = state.getInt(base + ".x");
            int y = state.getInt(base + ".y");
            int z = state.getInt(base + ".z");
            long respawnAt = state.getLong(base + ".respawnAt", now);

            if (blockId.isBlank()) {
                continue;
            }
            HiddenNodeState hidden = new HiddenNodeState(blockId.toLowerCase(Locale.ROOT), world, x, y, z, respawnAt);
            hiddenNodes.put(hidden.key(), hidden);
            if (respawnAt > now) {
                scheduleRespawn(hidden);
            }
        }
    }

    private void saveState() {
        state.set("hidden", null);
        for (HiddenNodeState hidden : hiddenNodes.values()) {
            String base = "hidden." + hidden.key();
            state.set(base + ".id", hidden.blockId);
            state.set(base + ".world", hidden.worldName);
            state.set(base + ".x", hidden.x);
            state.set(base + ".y", hidden.y);
            state.set(base + ".z", hidden.z);
            state.set(base + ".respawnAt", hidden.respawnAtMillis);
        }
        try {
            state.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save woodcutting_state.yml: " + e.getMessage());
        }
    }

    private String key(Location location) {
        if (location == null || location.getWorld() == null) {
            return "unknown:0:0:0";
        }
        return key(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private String key(String world, int x, int y, int z) {
        return world.toLowerCase(Locale.ROOT) + ":" + x + ":" + y + ":" + z;
    }

    private final class HiddenNodeState {
        private final String blockId;
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;
        private final long respawnAtMillis;

        private HiddenNodeState(String blockId, String worldName, int x, int y, int z, long respawnAtMillis) {
            this.blockId = blockId;
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.respawnAtMillis = respawnAtMillis;
        }

        private String key() {
            return WoodcuttingNodeListener.this.key(worldName, x, y, z);
        }
    }

    private static final class NodeProgressState {
        private final String worldName;
        private final int x;
        private final int y;
        private final int z;
        private final double maxDurability;
        private double damage;
        private TextDisplay display;

        private NodeProgressState(String worldName, int x, int y, int z, double maxDurability) {
            this.worldName = worldName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.maxDurability = maxDurability;
        }
    }
}
