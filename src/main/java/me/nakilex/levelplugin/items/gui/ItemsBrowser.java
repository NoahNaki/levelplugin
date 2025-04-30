package me.nakilex.levelplugin.items.gui;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.StatRange;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ItemsBrowser implements CommandExecutor, Listener {
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS;
    private static final int PAGE_SIZE = 28; // 4 rows × 7 cols of content

    private final JavaPlugin plugin;

    public ItemsBrowser(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("itemsbrowser").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private String title(int page) {
        return ChatColor.GRAY + "Items Browser - Page " + (page + 1);
    }

    private static ItemStack createMenuItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(java.util.Arrays.asList(loreLines));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openPage(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, SIZE, title(page));

        // 1) Fill every slot with a light‐gray pane
        ItemStack filler = createMenuItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            gui.setItem(i, filler);
        }

        // 2) Grab and sort all template IDs
        List<Integer> ids = new ArrayList<>(ItemManager.getInstance().getAllTemplates().keySet());
        Collections.sort(ids);
        int start = page * PAGE_SIZE;

        // 3) Build the 4×7 grid of previews
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= ids.size()) break;

            CustomItem tpl = ItemManager.getInstance().getTemplateById(ids.get(idx));
            if (tpl == null) continue;

            // a) Create the ItemStack
            ItemStack preview = new ItemStack(tpl.getMaterial(), 1);
            ItemMeta pm = preview.getItemMeta();
            if (pm == null) continue;

            // b) Name + rarity color
            ChatColor col = tpl.getRarity().getColor();
            pm.setDisplayName(col + tpl.getBaseName());

            // c) Build lore
            List<String> lore = new ArrayList<>();
            lore.add(""); // spacer

            // — Level Requirement with ✔/✘
            int playerLvl = StatsManager.getInstance().getLevel(player);
            boolean lvlOk = playerLvl >= tpl.getLevelRequirement();
            lore.add((lvlOk ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                + ChatColor.GRAY + "Level Requirement: "
                + ChatColor.WHITE + tpl.getLevelRequirement());

            // — Class Requirement with ✔/✘
            if (!tpl.getClassRequirement().equalsIgnoreCase("ANY")) {
                PlayerClass pcls = StatsManager.getInstance()
                    .getPlayerStats(player.getUniqueId()).playerClass;
                boolean clsOk = pcls.name().equalsIgnoreCase(tpl.getClassRequirement());
                String cap = tpl.getClassRequirement().substring(0,1).toUpperCase()
                    + tpl.getClassRequirement().substring(1).toLowerCase();
                lore.add((clsOk ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                    + ChatColor.GRAY + "Class Requirement: "
                    + ChatColor.WHITE + cap);
            }

            lore.add(""); // spacer

            // — Stat RANGES (numbers in white)
            StatRange s;
            s = tpl.getHpRange();
            if (!(s.getMin()==0 && s.getMax()==0))
                lore.add(ChatColor.RED   + "❤ " + ChatColor.GRAY + "Health: "
                    + ChatColor.WHITE + "+" + s);
            s = tpl.getDefRange();
            if (!(s.getMin()==0 && s.getMax()==0))
                lore.add(ChatColor.GRAY  + "⛂ " + ChatColor.GRAY + "Defence: "
                    + ChatColor.WHITE + "+" + s);
            s = tpl.getStrRange();
            if (!(s.getMin()==0 && s.getMax()==0))
                lore.add(ChatColor.BLUE  + "☠ " + ChatColor.GRAY + "Strength: "
                    + ChatColor.WHITE + "+" + s);
            s = tpl.getAgiRange();
            if (!(s.getMin()==0 && s.getMax()==0))
                lore.add(ChatColor.GREEN + "≈ " + ChatColor.GRAY + "Agility: "
                    + ChatColor.WHITE + "+" + s);
            s = tpl.getIntelRange();
            if (!(s.getMin()==0 && s.getMax()==0))
                lore.add(ChatColor.AQUA  + "♦ " + ChatColor.GRAY + "Intelligence: "
                    + ChatColor.WHITE + "+" + s);
            s = tpl.getDexRange();
            if (!(s.getMin()==0 && s.getMax()==0))
                lore.add(ChatColor.YELLOW+ "➹ " + ChatColor.GRAY + "Dexterity: "
                    + ChatColor.WHITE + "+" + s);

            lore.add(""); // spacer

            // — Rarity
            lore.add(col + "" + ChatColor.BOLD + tpl.getRarity().name());

            // d) Apply lore & flags
            pm.setLore(lore);
            pm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            pm.setUnbreakable(true);

            // e) Stamp the template ID (so clicking gives the right item)
            pm.getPersistentDataContainer()
                .set(ItemUtil.ITEM_ID_KEY, PersistentDataType.INTEGER, tpl.getId());
            pm.getPersistentDataContainer()
                .set(ItemUtil.UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, 0);

            preview.setItemMeta(pm);

            // f) Compute final slot and place
            int row = 1 + (i / 7);
            int colIndex = 1 + (i % 7);
            gui.setItem(row * COLS + colIndex, preview);
        }

        // 4) Pagination buttons
        ItemStack prev = createMenuItem(Material.ARROW, ChatColor.GREEN + "Previous Page");
        gui.setItem(SIZE - COLS, prev);
        ItemStack next = createMenuItem(Material.ARROW, ChatColor.GREEN + "Next Page");
        gui.setItem(SIZE - 1, next);

        // 5) Finally open
        player.openInventory(gui);
    }





    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can browse items.");
            return true;
        }
        openPage((Player) sender, 0);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().startsWith(ChatColor.GRAY + "Items Browser")) return;
        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();
        String stripped = ChatColor.stripColor(e.getView().getTitle());
        int currentPage = Integer.parseInt(stripped.split(" ")[stripped.split(" ").length - 1]) - 1;
        int maxPage = (ItemManager.getInstance().getAllTemplates().size() - 1) / PAGE_SIZE;

        // Next Page?
        if (name.equals(ChatColor.GREEN + "Next Page")) {
            int nextPage = currentPage < maxPage ? currentPage + 1 : 0;
            openPage(player, nextPage);
            return;
        }

        // Previous Page?
        if (name.equals(ChatColor.GREEN + "Previous Page")) {
            int prevPage = currentPage > 0 ? currentPage - 1 : maxPage;
            openPage(player, prevPage);
            return;
        }

        // Otherwise, if this is one of our item-templates, give it
        int templateId = ItemUtil.getCustomItemId(clicked);
        if (templateId != -1) {
            CustomItem instance = ItemManager.getInstance().rollNewInstance(templateId);
            // give to player
            ItemStack toGive = ItemUtil.createItemStackFromCustomItem(instance, 1, player);
            player.getInventory().addItem(toGive);
            player.sendMessage(ChatColor.GREEN + "You received: " + toGive.getItemMeta().getDisplayName());
        }
    }
}
