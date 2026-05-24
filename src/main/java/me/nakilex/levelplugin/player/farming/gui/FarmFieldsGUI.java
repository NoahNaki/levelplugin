package me.nakilex.levelplugin.player.farming.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.environment.EnvironmentManager;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class FarmFieldsGUI implements Listener, CommandExecutor {
    private static final String MAIN_TITLE = "Farm Fields";
    private static final String SELECT_TITLE_PREFIX = "Select Seed: Field ";
    private static final int[] FIELD_SLOTS = {11, 13, 15};
    private static final Location TEMPLATE_ANCHOR = new Location(null, 3489, 77, -3603);

    private final Main plugin;
    private final EnvironmentManager environmentManager;
    private final Map<UUID, FarmingCrop[]> selections = new HashMap<>();
    private final File dataFile;
    private YamlConfiguration data;

    private record Plot(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}
    private static final Plot[] PLOTS = {
            new Plot(3770, 96, -3472, 3800, 103, -3449),
            new Plot(3787, 81, -3549, 3810, 96, -3499),
            new Plot(3752, 79, -3554, 3781, 95, -3512)
    };

    public FarmFieldsGUI(Main plugin, EnvironmentManager environmentManager) {
        this.plugin = plugin;
        this.environmentManager = environmentManager;
        this.dataFile = new File(plugin.getDataFolder(), "farm_fields.yml");
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        load();
        plugin.getCommand("farmfields").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        openMain(player);
        return true;
    }

    private void openMain(Player player) {
        Inventory inv = GuiBuilder.create(27, MAIN_TITLE).filler(Material.GRAY_STAINED_GLASS_PANE).border().build();
        int farmLevel = Math.max(0, environmentManager.getPlayerBuildingStage(player, "farm"));
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
        Location origin = environmentManager.getOrigin(player.getUniqueId());
        if (origin == null || origin.getWorld() == null) return;
        Plot local = PLOTS[fieldIndex];
        int dx = origin.getBlockX() - (int) TEMPLATE_ANCHOR.getX();
        int dy = origin.getBlockY() - (int) TEMPLATE_ANCHOR.getY();
        int dz = origin.getBlockZ() - (int) TEMPLATE_ANCHOR.getZ();
        Plot plot = new Plot(local.minX + dx, local.minY + dy, local.minZ + dz, local.maxX + dx, local.maxY + dy, local.maxZ + dz);
        World world = origin.getWorld();
        int minX = Math.min(plot.minX, plot.maxX), maxX = Math.max(plot.minX, plot.maxX);
        int minY = Math.min(plot.minY, plot.maxY), maxY = Math.max(plot.minY, plot.maxY);
        int minZ = Math.min(plot.minZ, plot.maxZ), maxZ = Math.max(plot.minZ, plot.maxZ);
        int planted = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block base = world.getBlockAt(x, y, z);
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
            planted = plantNearbyFarmland(player, crop, 30);
            if (planted > 0) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                        "Field bounds had no farmland; planted nearby farmland instead (" + ChatColor.WHITE + planted
                                + ChatColor.GRAY + " blocks)." );
            }
        }
    }

    private int plantNearbyFarmland(Player player, FarmingCrop crop, int radius) {
        if (player == null || crop == null || player.getWorld() == null) return 0;
        Location c = player.getLocation();
        World world = c.getWorld();
        int cx = c.getBlockX(), cy = c.getBlockY(), cz = c.getBlockZ();
        int planted = 0;
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = Math.max(world.getMinHeight(), cy - 10); y <= Math.min(world.getMaxHeight() - 2, cy + 10); y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    Block base = world.getBlockAt(x, y, z);
                    if (base.getType() != Material.FARMLAND) continue;
                    Block cropBlock = base.getRelative(0, 1, 0);
                    if (cropBlock.getType() != Material.AIR && cropBlock.getType() != crop.getBlockMaterial()) continue;
                    cropBlock.setType(crop.getBlockMaterial(), false);
                    if (cropBlock.getBlockData() instanceof Ageable ageable) {
                        ageable.setAge(0);
                        cropBlock.setBlockData(ageable, false);
                    }
                    planted++;
                }
            }
        }
        return planted;
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
