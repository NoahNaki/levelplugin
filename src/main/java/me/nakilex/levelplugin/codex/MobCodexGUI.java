package me.nakilex.levelplugin.codex;

import me.nakilex.levelplugin.mob.utils.MobNameUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MobCodexGUI implements Listener {
    // Use a unique title so our click listener doesn't interfere with the
    // main codex menu which also uses "Codex" as its title.
    private static final String TITLE = ChatColor.BLACK + "Codex - Mobs";
    private static final int SIZE = 54;

    private static final int ITEMS_PER_PAGE = CodexGuiUtil.CONTENT_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int BACK_SLOT = 49;

    private final CodexManager manager;
    private CodexMainGUI mainGui;
    private final Map<UUID, Integer> pageMap = new HashMap<>();

    public MobCodexGUI(CodexManager manager, CodexMainGUI mainGui) {
        this.manager = manager;
        this.mainGui = mainGui;
    }

    public void setMainGui(CodexMainGUI gui) { this.mainGui = gui; }

    public void open(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        open(player, page);
    }

    private void open(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();
        Map<String, String> lines = new LinkedHashMap<>();
        lines.put("Mobs", manager.getDiscoveredMobCount(player.getUniqueId()) + "/" + manager.getTotalMobCount());
        inv.setItem(4, CodexGuiUtil.createInfoBook("Discoveries", lines));

        List<String> mobs = manager.getAllMobKeys();
        int start = page * ITEMS_PER_PAGE;
        for (int i = start, slot = 0; i < mobs.size() && slot < ITEMS_PER_PAGE; i++) {
            String key = mobs.get(i);
            inv.setItem(CodexGuiUtil.CONTENT_SLOTS[slot++], createMobIcon(player.getUniqueId(), key));
        }

        if (page > 0) inv.setItem(PREV_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        if (mobs.size() > (page + 1) * ITEMS_PER_PAGE) inv.setItem(NEXT_SLOT, GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        inv.setItem(BACK_SLOT, GuiUtil.getNexoItem("arrow_left", ChatColor.YELLOW + "Back"));

        player.openInventory(inv);
    }

    private ItemStack createMobIcon(UUID id, String key) {
        boolean discovered = manager.hasDiscovered(id, key);
        ItemStack item = new ItemStack(discovered ? Material.SKELETON_SKULL : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (discovered) {
                String name = MobNameUtil.getPlainDisplayName(key);
                meta.setDisplayName(ChatColor.GREEN + name);
                int level = manager.getMobLevel(id, key);
                double progress = manager.getMobProgress(id, key);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + level);
                String bar = GuiUtil.createProgressBar(progress, 15);
                int kills = manager.getKillCount(id, key);
                if (level >= manager.getMaxMobLevel()) {
                    lore.add(bar + " " + ChatColor.YELLOW + kills + ChatColor.GOLD + "+");
                } else {
                    int next = manager.getKillsForLevel(level + 1);
                    lore.add(bar + " " + ChatColor.YELLOW + kills + ChatColor.GOLD + "/" + ChatColor.YELLOW + next);
                }
                meta.setLore(lore);
            } else {
                meta.setDisplayName(ChatColor.DARK_GRAY + "???");
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(TITLE)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;

        int slot = e.getRawSlot();
        if (slot == PREV_SLOT) {
            int page = pageMap.getOrDefault(p.getUniqueId(), 0);
            open(p, Math.max(0, page - 1));
            return;
        }
        if (slot == NEXT_SLOT) {
            int page = pageMap.getOrDefault(p.getUniqueId(), 0);
            open(p, page + 1);
            return;
        }
        if (slot == BACK_SLOT && mainGui != null) {
            mainGui.open(p);
        }
    }
}
