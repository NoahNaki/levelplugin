// File: src/main/java/me/nakilex/levelplugin/runes/gui/RuneBrowser.java
package me.nakilex.levelplugin.runes.gui;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.runes.manager.RunesManager;
import me.nakilex.levelplugin.runes.model.Rune;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RuneBrowser implements CommandExecutor, Listener {
    private static final int ROWS = 6;
    private static final int COLS = 9;
    private static final int SIZE = ROWS * COLS;
    private static final int PAGE_SIZE = 28; // 4 rows × 7 cols of content
    private static final int RARITY_FILTER_SLOT = 48;
    private static final int CLASS_FILTER_SLOT = 50;
    private static final String[] CLASS_NAMES = {"MAGE", "ARCHER", "WARRIOR", "ROGUE", "ALL"};
    private static final String TITLE_PREFIX = ChatColor.GRAY + "Runes Browser - Page ";

    private final JavaPlugin plugin;
    private final RunesManager runesManager;
    private final NamespacedKey runeIdKey;
    private final java.util.Map<java.util.UUID, Integer> rarityFilters = new java.util.HashMap<>();
    private final java.util.Map<java.util.UUID, Integer> classFilters = new java.util.HashMap<>();

    public RuneBrowser(JavaPlugin plugin, RunesManager runesManager) {
        this.plugin = plugin;
        this.runesManager = runesManager;
        this.runeIdKey = new NamespacedKey(plugin, "rune_id");

        // register command and listener
        plugin.getCommand("runebrowser").setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private String title(int page) {
        return TITLE_PREFIX + (page + 1);
    }

    private static ItemStack createMenuItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(java.util.Arrays.asList(loreLines));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRarityButton(int filter) {
        ItemStack it = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Rarity Filter");
            java.util.List<String> lore = new java.util.ArrayList<>();
            Rune.Rarity[] arr = Rune.Rarity.values();
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

    private ItemStack createClassButton(int filter) {
        ItemStack it = new ItemStack(Material.BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Class Filter");
            java.util.List<String> lore = new java.util.ArrayList<>();
            for (int i = 0; i < CLASS_NAMES.length; i++) {
                String line = (i == filter ? ChatColor.GREEN : ChatColor.GRAY) + CLASS_NAMES[i];
                lore.add(line);
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private void openPage(Player player, int page) {
        Inventory gui = Bukkit.createInventory(null, SIZE, title(page));

        // 1) Fill background
        ItemStack filler = createMenuItem(Material.LIGHT_GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) gui.setItem(i, filler);

        // 2) Gather & sort all runes
        List<Rune> runes = new ArrayList<>(runesManager.getAllRunes());
        int rFilter = rarityFilters.getOrDefault(player.getUniqueId(), Rune.Rarity.values().length);
        int cFilter = classFilters.getOrDefault(player.getUniqueId(), CLASS_NAMES.length - 1);
        runes.removeIf(r -> (rFilter < Rune.Rarity.values().length && r.getRarity() != Rune.Rarity.values()[rFilter])
                || (cFilter < CLASS_NAMES.length - 1 && !r.getTargetClass().equalsIgnoreCase(CLASS_NAMES[cFilter])));
        runes.sort(Comparator.comparing((Rune r) -> r.getRarity().ordinal())
            .thenComparing(Rune::getDisplayName));
        int start = page * PAGE_SIZE;

        // 3) Populate grid
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= runes.size()) break;
            Rune rune = runes.get(idx);

            // a) Create preview ItemStack
            ItemStack preview = new ItemStack(Material.PAPER);
            ItemMeta pm = preview.getItemMeta();
            if (pm == null) continue;

            // b) Display name with rarity color
            ChatColor col = rune.getRarity().getColor();
            pm.setDisplayName(col + rune.getDisplayName());

            // c) Lore: spacer + description + rarity
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            rune.getDescription().forEach(line -> lore.add(ChatColor.GRAY + line));
            lore.add(" ");
            lore.add(col + "" + ChatColor.BOLD + rune.getRarity().name());
            pm.setLore(lore);

            // d) Persist the rune ID
            pm.getPersistentDataContainer()
                .set(runeIdKey, PersistentDataType.STRING, rune.getId());

            preview.setItemMeta(pm);

            // e) Place in GUI (4×7 grid centered)
            int row = 1 + (i / 7);
            int colIndex = 1 + (i % 7);
            gui.setItem(row * COLS + colIndex, preview);
        }

        // 4) Pagination controls
        ItemStack prev = createMenuItem(Material.ARROW, ChatColor.GREEN + "Previous Page");
        gui.setItem(SIZE - COLS, prev);
        ItemStack next = createMenuItem(Material.ARROW, ChatColor.GREEN + "Next Page");
        gui.setItem(SIZE - 1, next);

        gui.setItem(RARITY_FILTER_SLOT, createRarityButton(rFilter));
        gui.setItem(CLASS_FILTER_SLOT, createClassButton(cFilter));

        // 5) Open
        player.openInventory(gui);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can browse runes.");
            return true;
        }
        openPage((Player) sender, 0);
        return true;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        // 1) Only respond to our "Runes Browser" inventories:
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!e.getView().getTitle().startsWith(TITLE_PREFIX)) return;

        e.setCancelled(true);
        Player player = (Player)e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();
        int page = Integer.parseInt(e.getView().getTitle().substring(TITLE_PREFIX.length())) - 1;

        if (e.getRawSlot() == RARITY_FILTER_SLOT) {
            int f = rarityFilters.getOrDefault(player.getUniqueId(), Rune.Rarity.values().length);
            if (e.getClick() == ClickType.RIGHT) f--; else f++;
            int max = Rune.Rarity.values().length;
            if (f < 0) f = max; if (f > max) f = 0;
            rarityFilters.put(player.getUniqueId(), f);
            openPage(player, 0);
            return;
        }

        if (e.getRawSlot() == CLASS_FILTER_SLOT) {
            int f = classFilters.getOrDefault(player.getUniqueId(), CLASS_NAMES.length - 1);
            if (e.getClick() == ClickType.RIGHT) f--; else f++;
            int max = CLASS_NAMES.length - 1;
            if (f < 0) f = max; if (f > max) f = 0;
            classFilters.put(player.getUniqueId(), f);
            openPage(player, 0);
            return;
        }

        if (name.equals(ChatColor.GREEN + "Next Page")) {
            openPage(player, page + 1);
            return;
        }
        if (name.equals(ChatColor.GREEN + "Previous Page")) {
            if (page > 0) openPage(player, page - 1);
            return;
        }

        // 2) Otherwise this must be one of our rune-preview papers:
        // Pull the rune-id string out of the PDC under runeIdKey:
        String runeId = clicked.getItemMeta()
            .getPersistentDataContainer()
            .get(runeIdKey, PersistentDataType.STRING);
        if (runeId == null) {
            player.sendMessage(ChatColor.RED + "This is not a rune!");
            return;
        }

        // 3) Look it up:
        Rune rune = runesManager.getRuneById(runeId);
        if (rune == null) {
            player.sendMessage(ChatColor.RED + "That rune no longer exists!");
            return;
        }

        // 4) Give the player an already identified rune item
        ItemStack identified = runesManager.createIdentifiedRuneItem(rune);
        player.getInventory().addItem(identified);
        player.sendMessage(ChatColor.GREEN + "You received: "
            + identified.getItemMeta().getDisplayName());
    }
}

