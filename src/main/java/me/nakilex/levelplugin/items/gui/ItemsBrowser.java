package me.nakilex.levelplugin.items.gui;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.StatRange;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
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

    private static final int TYPE_FILTER_SLOT   = 46;
    private static final int RARITY_FILTER_SLOT = 48;
    private static final int LEVEL_FILTER_SLOT  = 50;

    private final JavaPlugin plugin;
    private final java.util.Map<java.util.UUID,Integer> typeFilters   = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID,Integer> rarityFilters = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID,Integer> levelFilters  = new java.util.HashMap<>();

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

    private ItemStack getNexoItem(String id, String name) {
        com.nexomc.nexo.items.ItemBuilder b = com.nexomc.nexo.api.NexoItems.itemFromId(id);
        if (b == null) return new ItemStack(Material.BARRIER);
        ItemStack it = b.build();
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); it.setItemMeta(m); }
        return it;
    }

    private ItemStack createTypeButton(int filter) {
        ItemStack it = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Type Filter");
            List<String> lore = new ArrayList<>();
            String[] types = {"ARMOR","WEAPON","ALL"};
            for (int i = 0; i < types.length; i++) {
                String line = (i == filter ? ChatColor.GREEN : ChatColor.GRAY) + types[i];
                lore.add(line);
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createRarityButton(int filter) {
        ItemStack it = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Rarity Filter");
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            me.nakilex.levelplugin.items.data.ItemRarity[] arr = me.nakilex.levelplugin.items.data.ItemRarity.values();
            for (int i = 0; i < arr.length; i++) {
                String line = (i == filter ? ChatColor.GREEN : ChatColor.GRAY) + arr[i].name();
                lore.add(line);
            }
            lore.add((arr.length == filter ? ChatColor.GREEN : ChatColor.GRAY) + "ALL");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack createLevelButton(int filter) {
        ItemStack it = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Level Filter");
            List<String> lore = new ArrayList<>();
            String[] ranges = {"Lv. 1-19","Lv. 20-39","Lv. 40-59","Lv. 60-79","Lv. 80+","ALL"};
            for (int i = 0; i < ranges.length; i++) {
                String line = (i == filter ? ChatColor.GREEN : ChatColor.GRAY) + ranges[i];
                lore.add(line);
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }


    private void openPage(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, SIZE, title(page));

        ItemStack filler = createMenuItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, filler);
        }

        // 2) Grab and sort all template IDs
        List<Integer> ids = new ArrayList<>(ItemManager.getInstance().getAllTemplates().keySet());
        Collections.sort(ids);

        int tFilter = typeFilters.getOrDefault(player.getUniqueId(), 2);
        int rFilter = rarityFilters.getOrDefault(player.getUniqueId(), me.nakilex.levelplugin.items.data.ItemRarity.values().length);
        int lFilter = levelFilters.getOrDefault(player.getUniqueId(), 5);

        List<CustomItem> templates = new ArrayList<>();
        for (int id : ids) {
            CustomItem tpl = ItemManager.getInstance().getTemplateById(id);
            if (tpl == null) continue;
            if (rFilter < me.nakilex.levelplugin.items.data.ItemRarity.values().length && tpl.getRarity() != me.nakilex.levelplugin.items.data.ItemRarity.values()[rFilter])
                continue;
            if (lFilter < 5) {
                int lvl = tpl.getLevelRequirement();
                int min = lFilter*20 + 1; int max = lFilter==4?999: min+19;
                if (lvl < min || lvl > max) continue;
            }
            boolean isWeapon = me.nakilex.levelplugin.items.data.WeaponType.matchType(new ItemStack(tpl.getMaterial())) != null;
            if (tFilter == 0 && isWeapon) continue;
            if (tFilter == 1 && !isWeapon) continue;
            templates.add(tpl);
        }

        int start = page * PAGE_SIZE;

        // 3) Build the 4×7 grid of previews
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= templates.size()) break;

            CustomItem tpl = templates.get(idx);
            if (tpl == null) continue;

            // a) Create the ItemStack. For Ego templates build the Nexo model
            //    using EgoWeaponManager so the preview matches the actual item.
            ItemStack preview;
            ItemMeta pm;
            if (tpl.isEgo() && tpl.getEgoKey() != null) {
                me.nakilex.levelplugin.ego.EgoWeapon proto =
                        me.nakilex.levelplugin.ego.EgoWeaponManager
                                .getInstance().getPrototype(tpl.getEgoKey());
                if (proto != null) {
                    preview = me.nakilex.levelplugin.ego.EgoWeaponManager
                            .getInstance().createWeaponItem(proto.copy(), tpl.getId());
                } else {
                    preview = new ItemStack(tpl.getMaterial(), 1);
                }
            } else {
                preview = new ItemStack(tpl.getMaterial(), 1);
            }
            pm = preview.getItemMeta();
            if (pm == null) continue;

            // b) Apply display/lore depending on whether this is an Ego template
            ChatColor col = tpl.getRarity().getColor();
            if (tpl.isEgo() && tpl.getEgoKey() != null) {
                // The preview already contains proper lore from createWeaponItem.
                pm.getPersistentDataContainer()
                        .set(ItemUtil.ITEM_ID_KEY, PersistentDataType.INTEGER, tpl.getId());
                pm.getPersistentDataContainer()
                        .set(ItemUtil.UPGRADE_LEVEL_KEY, PersistentDataType.INTEGER, 0);
                preview.setItemMeta(pm);
            } else {
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
            }

            // f) Compute final slot and place
            int row = 1 + (i / 7);
            int colIndex = 1 + (i % 7);
            gui.setItem(row * COLS + colIndex, preview);
        }

        // 4) Pagination buttons
        ItemStack prev = getNexoItem("arrow_left", ChatColor.GREEN + "Previous Page");
        gui.setItem(SIZE - COLS, prev);
        ItemStack next = getNexoItem("arrow_right", ChatColor.GREEN + "Next Page");
        gui.setItem(SIZE - 1, next);

        gui.setItem(TYPE_FILTER_SLOT, createTypeButton(tFilter));
        gui.setItem(RARITY_FILTER_SLOT, createRarityButton(rFilter));
        gui.setItem(LEVEL_FILTER_SLOT, createLevelButton(lFilter));

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
        int tFilter = typeFilters.getOrDefault(player.getUniqueId(), 2);
        int rFilter = rarityFilters.getOrDefault(player.getUniqueId(), me.nakilex.levelplugin.items.data.ItemRarity.values().length);
        int lFilter = levelFilters.getOrDefault(player.getUniqueId(), 5);
        int total = 0;
        for (CustomItem ci : ItemManager.getInstance().getAllTemplates().values()) {
            if (rFilter < me.nakilex.levelplugin.items.data.ItemRarity.values().length && ci.getRarity() != me.nakilex.levelplugin.items.data.ItemRarity.values()[rFilter]) continue;
            if (lFilter < 5) {
                int lvl = ci.getLevelRequirement();
                int min = lFilter*20 + 1; int max = lFilter==4?999:min+19;
                if (lvl < min || lvl > max) continue;
            }
            boolean isWeapon = me.nakilex.levelplugin.items.data.WeaponType.matchType(new ItemStack(ci.getMaterial())) != null;
            if (tFilter == 0 && isWeapon) continue;
            if (tFilter == 1 && !isWeapon) continue;
            total++;
        }
        int maxPage = (Math.max(total,1) - 1) / PAGE_SIZE;

        if (e.getRawSlot() == TYPE_FILTER_SLOT) {
            int f = typeFilters.getOrDefault(player.getUniqueId(), 2);
            if (e.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) f--; else f++;
            if (f < 0) f = 2; if (f > 2) f = 0;
            typeFilters.put(player.getUniqueId(), f);
            openPage(player, 0);
            return;
        }

        if (e.getRawSlot() == RARITY_FILTER_SLOT) {
            int f = rarityFilters.getOrDefault(player.getUniqueId(), me.nakilex.levelplugin.items.data.ItemRarity.values().length);
            if (e.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) f--; else f++;
            int max = me.nakilex.levelplugin.items.data.ItemRarity.values().length;
            if (f < 0) f = max; if (f > max) f = 0;
            rarityFilters.put(player.getUniqueId(), f);
            openPage(player, 0);
            return;
        }

        if (e.getRawSlot() == LEVEL_FILTER_SLOT) {
            int f = levelFilters.getOrDefault(player.getUniqueId(), 5);
            if (e.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) f--; else f++;
            if (f < 0) f = 5; if (f > 5) f = 0;
            levelFilters.put(player.getUniqueId(), f);
            openPage(player, 0);
            return;
        }


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
            CustomItem template = ItemManager.getInstance().getTemplateById(templateId);
            if (template != null && template.isEgo()) {
                String key = template.getEgoKey();
                if (key != null) {
                    me.nakilex.levelplugin.ego.EgoWeapon proto =
                            me.nakilex.levelplugin.ego.EgoWeaponManager.getInstance().getPrototype(key);
                    if (proto != null) {
                        me.nakilex.levelplugin.ego.EgoWeapon weapon = proto.copy();
                        me.nakilex.levelplugin.ego.EgoWeaponManager.getInstance()
                                .setWeapon(player.getUniqueId(), weapon);
                        ItemStack toGive = me.nakilex.levelplugin.ego.EgoWeaponManager.getInstance()
                                .createWeaponItem(weapon, templateId);
                        player.getInventory().addItem(toGive);
                        player.sendMessage(ChatColor.GREEN + "You received: "
                                + toGive.getItemMeta().getDisplayName());
                        return;
                    }
                }
            }

            CustomItem instance = ItemManager.getInstance().rollNewInstance(templateId);
            ItemStack toGive = ItemUtil.createItemStackFromCustomItem(instance, 1, player);
            player.getInventory().addItem(toGive);
            player.sendMessage(ChatColor.GREEN + "You received: "
                    + toGive.getItemMeta().getDisplayName());
        }
    }
}
