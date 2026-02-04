package me.nakilex.levelplugin.player.classes.gui;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
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

    public static final String TITLE = "Select Class";

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
    private static final Map<UUID, List<GuiWidget>> WIDGETS = new HashMap<>();

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
            pageList.add(pc);
            slot++;
        }
        PAGE_CONTENT.put(player.getUniqueId(), pageList);

        OPEN.put(player.getUniqueId(), inv);
        List<GuiWidget> widgets = buildWidgets(player, ps, list.size(), page, filter, sort, pageList);
        WIDGETS.put(player.getUniqueId(), widgets);
        renderWidgets(inv, player, widgets);
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
            Map.entry(PlayerClass.WITCH, new Rating(5,3,3,5)),
            Map.entry(PlayerClass.BARBARIAN, new Rating(5,3,3,2)),
            Map.entry(PlayerClass.DRAGONIAN, new Rating(5,3,3,3)),
            Map.entry(PlayerClass.GALEGLAIVE, new Rating(4,2,5,3)),
            Map.entry(PlayerClass.DEATHKNIGHT, new Rating(5,4,2,3)),
            Map.entry(PlayerClass.ARCTICKNIGHT, new Rating(4,4,2,3)),
            Map.entry(PlayerClass.DRAGONWARRIOR, new Rating(5,4,3,2)),
            Map.entry(PlayerClass.AWAKROGUE, new Rating(5,3,5,3)),
            Map.entry(PlayerClass.AWAKWARRIOR, new Rating(5,4,3,3)),
            Map.entry(PlayerClass.AWAKARCHER, new Rating(4,2,4,4)),
            Map.entry(PlayerClass.ARCHMAGE, new Rating(5,2,3,5)),
            Map.entry(PlayerClass.ARCHER, new Rating(3,2,4,3)),
            Map.entry(PlayerClass.DEADEYE, new Rating(4,2,4,3)),
            Map.entry(PlayerClass.PHOENIXHUNTER, new Rating(5,2,4,4)),
            Map.entry(PlayerClass.PALADIN, new Rating(4,5,2,3)),
            Map.entry(PlayerClass.ABYSSION, new Rating(4,4,3,3))
    );

    private static final Map<PlayerClass, String> CLASS_SUMMARY = Map.ofEntries(
            Map.entry(PlayerClass.WARRIOR, "Close range fighter with charge and hook combos."),
            Map.entry(PlayerClass.ROGUE, "Swift rogue with high mobility skills."),
            Map.entry(PlayerClass.MAGE, "Master of elemental magic with powerful spells."),
            Map.entry(PlayerClass.CLERIC, "Support class able to heal and shield allies."),
            Map.entry(PlayerClass.WITCH, "Mystic caster wielding curses and dark arts."),
            Map.entry(PlayerClass.BARBARIAN, "Ferocious warrior using leaps and furious blows."),
            Map.entry(PlayerClass.DRAGONIAN, "Dragon-blooded fighter wielding breath attacks."),
            Map.entry(PlayerClass.GALEGLAIVE, "Agile windblade user with swift strikes."),
            Map.entry(PlayerClass.DEATHKNIGHT, "Dark knight controlling necrotic power."),
            Map.entry(PlayerClass.ARCTICKNIGHT, "Frost warrior unleashing icy attacks."),
            Map.entry(PlayerClass.DRAGONWARRIOR, "Hybrid dragon warrior channeling draconic energy."),
            Map.entry(PlayerClass.AWAKROGUE, "Awakened rogue harnessing lethal combos and shadow power."),
            Map.entry(PlayerClass.AWAKWARRIOR, "Awakened warrior wielding brutal combos and barriers."),
            Map.entry(PlayerClass.AWAKARCHER, "Awakened archer unleashing explosive arrow storms."),
            Map.entry(PlayerClass.ARCHMAGE, "Archmage wielding arcane storms and void chains."),
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

            lore.add(" ");
            lore.add(ChatColor.YELLOW.toString() + ChatColor.BOLD + "Basic Attack" + ChatColor.RESET
                    + ChatColor.WHITE + " - " + ChatColor.GRAY + "Left Click");

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
            lore.addAll(TooltipUtil.clickInstructions("to cycle", null));
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
            lore.addAll(TooltipUtil.clickInstructions("to cycle", null));
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
        if (handleWidgetClick(e, player)) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Inventory open = OPEN.get(e.getPlayer().getUniqueId());
        if (open != null && e.getInventory().equals(open)) {
            OPEN.remove(e.getPlayer().getUniqueId());
            PAGE_CONTENT.remove(e.getPlayer().getUniqueId());
            WIDGETS.remove(e.getPlayer().getUniqueId());
        }
    }

    private static List<GuiWidget> buildWidgets(Player player,
                                                StatsManager.PlayerStats ps,
                                                int totalClasses,
                                                int page,
                                                int filter,
                                                int sort,
                                                List<PlayerClass> pageList) {
        List<GuiWidget> widgets = new ArrayList<>();
        for (int i = 0; i < pageList.size() && i < CLASS_SLOTS.length; i++) {
            int slot = CLASS_SLOTS[i];
            PlayerClass pc = pageList.get(i);
            widgets.add(new ActionWidget(slot,
                    context -> ps.unlockedClasses.contains(pc) ? createItem(pc) : createLockedItem(pc),
                    (click, context) -> handleClassClick(context.player(), ps, pc)));
        }

        if (page > 0) {
            widgets.add(new ActionWidget(PREV_SLOT,
                    context -> GuiUtil.getNexoItem("arrow_left", ChatColor.GREEN + "Previous"),
                    (click, context) -> open(context.player(), Math.max(0, page - 1))));
        }
        if (totalClasses > (page + 1) * ITEMS_PER_PAGE) {
            widgets.add(new ActionWidget(NEXT_SLOT,
                    context -> GuiUtil.getNexoItem("arrow_right", ChatColor.GREEN + "Next"),
                    (click, context) -> open(context.player(), page + 1)));
        }
        widgets.add(new ActionWidget(FILTER_SLOT,
                context -> createFilterButton(filter),
                (click, context) -> {
                    int updated = (filter + 1) % 2;
                    FILTER.put(context.player().getUniqueId(), updated);
                    open(context.player(), PAGE.getOrDefault(context.player().getUniqueId(), 0));
                }));
        widgets.add(new ActionWidget(SORT_SLOT,
                context -> createSortButton(sort),
                (click, context) -> {
                    int updated = (sort + 1) % 2;
                    SORT.put(context.player().getUniqueId(), updated);
                    open(context.player(), PAGE.getOrDefault(context.player().getUniqueId(), 0));
                }));
        widgets.add(new ActionWidget(INFO_SLOT,
                context -> GuiUtil.getNexoItem("info", ChatColor.YELLOW + "Information"),
                null));
        return widgets;
    }

    private static void handleClassClick(Player player, StatsManager.PlayerStats ps, PlayerClass pc) {
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
    }

    private static boolean handleWidgetClick(InventoryClickEvent event, Player player) {
        List<GuiWidget> widgets = WIDGETS.get(player.getUniqueId());
        if (widgets == null) {
            return false;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return false;
        }
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return false;
        }
        event.setCancelled(true);
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
        return true;
    }

    private static void renderWidgets(Inventory inventory, Player player, List<GuiWidget> widgets) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }
}
