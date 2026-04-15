package me.nakilex.levelplugin.debug.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.debug.StrongholdDebugGenerator;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class StrongholdAssetDebugGUI implements Listener {
    private static final String TITLE = "Stronghold Assets";
    private static StrongholdAssetDebugGUI instance;
    private boolean registered;

    private StrongholdAssetDebugGUI() {
    }

    public static StrongholdAssetDebugGUI getInstance() {
        if (instance == null) {
            instance = new StrongholdAssetDebugGUI();
        }
        return instance;
    }

    public void open(Player player) {
        if (player == null) {
            return;
        }
        ensureRegistered();
        Inventory inv = Bukkit.createInventory(null, 45, TITLE);
        render(inv);
        player.openInventory(inv);
    }

    private void ensureRegistered() {
        if (registered) {
            return;
        }
        Main plugin = Main.getInstance();
        if (plugin != null) {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            registered = true;
        }
    }

    private void render(Inventory inv) {
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        GuiUtil.fillBorder(inv, filler);

        StrongholdDebugGenerator.AssetScatterConfig cfg = StrongholdDebugGenerator.getAssetScatterConfig();
        StrongholdDebugGenerator.AssetDistributionCounts counts = StrongholdDebugGenerator.previewAssetDistribution();

        inv.setItem(11, numberItem(Material.CHEST, "Total Assets", cfg.totalCount(), "Total detached assets spawned after stronghold generation."));
        inv.setItem(13, percentItem(Material.OAK_SAPLING, "Trees", cfg.treePercent(), counts.trees()));
        inv.setItem(15, percentItem(Material.MOSSY_STONE_BRICKS, "Ruins", cfg.ruinPercent(), counts.ruins()));
        inv.setItem(17, percentItem(Material.COBBLESTONE, "Rocks", cfg.rockPercent(), counts.rocks()));

        int sum = cfg.treePercent() + cfg.ruinPercent() + cfg.rockPercent();
        List<String> infoLore = TooltipUtil.bulletList(
                "Current percent sum: " + sum + "%.",
                "Distribution auto-normalizes when spawning.",
                "Detached assets use a 5% overlap cap."
        );
        inv.setItem(31, GuiUtil.createGuiItem(Material.BOOK, ChatColor.AQUA + "Distribution Info", infoLore));

        List<String> resetLore = TooltipUtil.bulletList("Reset to 70% trees, 10% ruins, 20% rocks, total 250.");
        resetLore.add(" ");
        resetLore.addAll(TooltipUtil.clickInstructions("to reset", null));
        inv.setItem(40, GuiUtil.createGuiItem(Material.BARRIER, ChatColor.RED + "Reset", resetLore));
    }

    private ItemStack numberItem(Material material, String name, int current, String description) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + current);
        lore.add(" ");
        lore.add(ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + "Left/Right: ±1");
        lore.add(ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + "Shift+Left/Right: ±10");
        return GuiUtil.createGuiItem(material, ChatColor.GOLD + name, lore);
    }

    private ItemStack percentItem(Material material, String name, int percent, int computedCount) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current: " + ChatColor.WHITE + percent + "%");
        lore.add(ChatColor.GRAY + "Preview count: " + ChatColor.WHITE + computedCount);
        lore.add(" ");
        lore.add(ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + "Left/Right: ±1%");
        lore.add(ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + "Shift+Left/Right: ±5%");
        return GuiUtil.createGuiItem(material, ChatColor.AQUA + name, lore);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!TITLE.equals(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int slot = event.getRawSlot();
        ClickType click = event.getClick();
        int sign = (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) ? -1 : 1;
        boolean shift = click == ClickType.SHIFT_LEFT || click == ClickType.SHIFT_RIGHT;

        StrongholdDebugGenerator.AssetScatterConfig cfg = StrongholdDebugGenerator.getAssetScatterConfig();
        int step = shift ? 10 : 1;

        switch (slot) {
            case 11 -> StrongholdDebugGenerator.setAssetScatterTotalCount(cfg.totalCount() + sign * step);
            case 13 -> adjustDistribution(cfg.treePercent() + sign * (shift ? 5 : 1), cfg.ruinPercent(), cfg.rockPercent());
            case 15 -> adjustDistribution(cfg.treePercent(), cfg.ruinPercent() + sign * (shift ? 5 : 1), cfg.rockPercent());
            case 17 -> adjustDistribution(cfg.treePercent(), cfg.ruinPercent(), cfg.rockPercent() + sign * (shift ? 5 : 1));
            case 40 -> {
                StrongholdDebugGenerator.setAssetScatterTotalCount(250);
                StrongholdDebugGenerator.setAssetScatterDistribution(70, 10, 20);
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                        "Stronghold detached asset distribution reset to 70/10/20 with total 250.");
            }
            default -> {
                return;
            }
        }
        render(event.getView().getTopInventory());
    }

    private void adjustDistribution(int trees, int ruins, int rocks) {
        StrongholdDebugGenerator.setAssetScatterDistribution(trees, ruins, rocks);
    }
}
