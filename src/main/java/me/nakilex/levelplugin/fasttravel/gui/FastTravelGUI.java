package me.nakilex.levelplugin.fasttravel.gui;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.fasttravel.FastTravelManager;
import me.nakilex.levelplugin.fasttravel.data.FastTravelPoint;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

public class FastTravelGUI implements Listener {
    public static final String TITLE = ChatColor.DARK_AQUA + "Fast Travel";
    private static final int SIZE = 54;
    private static final int[] SLOTS = {
            10,11,12,13,14,15,16,
            19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,
            37,38,39,40,41,42,43
    };
    private static final int ITEMS_PER_PAGE = SLOTS.length;
    private static final int PREV_PAGE = 45;
    private static final int NEXT_PAGE = 53;
    private static final int SORT_SLOT = 50;

    private final FastTravelManager manager;
    private final EconomyManager economy;
    private final Map<UUID, Integer> pageMap = new HashMap<>();
    private final Map<UUID, BukkitRunnable> castTasks = new HashMap<>();
    private final Map<UUID, Integer> sortMap = new HashMap<>();
    private final Map<UUID, Map<String, Long>> lastUsed = new HashMap<>();

    public FastTravelGUI(FastTravelManager manager, EconomyManager economy) {
        this.manager = manager;
        this.economy = economy;
    }

    public void open(Player player) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        sortMap.putIfAbsent(player.getUniqueId(), 2); // default A-Z
        open(player, page);
    }

    private void open(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        Inventory gui = Bukkit.createInventory(null, SIZE, TITLE);
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) gui.setItem(i, filler);
        }

        int sort = sortMap.getOrDefault(player.getUniqueId(), 0);
        List<FastTravelPoint> list = manager.getPoints().stream()
                .filter(FastTravelPoint::isTown)
                .collect(Collectors.toList());

        Comparator<FastTravelPoint> comp = switch (sort) {
            case 0 -> Comparator.comparingDouble((FastTravelPoint pt) -> player.getLocation().distance(pt.getLocation())).reversed();
            case 1 -> Comparator.comparingDouble(pt -> player.getLocation().distance(pt.getLocation()));
            case 2 -> Comparator.comparing(FastTravelPoint::getName, String.CASE_INSENSITIVE_ORDER);
            case 3 -> Comparator.comparingLong((FastTravelPoint pt) -> getLastUsed(player, pt.getName())).reversed();
            default -> Comparator.comparing(FastTravelPoint::getName, String.CASE_INSENSITIVE_ORDER);
        };
        list.sort(comp);

        int start = page * ITEMS_PER_PAGE;
        int slot = 0;
        for (int i = start; i < list.size() && slot < ITEMS_PER_PAGE; i++) {
            FastTravelPoint pt = list.get(i);
            boolean unlocked = manager.isUnlocked(player, pt.getName());
            ItemStack item = new ItemStack(unlocked ? Material.LODESTONE : Material.BARRIER);
            ItemMeta im = item.getItemMeta();
            if (im != null) {
                im.setDisplayName(pt.getColor() + "" + ChatColor.BOLD + pt.getName());
                if (unlocked) {
                    int cost = (int) player.getLocation().distance(pt.getLocation());
                    im.setLore(List.of(
                            ChatColor.GRAY + pt.getDescription(),
                            ChatColor.GRAY + "Teleportation Cost:",
                            ChatColor.WHITE + String.valueOf(cost) + ChatColor.YELLOW + " ⛃"
                    ));
                } else {
                    im.setLore(List.of(ChatColor.DARK_GRAY + "Locked"));
                }
                im.setLocalizedName(pt.getName());
                item.setItemMeta(im);
            }
            gui.setItem(SLOTS[slot++], item);
        }

        if (page > 0) gui.setItem(PREV_PAGE, getOraxenItem("arrow_left", ChatColor.GREEN + "Previous"));
        if (list.size() > (page + 1) * ITEMS_PER_PAGE) gui.setItem(NEXT_PAGE, getOraxenItem("arrow_right", ChatColor.GREEN + "Next"));
        gui.setItem(SORT_SLOT, createSortButton(sort));

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot == PREV_PAGE) {
            int page = Math.max(0, pageMap.getOrDefault(player.getUniqueId(), 0) - 1);
            open(player, page);
            return;
        }
        if (rawSlot == NEXT_PAGE) {
            int page = pageMap.getOrDefault(player.getUniqueId(), 0) + 1;
            open(player, page);
            return;
        }
        if (rawSlot == SORT_SLOT) {
            int mode = sortMap.getOrDefault(player.getUniqueId(), 0);
            int total = 4;
            if (event.getClick() == ClickType.RIGHT) {
                mode = (mode + total - 1) % total;
            } else {
                mode = (mode + 1) % total;
            }
            sortMap.put(player.getUniqueId(), mode);
            open(player, pageMap.getOrDefault(player.getUniqueId(), 0));
            return;
        }

        int index = -1;
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] == rawSlot) { index = i; break; }
        }
        if (index == -1) return;
        int sort = sortMap.getOrDefault(player.getUniqueId(), 0);
        List<FastTravelPoint> list = manager.getPoints().stream()
                .filter(FastTravelPoint::isTown)
                .collect(Collectors.toList());
        Comparator<FastTravelPoint> comp = switch (sort) {
            case 0 -> Comparator.comparingDouble((FastTravelPoint pt) -> player.getLocation().distance(pt.getLocation())).reversed();
            case 1 -> Comparator.comparingDouble(pt -> player.getLocation().distance(pt.getLocation()));
            case 2 -> Comparator.comparing(FastTravelPoint::getName, String.CASE_INSENSITIVE_ORDER);
            case 3 -> Comparator.comparingLong((FastTravelPoint pt) -> getLastUsed(player, pt.getName())).reversed();
            default -> Comparator.comparing(FastTravelPoint::getName, String.CASE_INSENSITIVE_ORDER);
        };
        list.sort(comp);
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        int globalIndex = page * ITEMS_PER_PAGE + index;
        if (globalIndex >= list.size()) return;
        FastTravelPoint target = list.get(globalIndex);
        if (!manager.isUnlocked(player, target.getName())) return;
        int cost = (int) player.getLocation().distance(target.getLocation());
        if (economy.getBalance(player) < cost) {
            player.sendMessage(ChatColor.RED + "You need " + cost + " coins to travel.");
            return;
        }
        player.closeInventory();
        startCast(player, target, cost);
    }

    private ItemStack getOraxenItem(String id, String name) {
        ItemBuilder builder = OraxenItems.getItemById(id);
        ItemStack item = builder == null ? new ItemStack(Material.BARRIER) : builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSortButton(int mode) {
        ItemStack it = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Sort");
            List<String> lore = new ArrayList<>();
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

    private long getLastUsed(Player player, String name) {
        Map<String, Long> map = lastUsed.get(player.getUniqueId());
        if (map == null) return 0L;
        return map.getOrDefault(name.toLowerCase(), 0L);
    }

    private void startCast(Player player, FastTravelPoint target, int cost) {
        if (castTasks.containsKey(player.getUniqueId())) return;

        Location startLoc = player.getLocation();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 4, false, false, false));
        player.sendMessage(ChatColor.AQUA + "Hold still to fast travel...");

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 60;

            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }

                if (player.getLocation().distanceSquared(startLoc) > 0.25) {
                    player.sendMessage(ChatColor.RED + "Fast travel cancelled.");
                    player.removePotionEffect(PotionEffectType.SLOW);
                    cancel();
                    castTasks.remove(player.getUniqueId());
                    return;
                }

                double radius = 2.5 * ticks / 60.0;
                Location base = player.getLocation().add(0, 1, 0);
                for (int i = 0; i < 16; i++) {
                    double angle = 2 * Math.PI * i / 16;
                    double x = base.getX() + radius * Math.cos(angle);
                    double z = base.getZ() + radius * Math.sin(angle);
                    base.getWorld().spawnParticle(Particle.DRAGON_BREATH, x, base.getY(), z, 0, 0, 0, 0);
                }

                ticks--;
                if (ticks <= 0) {
                    player.removePotionEffect(PotionEffectType.SLOW);
                    economy.deductCoins(player, cost);
                    player.teleport(target.getLocation());
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, false));
                    target.getLocation().getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    target.getLocation().getWorld().spawnParticle(Particle.PORTAL, target.getLocation(), 80, 1, 1, 1, 0.5);
                    player.sendMessage(ChatColor.GREEN + "Fast traveled to " + target.getName() + " for " + cost + " coins.");
                    lastUsed.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                            .put(target.getName().toLowerCase(), System.currentTimeMillis());
                    cancel();
                    castTasks.remove(player.getUniqueId());
                }
            }
        };

        castTasks.put(player.getUniqueId(), task);
        task.runTaskTimer(Main.getInstance(), 0L, 1L);
    }
}
