package me.nakilex.levelplugin.fasttravel.gui;

import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FastTravelGUI implements Listener {
    private static final String TITLE = ChatColor.DARK_AQUA + "Fast Travel";
    private static final int SIZE = 54;
    private static final int[] POINT_SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int ITEMS_PER_PAGE = POINT_SLOTS.length;
    private static final int PREV_PAGE = 45;
    private static final int NEXT_PAGE = 53;
    private static final int SORT_SLOT = 50;

    private final JavaPlugin plugin;
    private final FastTravelManager manager;
    private final EconomyManager economy;
    private final Map<Player, Inventory> open = new HashMap<>();
    private final Map<UUID, Integer> pageMap = new HashMap<>();
    private final Map<UUID, Integer> sortMap = new HashMap<>();

    public FastTravelGUI(JavaPlugin plugin, FastTravelManager manager, EconomyManager economy) {
        this.plugin = plugin;
        this.manager = manager;
        this.economy = economy;
    }
    public void open(Player player) {
        open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
    }

    private void open(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        Inventory gui = Bukkit.createInventory(null, SIZE, TITLE);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta filMeta = filler.getItemMeta();
        if (filMeta != null) { filMeta.setDisplayName(" "); filler.setItemMeta(filMeta); }
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, filler);
        }

        java.util.List<FastTravelPoint> list = new java.util.ArrayList<>();
        for (FastTravelPoint pt : manager.getPoints()) if (pt.isTown()) list.add(pt);

        int sort = sortMap.getOrDefault(player.getUniqueId(), 0);
        sortPoints(list, sort, player);

        int start = page * ITEMS_PER_PAGE;
        int slot = 0;
        for (int i = start; i < list.size() && slot < ITEMS_PER_PAGE; i++) {
            FastTravelPoint pt = list.get(i);
            boolean unlocked = manager.isUnlocked(player, pt.getName());
            ItemStack item = new ItemStack(unlocked ? Material.LODESTONE : Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(pt.getColor() + "" + ChatColor.BOLD + pt.getName());
                java.util.List<String> lore = new java.util.ArrayList<>();
                lore.add(ChatColor.GRAY + pt.getDescription());
                if (unlocked) {
                    int cost = (int) player.getLocation().distance(pt.getLocation());
                    lore.add(ChatColor.GRAY + "Teleportation Cost:");
                    lore.add(ChatColor.WHITE + "" + cost + ChatColor.YELLOW + " ⛃");
                } else {
                    lore.add(ChatColor.DARK_GRAY + "Locked");
                }
                meta.setLore(lore);
                meta.setLocalizedName(pt.getName().toLowerCase());
                item.setItemMeta(meta);
            }
            gui.setItem(POINT_SLOTS[slot++], item);
        }

        if (page > 0) gui.setItem(PREV_PAGE, getArrow(ChatColor.GREEN + "Previous", false));
        if (list.size() > (page + 1) * ITEMS_PER_PAGE) gui.setItem(NEXT_PAGE, getArrow(ChatColor.GREEN + "Next", true));
        gui.setItem(SORT_SLOT, createSortButton(sort));

        open.put(player, gui);
        player.openInventory(gui);
    }

    
    
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inv = open.get(player);
        if (inv == null || !inv.equals(event.getInventory())) return;
        event.setCancelled(true);
        int raw = event.getRawSlot();
        if (raw == PREV_PAGE) {
            int page = Math.max(0, pageMap.getOrDefault(player.getUniqueId(), 0) - 1);
            open(player, page);
            return;
        }
        if (raw == NEXT_PAGE) {
            int page = pageMap.getOrDefault(player.getUniqueId(), 0) + 1;
            open(player, page);
            return;
        }
        if (raw == SORT_SLOT) {
            int mode = sortMap.getOrDefault(player.getUniqueId(), 0);
            mode = (mode + 1) % 4;
            sortMap.put(player.getUniqueId(), mode);
            open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        if (raw < 0 || raw >= inv.getSize()) return;
        ItemStack clicked = inv.getItem(raw);
        if (clicked == null || clicked.getType() == Material.BARRIER) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        String key = meta.getLocalizedName();
        FastTravelPoint target = manager.getPoint(key);
        if (!manager.isUnlocked(player, target.getName())) return;
        int cost = (int) player.getLocation().distance(target.getLocation());
        if (economy.getBalance(player) < cost) {
            player.sendMessage(ChatColor.RED + "You need " + cost + " coins to travel.");
            return;
        }
        player.closeInventory();
        Location start = player.getLocation().clone();
        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (!player.isOnline()) { cancel(); return; }
                if (player.getLocation().distanceSquared(start) > 0.25) {
                    player.sendMessage(ChatColor.RED + "Teleport cancelled!");
                    cancel();
                    return;
                }
                double radius = 3.0 * (1 - ticks / 60.0);
                for (int i = 0; i < 20; i++) {
                    double angle = 2 * Math.PI * i / 20;
                    player.getWorld().spawnParticle(Particle.DRAGON_BREATH,
                            start.getX() + Math.cos(angle) * radius,
                            start.getY(),
                            start.getZ() + Math.sin(angle) * radius,
                            0, 0, 0, 0);
                }
                if (++ticks >= 60) {
                    economy.deductCoins(player, cost);
                    player.teleport(target.getLocation());
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false));
                    player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 50, 0.5, 0.5, 0.5);
                    manager.recordUse(player, target.getName());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private ItemStack getArrow(String name, boolean right) {
        return getOraxenItem(right ? "arrow_right" : "arrow_left", name);
    }

    private ItemStack createSortButton(int mode) {
        ItemStack it = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Sort");
            java.util.List<String> lore = new java.util.ArrayList<>();
            String[] opts = {"Distance Far", "Distance Close", "A-Z", "Last Used"};
            for (int i = 0; i < opts.length; i++) {
                String pre = i == mode ? ChatColor.GREEN + "➤ " : ChatColor.GRAY + "  ";
                lore.add(pre + opts[i]);
            }
            meta.setLore(lore);
            it.setItemMeta(meta);
        }
        return it;
    }

    private void sortPoints(java.util.List<FastTravelPoint> list, int mode, Player player) {
        switch (mode) {
            case 0 -> list.sort(java.util.Comparator.comparingDouble(pt -> -pt.getLocation().distance(player.getLocation())));
            case 1 -> list.sort(java.util.Comparator.comparingDouble(pt -> pt.getLocation().distance(player.getLocation())));
            case 2 -> list.sort(java.util.Comparator.comparing(FastTravelPoint::getName, String.CASE_INSENSITIVE_ORDER));
            case 3 -> list.sort(java.util.Comparator.comparingLong((FastTravelPoint pt) -> -manager.getLastUse(player, pt.getName())));
        }
    }

    private ItemStack getOraxenItem(String id, String name) {
        io.th0rgal.oraxen.items.ItemBuilder b = io.th0rgal.oraxen.api.OraxenItems.getItemById(id);
        ItemStack it = b == null ? new ItemStack(Material.BARRIER) : b.build();
        ItemMeta meta = it.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); it.setItemMeta(meta); }
        return it;
    }
}
