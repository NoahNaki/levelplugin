package me.nakilex.levelplugin.effectdemo;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import me.nakilex.levelplugin.effectdemo.DemoEffects;

/**
 * Simple GUI showcasing a few EffectLib particle effects.
 */
public class EffectDemoGUI {
    private static final int SIZE = 54;
    private static final int NAV_PREV_SLOT = 45;
    private static final int NAV_NEXT_SLOT = 53;

    private static final ItemStack FILLER = createFiller();

    private static final List<Integer> ITEM_SLOTS = new ArrayList<>();
    static {
        for (int i = 0; i < SIZE; i++) {
            if (i == NAV_NEXT_SLOT || i == NAV_PREV_SLOT) continue;
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) continue;
            ITEM_SLOTS.add(i);
        }
    }

    public static int itemsPerPage() {
        return ITEM_SLOTS.size();
    }

    static List<Integer> getItemSlots() {
        return ITEM_SLOTS;
    }

    public static int nextSlot() { return NAV_NEXT_SLOT; }
    public static int prevSlot() { return NAV_PREV_SLOT; }

    private static String title(int page) {
        return ChatColor.BLUE + "FX Demo - Page " + (page + 1);
    }

    public static void open(Player player) {
        open(player, 0);
    }

    public static void open(Player player, int page) {
        DemoEffects[] effects = DemoEffects.values();
        int perPage = itemsPerPage();
        int maxPage = (effects.length - 1) / perPage;
        if (page < 0) page = 0;
        if (page > maxPage) page = maxPage;

        Inventory inv = Bukkit.createInventory(null, SIZE, title(page));

        // border filler
        for (int i = 0; i < SIZE; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, FILLER);
            }
        }

        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < effects.length; i++) {
            DemoEffects effect = effects[start + i];
            addItem(inv, ITEM_SLOTS.get(i), effect.getIcon(), effect.getLabel());
        }

        if (page > 0) inv.setItem(NAV_PREV_SLOT, getNexoItem("arrow_left", ChatColor.YELLOW + "Previous Page"));
        if (page < maxPage) inv.setItem(NAV_NEXT_SLOT, getNexoItem("arrow_right", ChatColor.YELLOW + "Next Page"));

        player.openInventory(inv);
    }

    private static void addItem(Inventory inv, int slot, Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + name);
            it.setItemMeta(meta);
        }
        inv.setItem(slot, it);
    }

    private static ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack getNexoItem(String id, String name) {
        ItemBuilder builder = NexoItems.itemFromId(id);
        if (builder == null) return new ItemStack(Material.BARRIER);
        ItemStack item = builder.build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
