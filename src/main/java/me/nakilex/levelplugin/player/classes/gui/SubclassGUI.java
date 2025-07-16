package me.nakilex.levelplugin.player.classes.gui;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.spells.managers.SpellManager;
import me.nakilex.levelplugin.spells.Spell;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Sound;

import java.util.*;

/**
 * GUI for selecting unlocked subclasses. Supports sorting and pagination
 * similar to the storage GUI style.
 */
public class SubclassGUI implements Listener {

    public static final String TITLE = ChatColor.DARK_AQUA + "Select Class";

    private static final int GUI_SIZE = 54; // full chest
    private static final int[] CLASS_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int ITEMS_PER_PAGE = CLASS_SLOTS.length;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int FILTER_SLOT = 48;
    private static final int SORT_SLOT = 50;
    private static final int INFO_SLOT = 8;

    private static final ItemStack FILLER;

    static {
        FILLER = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = FILLER.getItemMeta();
        if (fm != null) { fm.setDisplayName(" "); FILLER.setItemMeta(fm); }
    }

    private static final Map<UUID, Inventory> OPEN = new HashMap<>();
    static final Map<UUID, Integer> PAGE = new HashMap<>();
    static final Map<UUID, Integer> FILTER = new HashMap<>();
    static final Map<UUID, Integer> SORT = new HashMap<>();
    private static final Map<UUID, List<PlayerClass>> PAGE_CONTENT = new HashMap<>();

    public static void open(Player player) {
        int page = PAGE.getOrDefault(player.getUniqueId(), 0);
        open(player, page);
    }

    static void open(Player player, int page) {
        PAGE.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, TITLE);

        for (int i = 0; i < GUI_SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, FILLER);
            }
        }

        StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
        List<PlayerClass> list = new ArrayList<>();
        for (PlayerClass pc : PlayerClass.values()) {
            if (pc != PlayerClass.VILLAGER) list.add(pc);
        }

        int filter = FILTER.getOrDefault(player.getUniqueId(), 0);
        int sort = SORT.getOrDefault(player.getUniqueId(), 0);

        // sort
        if (sort == 1) {
            list.sort(Comparator
                    .comparing((PlayerClass pc) -> !ps.unlockedClasses.contains(pc))
                    .thenComparing(pc -> pc.name().toLowerCase()));
        } else {
            list.sort(Comparator.comparing(pc -> pc.name().toLowerCase()));
        }

        // filter
        if (filter == 1) {
            list.removeIf(pc -> !ps.unlockedClasses.contains(pc));
        }

        int start = page * ITEMS_PER_PAGE;
        List<PlayerClass> pageList = new ArrayList<>();
        for (int i = start, slot = 0; i < list.size() && slot < ITEMS_PER_PAGE; i++) {
            PlayerClass pc = list.get(i);
            boolean unlocked = ps.unlockedClasses.contains(pc);
            ItemStack item = unlocked ? createItem(pc) : createLockedItem(pc);
            inv.setItem(CLASS_SLOTS[slot++], item);
            pageList.add(pc);
        }
        PAGE_CONTENT.put(player.getUniqueId(), pageList);

        if (page > 0) inv.setItem(PREV_SLOT, getNexoItem("arrow_left", ChatColor.GREEN + "Previous"));
        if (list.size() > (page + 1) * ITEMS_PER_PAGE) inv.setItem(NEXT_SLOT, getNexoItem("arrow_right", ChatColor.GREEN + "Next"));
        inv.setItem(FILTER_SLOT, createFilterButton(filter));
        inv.setItem(SORT_SLOT, createSortButton(sort));
        inv.setItem(INFO_SLOT, getNexoItem("info", ChatColor.YELLOW + "Information"));

        OPEN.put(player.getUniqueId(), inv);
        player.openInventory(inv);
    }

    private static final class Rating {
        final int dmg, def, mob, util;
        Rating(int d, int f, int m, int u) { this.dmg = d; this.def = f; this.mob = m; this.util = u; }
    }

    private static final Map<PlayerClass, Rating> CLASS_RATINGS = Map.ofEntries(
            Map.entry(PlayerClass.WARRIOR, new Rating(4,4,3,2)),
            Map.entry(PlayerClass.ROGUE, new Rating(3,2,5,3)),
            Map.entry(PlayerClass.MAGE, new Rating(5,1,3,5)),
            Map.entry(PlayerClass.CLERIC, new Rating(2,4,2,4)),
            Map.entry(PlayerClass.BARBARIAN, new Rating(5,3,3,2)),
            Map.entry(PlayerClass.DRAGONIAN, new Rating(5,3,3,3)),
            Map.entry(PlayerClass.GALEGLAIVE, new Rating(4,2,5,3)),
            Map.entry(PlayerClass.DEATHKNIGHT, new Rating(5,4,2,3)),
            Map.entry(PlayerClass.ARCTICKNIGHT, new Rating(4,4,2,3)),
            Map.entry(PlayerClass.DRAGONWARRIOR, new Rating(5,4,3,2)),
            Map.entry(PlayerClass.OVERLORD, new Rating(5,3,4,5)),
            Map.entry(PlayerClass.ARCHER, new Rating(3,2,4,3)),
            Map.entry(PlayerClass.DEADEYE, new Rating(4,2,4,3)),
            Map.entry(PlayerClass.PHOENIXHUNTER, new Rating(5,2,4,4)),
            Map.entry(PlayerClass.PALADIN, new Rating(4,5,2,3)),
            Map.entry(PlayerClass.ABYSSION, new Rating(4,4,3,3))
    );

    private static Map<String, String> SPELL_USAGE;

    static {
        try {
            var f = me.nakilex.levelplugin.spells.gui.SpellGUI.class.getDeclaredField("SPELL_USAGE");
            f.setAccessible(true);
            SPELL_USAGE = (Map<String, String>) f.get(null);
        } catch (Exception e) {
            SPELL_USAGE = Collections.emptyMap();
        }
    }

    private static final Map<PlayerClass, String> CLASS_SUMMARY = Map.ofEntries(
            Map.entry(PlayerClass.WARRIOR, "Close range fighter with charge and hook combos."),
            Map.entry(PlayerClass.ROGUE, "Swift assassin with high mobility skills."),
            Map.entry(PlayerClass.MAGE, "Master of elemental magic with powerful spells."),
            Map.entry(PlayerClass.CLERIC, "Support class able to heal and shield allies."),
            Map.entry(PlayerClass.BARBARIAN, "Ferocious warrior using leaps and furious blows."),
            Map.entry(PlayerClass.DRAGONIAN, "Dragon-blooded fighter wielding breath attacks."),
            Map.entry(PlayerClass.GALEGLAIVE, "Agile windblade user with swift strikes."),
            Map.entry(PlayerClass.DEATHKNIGHT, "Dark knight controlling necrotic power."),
            Map.entry(PlayerClass.ARCTICKNIGHT, "Frost warrior unleashing icy attacks."),
            Map.entry(PlayerClass.DRAGONWARRIOR, "Hybrid dragon warrior channeling draconic energy."),
            Map.entry(PlayerClass.OVERLORD, "Arcane caster wielding forbidden spells."),
            Map.entry(PlayerClass.ARCHER, "Experimental archer harnessing drones."),
            Map.entry(PlayerClass.DEADEYE, "Sharpshooter wielding pistols and explosives."),
            Map.entry(PlayerClass.PHOENIXHUNTER, "Flame archer empowered by the phoenix."),
            Map.entry(PlayerClass.PALADIN, "Holy fighter boasting strong defence."),
            Map.entry(PlayerClass.ABYSSION, "Tide-wielding swordsman controlling water."));

    private static String bar(int val) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < val; i++) sb.append(ChatColor.GOLD).append("■");
        for (int i = val; i < 5; i++) sb.append(ChatColor.DARK_GRAY).append("■");
        return sb.toString();
    }

    private static Rating rating(PlayerClass pc) {
        return CLASS_RATINGS.getOrDefault(pc, new Rating(3,3,3,3));
    }

    private static String ratingLine(ChatColor color, String label, int val) {
        return color + label + ":\t" + bar(val);
    }

    private static String formatClassName(PlayerClass pc) {
        String raw = pc.name().toLowerCase();
        StringBuilder out = new StringBuilder(raw.length());
        boolean cap = true;
        for (char c : raw.toCharArray()) {
            if (c == '_' || c == ' ') {
                out.append(' ');
                cap = true;
            } else if (cap) {
                out.append(Character.toUpperCase(c));
                cap = false;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static ItemStack createItem(PlayerClass pc) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + formatClassName(pc));
            List<String> lore = new ArrayList<>();

            String summary = CLASS_SUMMARY.get(pc);
            if (summary != null) {
                lore.add(ChatColor.GRAY + summary);
                lore.add(" ");
            }

            Map<String, Spell> spellMap = SpellManager.getInstance()
                    .getSpellsByClass(pc.name().toLowerCase());
            List<Spell> spells = new ArrayList<>(spellMap.values());
            if (spells.isEmpty()) {
                lore.add(" ");
                lore.add(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Basic Attack" + ChatColor.RESET
                        + ChatColor.WHITE + " - " + ChatColor.GRAY + "Left Click");
            } else {
                spells.sort(Comparator.comparingInt(Spell::getLevelReq));
                lore.add(" ");
                for (Spell sp : spells) {
                    String usage;
                    if ("BASIC_ATTACK".equalsIgnoreCase(sp.getCombo())) {
                        usage = "Left Click";
                    } else {
                        usage = SPELL_USAGE.getOrDefault(sp.getId(),
                                sp.getCombo().replace("L", "Left").replace("R", "Right"));
                    }
                    lore.add(ChatColor.YELLOW.toString() + ChatColor.BOLD + sp.getDisplayName()
                            + ChatColor.RESET + ChatColor.WHITE + " - " + ChatColor.GRAY + usage);
                }
            }

            Rating r = rating(pc);
            lore.add(" ");
            lore.add(ratingLine(ChatColor.RED, "Damage", r.dmg));
            lore.add(ratingLine(ChatColor.BLUE, "Defence", r.def));
            lore.add(ratingLine(ChatColor.GREEN, "Mobility", r.mob));
            lore.add(ratingLine(ChatColor.YELLOW, "Utility", r.util));
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Click " + ChatColor.GRAY + "to select this class!");

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack lockItem() {
        ItemBuilder b = NexoItems.itemFromId("lock");
        ItemStack item = b != null ? b.build() : new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Locked");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createLockedItem(PlayerClass pc) {
        ItemStack item = lockItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "???");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack getNexoItem(String id, String name) {
        ItemBuilder b = NexoItems.itemFromId(id);
        ItemStack it = b == null ? new ItemStack(Material.BARRIER) : b.build();
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); it.setItemMeta(meta); }
        return it;
    }

    private static ItemStack createFilterButton(int mode) {
        ItemStack it = new ItemStack(Material.HOPPER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Filter");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_GRAY + "Filter the classes");
            lore.add(" ");
            String[] opts = {"Show All", "Unlocked"};
            for (int i = 0; i < opts.length; i++) {
                lore.add(rangeLine(i, mode, opts[i]));
            }
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Click to cycle");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static ItemStack createSortButton(int mode) {
        ItemStack it = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Sorting");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "");
            lore.add(ChatColor.DARK_GRAY + "Sort the classes");
            lore.add(" ");
            String[] opts = {"A-Z", "Unlocked First"};
            for (int i = 0; i < opts.length; i++) {
                lore.add(rangeLine(i, mode, opts[i]));
            }
            lore.add(" ");
            lore.add(ChatColor.WHITE + "Click to cycle");
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static String rangeLine(int index, int current, String label) {
        ChatColor color = index == current ? ChatColor.WHITE : ChatColor.GRAY;
        ChatColor bullet = index == current ? ChatColor.GREEN : ChatColor.DARK_GRAY;
        return bullet + "- " + color + label;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        Inventory open = OPEN.get(player.getUniqueId());
        if (open == null || !e.getView().getTopInventory().equals(open)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot == PREV_SLOT) {
            int p = PAGE.getOrDefault(player.getUniqueId(), 0);
            open(player, Math.max(0, p - 1));
            return;
        }
        if (slot == NEXT_SLOT) {
            int p = PAGE.getOrDefault(player.getUniqueId(), 0);
            open(player, p + 1);
            return;
        }
        if (slot == FILTER_SLOT) {
            int f = FILTER.getOrDefault(player.getUniqueId(), 0);
            f = (f + 1) % 2;
            FILTER.put(player.getUniqueId(), f);
            open(player, PAGE.getOrDefault(player.getUniqueId(), 0));
            return;
        }
        if (slot == SORT_SLOT) {
            int s = SORT.getOrDefault(player.getUniqueId(), 0);
            s = (s + 1) % 2;
            SORT.put(player.getUniqueId(), s);
            open(player, PAGE.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        List<PlayerClass> current = PAGE_CONTENT.getOrDefault(player.getUniqueId(), Collections.emptyList());
        for (int i = 0; i < current.size() && i < CLASS_SLOTS.length; i++) {
            if (slot == CLASS_SLOTS[i]) {
                PlayerClass pc = current.get(i);
                StatsManager.PlayerStats ps = StatsManager.getInstance().getPlayerStats(player.getUniqueId());
                if (ps.unlockedClasses.contains(pc)) {
                    ps.playerClass = pc;
                    ChatFormatter.constructDivider(player, "§6§l-", 45);
                    ChatFormatter.sendCenteredMessage(player, "§6§lCLASS SELECTED!");
                    ChatFormatter.sendCenteredMessage(player, "");
                    ChatFormatter.sendCenteredMessage(player,
                            "§7You are now the §e§l" + pc.name() + " §7class!");
                    ChatFormatter.sendCenteredMessage(player, "");
                    ChatFormatter.constructDivider(player, "§6§l-", 45);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    player.closeInventory();
                } else {
                    player.sendMessage(ChatColor.RED + "You cannot select that class!");
                }
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory open = OPEN.get(e.getPlayer().getUniqueId());
        if (open != null && e.getInventory().equals(open)) {
            OPEN.remove(e.getPlayer().getUniqueId());
            PAGE_CONTENT.remove(e.getPlayer().getUniqueId());
        }
    }
}

