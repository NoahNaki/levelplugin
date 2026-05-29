package me.nakilex.levelplugin.items.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.v2.ItemDefinition;
import me.nakilex.levelplugin.items.v2.ItemRegistry;
import me.nakilex.levelplugin.items.v2.ItemStatType;
import me.nakilex.levelplugin.items.v2.ItemType;
import me.nakilex.levelplugin.items.v2.StatValue;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.managers.PlayerClassManager;
import me.nakilex.levelplugin.utils.GuiUtil;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ItemsBrowser implements CommandExecutor, Listener {
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS;
    private static final int PAGE_SIZE = 28; // 4 rows × 7 cols of content

    private static final int TYPE_FILTER_SLOT   = 46;
    private static final int RARITY_FILTER_SLOT = 48;
    private static final int LEVEL_FILTER_SLOT  = 50;

    private static final List<ItemStatType> STAT_ORDER = List.of(
            ItemStatType.HP,
            ItemStatType.DEF,
            ItemStatType.STR,
            ItemStatType.AGI,
            ItemStatType.INTEL,
            ItemStatType.DEX,
            ItemStatType.WIL,
            ItemStatType.TEC
    );

    private final JavaPlugin plugin;
    private final java.util.Map<java.util.UUID, Integer> typeFilters = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Integer> rarityFilters = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Integer> levelFilters = new java.util.HashMap<>();

    public ItemsBrowser(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("itemsbrowser").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private String title(int page) {
        return ChatColor.BLACK + "Items Browser - Page " + (page + 1);
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
        if (m != null) {
            m.setDisplayName(name);
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack createTypeButton(int filter) {
        ItemStack it = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Type Filter");
            List<String> lore = new ArrayList<>();
            String[] types = {"WEAPON", "ARMOR", "OTHER", "ALL"};
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
            ItemRarity[] arr = ItemRarity.values();
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
            String[] ranges = {"Lv. 1-19", "Lv. 20-39", "Lv. 40-59", "Lv. 60-79", "Lv. 80+", "ALL"};
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

        ItemRegistry registry = Main.getInstance().getItemRegistryV2();
        List<Integer> ids = new ArrayList<>(registry.getAll().keySet());
        Collections.sort(ids);

        int tFilter = typeFilters.getOrDefault(player.getUniqueId(), 3);
        int rFilter = rarityFilters.getOrDefault(player.getUniqueId(), ItemRarity.values().length);
        int lFilter = levelFilters.getOrDefault(player.getUniqueId(), 5);

        List<ItemDefinition> templates = new ArrayList<>();
        for (int id : ids) {
            ItemDefinition tpl = registry.get(id).orElse(null);
            if (tpl == null) continue;
            if (rFilter < ItemRarity.values().length && tpl.rarity() != ItemRarity.values()[rFilter]) {
                continue;
            }
            if (lFilter < 5) {
                int lvl = tpl.requirements().level();
                int min = lFilter * 20 + 1;
                int max = lFilter == 4 ? 999 : min + 19;
                if (lvl < min || lvl > max) continue;
            }
            if (!matchesTypeFilter(tpl, tFilter)) {
                continue;
            }
            templates.add(tpl);
        }

        int start = page * PAGE_SIZE;

        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= templates.size()) break;

            ItemDefinition tpl = templates.get(idx);
            if (tpl == null) continue;

            ItemStack preview = new ItemStack(tpl.visuals().baseMaterial(), 1);
            ItemMeta pm = preview.getItemMeta();
            if (pm == null) continue;

            ChatColor col = tpl.rarity().getColor();
            pm.setDisplayName(col + tpl.name());

            List<String> lore = new ArrayList<>();
            lore.add("");

            int playerLvl = StatsManager.getInstance().getLevel(player);
            boolean lvlOk = playerLvl >= tpl.requirements().level();
            lore.add((lvlOk ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                    + ChatColor.GRAY + "Level Requirement: "
                    + ChatColor.WHITE + tpl.requirements().level());

            if (!tpl.requirements().classes().isEmpty()) {
                PlayerClass playerClass = PlayerClassManager.getInstance().getPlayerClass(player);
                boolean classOk = tpl.requirements().classes().contains(playerClass);
                String classList = tpl.requirements().classes().stream()
                        .map(PlayerClass::getDisplayName)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("None");
                lore.add((classOk ? ChatColor.GREEN + "✔ " : ChatColor.RED + "✘ ")
                        + ChatColor.GRAY + "Class Requirement: "
                        + ChatColor.WHITE + classList);
            }

            lore.add("");

            int minGs = 0;
            int maxGs = 0;
            for (StatValue value : tpl.stats().values()) {
                minGs += (int) Math.round(value.min());
                maxGs += (int) Math.round(value.max());
            }
            String gsDisplay = (minGs == maxGs)
                    ? String.valueOf(minGs)
                    : (minGs + "-" + maxGs);
            lore.add(ChatColor.GRAY + "Gear Score: "
                    + ChatColor.LIGHT_PURPLE + ChatColor.BOLD + gsDisplay);
            lore.add("");

            Map<ItemStatType, String> statLabels = statLabels();
            for (ItemStatType stat : STAT_ORDER) {
                StatValue value = tpl.stats().get(stat);
                if (value == null) {
                    continue;
                }
                ChatColor valueColor = stat == ItemStatType.HP ? ChatColor.RED : ChatColor.GREEN;
                lore.add(statLabels.get(stat) + ": " + valueColor + "+" + value.formatForLore());
            }

            lore.add("");
            lore.add(col + "" + ChatColor.BOLD + tpl.rarity().name());

            pm.setLore(lore);
            pm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
            pm.setUnbreakable(true);
            pm.getPersistentDataContainer().set(ItemUtil.ITEM_ID_KEY, PersistentDataType.INTEGER, tpl.id());
            preview.setItemMeta(pm);
            ItemUtil.applyRarityTooltipStyle(preview, tpl.rarity());

            int row = 1 + (i / 7);
            int colIndex = 1 + (i % 7);
            gui.setItem(row * COLS + colIndex, preview);
        }

        ItemStack prev = getNexoItem("arrow_left", ChatColor.GREEN + "Previous Page");
        gui.setItem(SIZE - COLS, prev);
        ItemStack next = getNexoItem("arrow_right", ChatColor.GREEN + "Next Page");
        gui.setItem(SIZE - 1, next);

        gui.setItem(TYPE_FILTER_SLOT, createTypeButton(tFilter));
        gui.setItem(RARITY_FILTER_SLOT, createRarityButton(rFilter));
        gui.setItem(LEVEL_FILTER_SLOT, createLevelButton(lFilter));

        player.openInventory(gui);
    }

    private static boolean matchesTypeFilter(ItemDefinition definition, int filter) {
        ItemType type = definition.type();
        return switch (filter) {
            case 0 -> type == ItemType.WEAPON;
            case 1 -> type == ItemType.ARMOR;
            case 2 -> type != ItemType.WEAPON && type != ItemType.ARMOR;
            default -> true;
        };
    }

    private static Map<ItemStatType, String> statLabels() {
        Map<ItemStatType, String> labels = new EnumMap<>(ItemStatType.class);
        labels.put(ItemStatType.HP, ChatColor.GRAY + "HP");
        labels.put(ItemStatType.DEF, ChatColor.GRAY + "Defense");
        labels.put(ItemStatType.STR, GuiUtil.formatStatName(StatsManager.StatType.STR));
        labels.put(ItemStatType.AGI, GuiUtil.formatStatName(StatsManager.StatType.AGI));
        labels.put(ItemStatType.INTEL, GuiUtil.formatStatName(StatsManager.StatType.INT));
        labels.put(ItemStatType.DEX, GuiUtil.formatStatName(StatsManager.StatType.DEX));
        labels.put(ItemStatType.WIL, GuiUtil.formatStatName(StatsManager.StatType.WIL));
        labels.put(ItemStatType.TEC, GuiUtil.formatStatName(StatsManager.StatType.TEC));
        return labels;
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
        if (!ChatColor.stripColor(e.getView().getTitle()).startsWith("Items Browser")) return;
        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();
        String stripped = ChatColor.stripColor(e.getView().getTitle());
        int currentPage = Integer.parseInt(stripped.split(" ")[stripped.split(" ").length - 1]) - 1;
        int tFilter = typeFilters.getOrDefault(player.getUniqueId(), 3);
        int rFilter = rarityFilters.getOrDefault(player.getUniqueId(), ItemRarity.values().length);
        int lFilter = levelFilters.getOrDefault(player.getUniqueId(), 5);

        int total = 0;
        ItemRegistry registry = Main.getInstance().getItemRegistryV2();
        for (ItemDefinition ci : registry.getAll().values()) {
            if (rFilter < ItemRarity.values().length && ci.rarity() != ItemRarity.values()[rFilter]) continue;
            if (lFilter < 5) {
                int lvl = ci.requirements().level();
                int min = lFilter * 20 + 1;
                int max = lFilter == 4 ? 999 : min + 19;
                if (lvl < min || lvl > max) continue;
            }
            if (!matchesTypeFilter(ci, tFilter)) continue;
            total++;
        }
        int maxPage = (Math.max(total, 1) - 1) / PAGE_SIZE;

        if (e.getRawSlot() == TYPE_FILTER_SLOT) {
            int f = typeFilters.getOrDefault(player.getUniqueId(), 3);
            if (e.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) f--; else f++;
            if (f < 0) f = 3;
            if (f > 3) f = 0;
            typeFilters.put(player.getUniqueId(), f);
            openPage(player, 0);
            return;
        }

        if (e.getRawSlot() == RARITY_FILTER_SLOT) {
            int f = rarityFilters.getOrDefault(player.getUniqueId(), ItemRarity.values().length);
            if (e.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) f--; else f++;
            int max = ItemRarity.values().length;
            if (f < 0) f = max;
            if (f > max) f = 0;
            rarityFilters.put(player.getUniqueId(), f);
            openPage(player, 0);
            return;
        }

        if (e.getRawSlot() == LEVEL_FILTER_SLOT) {
            int f = levelFilters.getOrDefault(player.getUniqueId(), 5);
            if (e.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) f--; else f++;
            if (f < 0) f = 5;
            if (f > 5) f = 0;
            levelFilters.put(player.getUniqueId(), f);
            openPage(player, 0);
            return;
        }

        if (name.equals(ChatColor.GREEN + "Next Page")) {
            int nextPage = currentPage < maxPage ? currentPage + 1 : 0;
            openPage(player, nextPage);
            return;
        }

        if (name.equals(ChatColor.GREEN + "Previous Page")) {
            int prevPage = currentPage > 0 ? currentPage - 1 : maxPage;
            openPage(player, prevPage);
            return;
        }

        int templateId = ItemUtil.getCustomItemId(clicked);
        if (templateId == -1) {
            return;
        }
        me.nakilex.levelplugin.items.data.CustomItem instance = ItemManager.getInstance().rollNewInstance(templateId);
        if (instance == null) {
            return;
        }
        ItemStack toGive = ItemUtil.createItemStackFromCustomItem(instance, 1, player);
        player.getInventory().addItem(toGive);
        player.sendMessage(ChatColor.GREEN + "You received: "
                + toGive.getItemMeta().getDisplayName());
    }
}
