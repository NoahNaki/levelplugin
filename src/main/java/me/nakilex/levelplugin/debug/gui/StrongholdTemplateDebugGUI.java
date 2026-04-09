package me.nakilex.levelplugin.debug.gui;

import me.nakilex.levelplugin.debug.StrongholdDebugGenerator;
import me.nakilex.levelplugin.utils.ChatUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Browse and manage stronghold template regions.
 */
public class StrongholdTemplateDebugGUI implements Listener {
    private static final String TITLE = ChatUtil.applyEmojis("§8Stronghold Templates");
    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final Map<UUID, Integer> pageByPlayer = new HashMap<>();

    public void open(Player player) {
        pageByPlayer.putIfAbsent(player.getUniqueId(), 0);
        open(player, pageByPlayer.get(player.getUniqueId()));
    }

    private void open(Player player, int page) {
        List<StrongholdDebugGenerator.TemplateSnapshot> templates = sortedTemplates();
        int maxPage = Math.max(0, (templates.size() - 1) / CONTENT_SLOTS.length);
        int safePage = Math.max(0, Math.min(page, maxPage));
        pageByPlayer.put(player.getUniqueId(), safePage);

        Inventory inv = GuiBuilder.create(SIZE, TITLE)
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border()
                .build();

        int start = safePage * CONTENT_SLOTS.length;
        for (int i = 0; i < CONTENT_SLOTS.length; i++) {
            int idx = start + i;
            if (idx >= templates.size()) {
                break;
            }
            inv.setItem(CONTENT_SLOTS[i], buildTemplateItem(templates.get(idx)));
        }

        if (safePage > 0) {
            inv.setItem(45, navItem(Material.ARROW, "§aPrevious Page"));
        }
        if ((safePage + 1) * CONTENT_SLOTS.length < templates.size()) {
            inv.setItem(53, navItem(Material.ARROW, "§aNext Page"));
        }

        inv.setItem(49, navItem(Material.BARRIER, "§cClose"));
        player.openInventory(inv);
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

        int raw = event.getRawSlot();
        if (raw < 0 || raw >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (raw == 49) {
            player.closeInventory();
            return;
        }

        if (raw == 45) {
            open(player, pageByPlayer.getOrDefault(player.getUniqueId(), 0) - 1);
            return;
        }

        if (raw == 53) {
            open(player, pageByPlayer.getOrDefault(player.getUniqueId(), 0) + 1);
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
            return;
        }

        String templateId = templateId(clicked);
        if (templateId == null || templateId.isBlank()) {
            return;
        }

        World world = player.getWorld();
        switch (event.getClick()) {
            case LEFT -> {
                boolean enabled = StrongholdDebugGenerator.toggleTemplateEnabled(templateId);
                player.sendMessage((enabled ? ChatColor.GREEN : ChatColor.RED)
                        + "Template " + templateId + " is now " + (enabled ? "enabled" : "disabled") + ".");
                open(player, pageByPlayer.getOrDefault(player.getUniqueId(), 0));
            }
            case RIGHT -> {
                Location target = StrongholdDebugGenerator.getTemplateTeleportLocation(world, templateId);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Could not find template location for " + templateId + ".");
                    return;
                }
                player.teleport(target);
                player.sendMessage(ChatColor.YELLOW + "Teleported to template " + templateId + ".");
            }
            default -> {
            }
        }
    }

    private static List<StrongholdDebugGenerator.TemplateSnapshot> sortedTemplates() {
        List<StrongholdDebugGenerator.TemplateSnapshot> templates = new ArrayList<>(StrongholdDebugGenerator.getTemplateSnapshots());
        templates.sort(Comparator.comparing(StrongholdDebugGenerator.TemplateSnapshot::category)
                .thenComparing(StrongholdDebugGenerator.TemplateSnapshot::id, String.CASE_INSENSITIVE_ORDER));
        return templates;
    }

    private static ItemStack buildTemplateItem(StrongholdDebugGenerator.TemplateSnapshot tpl) {
        ItemStack item = GuiUtil.createToggleItem(tpl.enabled(), ChatColor.AQUA + tpl.id());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.addAll(TooltipUtil.bulletList(
                    "Category: " + ChatColor.WHITE + tpl.category(),
                    "Min: " + ChatColor.WHITE + xyz(tpl.minX(), tpl.minY(), tpl.minZ()),
                    "Max: " + ChatColor.WHITE + xyz(tpl.maxX(), tpl.maxY(), tpl.maxZ())
            ));
            lore.add(" ");
            lore.addAll(TooltipUtil.clickInstructions("to toggle enabled/disabled", "to teleport to this template"));
            lore.add(ChatColor.DARK_GRAY + "id:" + tpl.id().toLowerCase(Locale.ROOT));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack navItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static String templateId(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta() == null || item.getItemMeta().getLore() == null) {
            return null;
        }
        for (String line : item.getItemMeta().getLore()) {
            String stripped = ChatColor.stripColor(line);
            if (stripped != null && stripped.startsWith("id:")) {
                return stripped.substring(3);
            }
        }
        return null;
    }

    private static String xyz(int x, int y, int z) {
        return x + ", " + y + ", " + z;
    }
}
