package me.nakilex.levelplugin.player.farming.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.EnvironmentManager;
import me.nakilex.levelplugin.environment.EnvironmentAreaInstanceManager;
import me.nakilex.levelplugin.player.farming.data.FarmingCrop;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.bukkit.scheduler.BukkitTask;

public final class FarmFieldsGUI implements Listener, CommandExecutor {
    private static final String MAIN_TITLE = "Farm Fields";
    private static final String SELECT_TITLE_PREFIX = "Select Seed: Field ";
    private static final int[] FIELD_SLOTS = {11, 13, 15};

    private final Main plugin;
    private final EnvironmentManager environmentManager;
    private final EnvironmentAreaInstanceManager areaInstanceManager;
    private final Map<UUID, FarmingCrop[]> selections = new HashMap<>();
    private final File dataFile;
    private YamlConfiguration data;
    private BukkitTask growthTask;
    private final Map<FarmingCrop, Integer> growthStepTicks = Map.of(
            FarmingCrop.WHEAT, 40,
            FarmingCrop.POTATO, 50,
            FarmingCrop.CARROT, 55,
            FarmingCrop.BEETROOT, 60,
            FarmingCrop.SWEET_BERRIES, 70,
            FarmingCrop.PUMPKIN, 80
    );
    private long growthTick = 0L;
    private static double growthSpeedMultiplier = 1.0;
    public static double getGrowthSpeedMultiplier() { return growthSpeedMultiplier; }
    public static void setGrowthSpeedMultiplier(double value) { growthSpeedMultiplier = Math.max(0.1, Math.min(20.0, value)); }

    private record Plot(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}
    private static final Plot[] PLOTS = {
            // Coordinates are authored in the finished kingdom reference area.
            // We project them into each player's initialized kingdom via TEMPLATE_ANCHOR offset.
            new Plot(3770, 96, -3012, 3800, 103, -2989),
            new Plot(3787, 81, -3089, 3810, 96, -3039),
            new Plot(3752, 79, -3094, 3781, 95, -3052)
    };

    public FarmFieldsGUI(Main plugin, EnvironmentManager environmentManager) {
        this.plugin = plugin;
        this.environmentManager = environmentManager;
        this.areaInstanceManager = EnvironmentAreaInstanceManager.getInstance(plugin);
        this.dataFile = new File(plugin.getDataFolder(), "farm_fields.yml");
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        load();
        plugin.getCommand("farmfields").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startGrowthTask();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        openMain(player);
        return true;
    }

    private void openMain(Player player) {
        Inventory inv = GuiBuilder.create(27, MAIN_TITLE).filler(Material.GRAY_STAINED_GLASS_PANE).border().build();
        int farmLevel = Math.max(0, areaInstanceManager.getFarmLevel(player));
        FarmingCrop[] playerSelection = selections.computeIfAbsent(player.getUniqueId(), id -> new FarmingCrop[3]);

        for (int i = 0; i < 3; i++) {
            if (i + 1 > farmLevel) {
                inv.setItem(FIELD_SLOTS[i], GuiUtil.getNexoItem("lock", ChatColor.RED + "Field " + (i + 1) + " Locked",
                        TooltipUtil.bulletList("Upgrade Farm to level " + (i + 1) + " to unlock this plot.")));
                continue;
            }
            FarmingCrop crop = playerSelection[i];
            ItemStack item = new ItemStack(crop == null ? Material.FARMLAND : crop.getItemMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + "Field " + (i + 1));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Current Seed: " + ChatColor.WHITE + (crop == null ? "None" : pretty(crop.name())));
                lore.add("");
                lore.addAll(TooltipUtil.clickInstructions("to select seed", null));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(FIELD_SLOTS[i], item);
        }
        player.openInventory(inv);
    }

    private void openSeedSelect(Player player, int fieldIndex) {
        Inventory inv = GuiBuilder.create(27, SELECT_TITLE_PREFIX + (fieldIndex + 1)).filler(Material.BLACK_STAINED_GLASS_PANE).border().build();
        int[] slots = {10,11,12,13,14,15,16};
        int idx = 0;
        for (FarmingCrop crop : FarmingCrop.values()) {
            if (idx >= slots.length) break;
            ItemStack item = new ItemStack(crop.getItemMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + pretty(crop.name()));
                meta.setLore(TooltipUtil.bulletList("Required Farming Level: " + crop.getLevelRequirement()));
                item.setItemMeta(meta);
            }
            inv.setItem(slots[idx++], item);
        }
        player.openInventory(inv);
    }


    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        FarmingCrop crop = FarmingCrop.fromBlock(event.getBlock());
        if (crop == null) return;
        if (isManagedFarmBlock(event.getBlock(), crop)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!GuiUtil.titleMatches(title, MAIN_TITLE) && !title.startsWith(SELECT_TITLE_PREFIX)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;

        if (GuiUtil.titleMatches(title, MAIN_TITLE)) {
            int farmLevel = Math.max(0, environmentManager.getPlayerBuildingStage(player, "farm"));
            for (int i = 0; i < FIELD_SLOTS.length; i++) {
                if (event.getRawSlot() == FIELD_SLOTS[i]) {
                    if (i + 1 > farmLevel) {
                        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Field " + (i + 1) + " is locked.");
                        return;
                    }
                    openSeedSelect(player, i);
                    return;
                }
            }
            return;
        }

        int fieldNumber = Integer.parseInt(title.substring(SELECT_TITLE_PREFIX.length()).trim());
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        FarmingCrop crop = FarmingCrop.fromItem(clicked.getType());
        if (crop == null) return;
        int fieldIndex = fieldNumber - 1;
        FarmingCrop[] arr = selections.computeIfAbsent(player.getUniqueId(), id -> new FarmingCrop[3]);
        arr[fieldIndex] = crop;
        save();
        plantField(player, fieldIndex, crop);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Field " + fieldNumber + " now growing " + ChatColor.WHITE + pretty(crop.name()) + ChatColor.GREEN + ".");
        openMain(player);
    }

    private void plantField(Player player, int fieldIndex, FarmingCrop crop) {
        Plot local = PLOTS[fieldIndex];
        EnvironmentAreaInstanceManager.RuntimeCuboid plot = areaInstanceManager.projectFinishedSelectionForPlayer(
                player, local.minX(), local.minY(), local.minZ(), local.maxX(), local.maxY(), local.maxZ());
        if (plot == null || plot.world() == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Your kingdom area is not initialized yet.");
            return;
        }

        int planted = 0;
        for (int x = plot.minX(); x <= plot.maxX(); x++) {
            for (int y = plot.minY(); y <= plot.maxY(); y++) {
                for (int z = plot.minZ(); z <= plot.maxZ(); z++) {
                    Block base = plot.world().getBlockAt(x, y, z);
                    if (base.getType() != Material.FARMLAND) continue;
                    Block cropBlock = base.getRelative(0, 1, 0);
                    cropBlock.setType(crop.getBlockMaterial(), false);
                    if (cropBlock.getBlockData() instanceof Ageable ageable) {
                        ageable.setAge(0);
                        cropBlock.setBlockData(ageable, false);
                    }
                    planted++;
                }
            }
        }

        if (planted == 0) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "No farmland found in projected Field " + (fieldIndex + 1) + ".");
        }
    }

    private void startGrowthTask() {
        if (growthTask != null) {
            growthTask.cancel();
        }
        growthTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickManagedGrowth, 20L, 20L);
    }

    private void tickManagedGrowth() {
        growthTick++;
        for (Player player : Bukkit.getOnlinePlayers()) {
            FarmingCrop[] selected = selections.get(player.getUniqueId());
            if (selected == null) continue;
            for (int i = 0; i < selected.length; i++) {
                FarmingCrop crop = selected[i];
                if (crop == null) continue;
                int baseEvery = Math.max(1, growthStepTicks.getOrDefault(crop, 60));
                int every = Math.max(1, (int) Math.round(baseEvery / growthSpeedMultiplier));
                if (growthTick % every != 0) continue;
                advanceFieldAge(player, i, crop);
            }
        }
    }

    private void advanceFieldAge(Player player, int fieldIndex, FarmingCrop crop) {
        Plot local = PLOTS[fieldIndex];
        EnvironmentAreaInstanceManager.RuntimeCuboid plot = areaInstanceManager.projectFinishedSelectionForPlayer(
                player, local.minX(), local.minY(), local.minZ(), local.maxX(), local.maxY(), local.maxZ());
        if (plot == null || plot.world() == null) return;
        for (int x = plot.minX(); x <= plot.maxX(); x++) {
            for (int y = plot.minY(); y <= plot.maxY(); y++) {
                for (int z = plot.minZ(); z <= plot.maxZ(); z++) {
                    Block base = plot.world().getBlockAt(x, y, z);
                    if (base.getType() != Material.FARMLAND) continue;
                    incrementCropAge(base.getRelative(0, 1, 0), crop);
                }
            }
        }
    }

    private void incrementCropAge(Block cropBlock, FarmingCrop crop) {
        if (cropBlock == null || crop == null) return;
        if (cropBlock.getType() != crop.getBlockMaterial()) return;
        if (!(cropBlock.getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() >= ageable.getMaximumAge()) return;
        ageable.setAge(Math.min(ageable.getMaximumAge(), ageable.getAge() + 1));
        cropBlock.setBlockData(ageable, false);
    }

    private boolean isManagedFarmBlock(Block block, FarmingCrop crop) {
        if (block == null || crop == null) return false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            FarmingCrop[] selected = selections.get(player.getUniqueId());
            if (selected == null) continue;
            for (int i = 0; i < selected.length; i++) {
                if (selected[i] != crop) continue;
                Plot local = PLOTS[i];
                EnvironmentAreaInstanceManager.RuntimeCuboid plot = areaInstanceManager.projectFinishedSelectionForPlayer(
                        player, local.minX(), local.minY(), local.minZ(), local.maxX(), local.maxY(), local.maxZ());
                if (plot == null || plot.world() == null || !plot.world().equals(block.getWorld())) continue;
                int x = block.getX(), y = block.getY(), z = block.getZ();
                if (x >= plot.minX() && x <= plot.maxX() && y >= plot.minY() && y <= plot.maxY() && z >= plot.minZ() && z <= plot.maxZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void load() {
        for (String key : data.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                FarmingCrop[] arr = new FarmingCrop[3];
                for (int i = 0; i < 3; i++) {
                    String value = data.getString(key + ".field" + (i + 1));
                    if (value != null) arr[i] = FarmingCrop.valueOf(value);
                }
                selections.put(uuid, arr);
            } catch (Exception ignored) {}
        }
    }

    private void save() {
        for (Map.Entry<UUID, FarmingCrop[]> entry : selections.entrySet()) {
            String key = entry.getKey().toString();
            for (int i = 0; i < 3; i++) {
                FarmingCrop crop = entry.getValue()[i];
                data.set(key + ".field" + (i + 1), crop == null ? null : crop.name());
            }
        }
        try { data.save(dataFile); } catch (IOException e) { plugin.getLogger().warning("Failed to save farm_fields.yml: " + e.getMessage()); }
    }

    private static String pretty(String input) {
        String raw = input.toLowerCase(Locale.ROOT).replace('_', ' ');
        return raw.substring(0, 1).toUpperCase(Locale.ROOT) + raw.substring(1);
    }
}
