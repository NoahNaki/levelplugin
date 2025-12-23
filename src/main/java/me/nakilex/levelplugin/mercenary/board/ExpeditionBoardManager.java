package me.nakilex.levelplugin.mercenary.board;

import com.nexomc.nexo.api.NexoFurniture;
import com.nexomc.nexo.mechanics.furniture.FurnitureMechanic;
import me.nakilex.levelplugin.lootchests.utils.LocationUtils;
import me.nakilex.levelplugin.utils.FurnitureCleanupUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Handles persistence and spawning of expedition boards placed in the world.
 */
public class ExpeditionBoardManager {
    private static final String FURNITURE_ID = "quest_board";

    private final Plugin plugin;
    private final Logger logger;
    private final NamespacedKey wandKey;
    private final File file;
    private FileConfiguration config;

    private final List<ExpeditionBoardLocation> boards = new ArrayList<>();
    private final Map<Integer, Location> placedBoards = new HashMap<>();

    public ExpeditionBoardManager(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.wandKey = new NamespacedKey(plugin, "expedition_board_wand");
        this.file = new File(plugin.getDataFolder(), "expeditionboards.yml");
        reload();
    }

    public void reload() {
        loadConfig();
        loadBoardsFromConfig();
        spawnAll();
    }

    public ItemStack createWand() {
        ItemStack wand = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = wand.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Expedition Board Wand");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Right-click a block to place an expedition board");
            lore.add(ChatColor.GRAY + "Left-click a block to delete the nearest board");
            lore.addAll(TooltipUtil.clickInstructions(null, "to save the location"));
            meta.getPersistentDataContainer().set(wandKey, PersistentDataType.INTEGER, 1);
            meta.setLore(lore);
            wand.setItemMeta(meta);
        }
        return wand;
    }

    public boolean isWand(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        Integer marker = stack.getItemMeta().getPersistentDataContainer().get(wandKey, PersistentDataType.INTEGER);
        return marker != null && marker == 1;
    }

    public ExpeditionBoardLocation registerBoard(Location location, BlockFace facing) {
        int id = boards.stream().mapToInt(ExpeditionBoardLocation::id).max().orElse(0) + 1;
        ExpeditionBoardLocation board = new ExpeditionBoardLocation(id, normalizeWorld(location),
                location.getBlockX(), location.getBlockY(), location.getBlockZ(), facing);
        boards.add(board);
        saveBoard(board);
        spawnBoard(board);
        return board;
    }

    public ExpeditionBoardLocation deleteBoard(int id) {
        ExpeditionBoardLocation removed = null;
        for (ExpeditionBoardLocation board : new ArrayList<>(boards)) {
            if (board.id() == id) {
                removed = board;
                boards.remove(board);
                break;
            }
        }
        if (removed == null) {
            return null;
        }

        removeBoardEntities(removed);
        placedBoards.remove(id);
        config.set("boards." + id, null);
        saveConfig();
        return removed;
    }

    public ExpeditionBoardLocation findNearest(Location reference) {
        if (reference == null || reference.getWorld() == null) {
            return null;
        }

        ExpeditionBoardLocation closest = null;
        double best = Double.MAX_VALUE;
        for (ExpeditionBoardLocation board : boards) {
            double dist = board.distanceSquared(reference);
            if (dist < best) {
                best = dist;
                closest = board;
            }
        }
        return closest;
    }

    public List<ExpeditionBoardLocation> getBoards() {
        return Collections.unmodifiableList(boards);
    }

    private void spawnAll() {
        clearPlacedBoards();
        for (ExpeditionBoardLocation board : boards) {
            spawnBoard(board);
        }
    }

    private void clearPlacedBoards() {
        for (Location location : placedBoards.values()) {
            FurnitureCleanupUtil.clearNearbyFurnitureEntities(plugin, location, 4.0, "[ExpeditionBoardManager]");
        }
        placedBoards.clear();
    }

    private void spawnBoard(ExpeditionBoardLocation board) {
        Location location = board.toLocation();
        if (location == null) {
            logger.warning("[ExpeditionBoardManager] Unable to spawn board #" + board.id() + "; world not loaded.");
            return;
        }
        FurnitureCleanupUtil.clearNearbyFurnitureEntities(plugin, location, 4.0, "[ExpeditionBoardManager]");
        FurnitureMechanic mechanic = NexoFurniture.furnitureMechanic(FURNITURE_ID);
        if (mechanic == null) {
            logger.severe("[ExpeditionBoardManager] Furniture ID '" + FURNITURE_ID + "' is missing. Is Nexo configured?");
            return;
        }
        Location centered = LocationUtils.centerOnBlock(location);
        NexoFurniture.place(FURNITURE_ID, centered, 0f, board.facing());
        placedBoards.put(board.id(), location.getBlock().getLocation());
    }

    private void removeBoardEntities(ExpeditionBoardLocation board) {
        Location loc = board.toLocation();
        if (loc == null) {
            return;
        }
        FurnitureCleanupUtil.clearNearbyFurnitureEntities(plugin, loc, 4.0, "[ExpeditionBoardManager]");
    }

    private void loadConfig() {
        if (!file.exists()) {
            try {
                plugin.saveResource("expeditionboards.yml", false);
            } catch (IllegalArgumentException ignored) {
                // Resource may not exist; create an empty file instead.
                try {
                    file.getParentFile().mkdirs();
                    if (!file.createNewFile()) {
                        logger.warning("[ExpeditionBoardManager] Could not create expeditionboards.yml");
                    }
                } catch (IOException e) {
                    logger.warning("[ExpeditionBoardManager] Failed to create expeditionboards.yml: " + e.getMessage());
                }
            }
        }
        config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (Exception e) {
            logger.warning("[ExpeditionBoardManager] Failed to load expeditionboards.yml: " + e.getMessage());
        }
    }

    private void loadBoardsFromConfig() {
        boards.clear();
        ConfigurationSection section = config.getConfigurationSection("boards");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                String world = section.getString(key + ".world", "world");
                double x = section.getDouble(key + ".x");
                double y = section.getDouble(key + ".y");
                double z = section.getDouble(key + ".z");
                BlockFace facing = BlockFace.valueOf(section.getString(key + ".facing", BlockFace.NORTH.name()));
                boards.add(new ExpeditionBoardLocation(id, world, x, y, z, facing));
            } catch (Exception ex) {
                logger.warning("[ExpeditionBoardManager] Failed to load board entry '" + key + "': " + ex.getMessage());
            }
        }
    }

    private void saveBoard(ExpeditionBoardLocation board) {
        String path = "boards." + board.id();
        config.set(path + ".world", board.worldName());
        config.set(path + ".x", board.x());
        config.set(path + ".y", board.y());
        config.set(path + ".z", board.z());
        config.set(path + ".facing", board.facing().name());
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            logger.warning("[ExpeditionBoardManager] Could not save expeditionboards.yml: " + e.getMessage());
        }
    }

    private String normalizeWorld(Location location) {
        if (location == null || location.getWorld() == null) {
            return "world";
        }
        String storedWorld = location.getWorld().getName();
        if (storedWorld.equalsIgnoreCase("mmorpg")) {
            return "world";
        }
        return storedWorld;
    }
}
