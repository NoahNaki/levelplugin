package me.nakilex.levelplugin.environment.stage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores selection positions for stage editing and provides a common wand.
 */
public final class StageSelectionStore {
    private static final Map<UUID, Selection> selections = new HashMap<>();

    /** Wand used for both town and building stage commands. */
    public static final ItemStack WAND;

    static {
        ItemStack it = new ItemStack(Material.MACE);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Stage Wand");
            it.setItemMeta(meta);
        }
        WAND = it;
    }

    private StageSelectionStore() { }

    public static Selection getSelection(UUID uuid) {
        return selections.computeIfAbsent(uuid, k -> new Selection());
    }

    public static Location getPos1(UUID uuid) {
        return getSelection(uuid).pos1;
    }

    public static Location getPos2(UUID uuid) {
        return getSelection(uuid).pos2;
    }

    /**
     * Checks whether both positions have been selected for the given player.
     *
     * @param uuid the player's unique ID
     * @return true if both pos1 and pos2 are set
     */
    public static boolean hasSelection(UUID uuid) {
        Selection sel = selections.get(uuid);
        boolean has = sel != null && sel.pos1 != null && sel.pos2 != null;
        if (!has) {
            Bukkit.getLogger().info("[SelectionDebug] " + uuid + " pos1=" + fmt(sel != null ? sel.pos1 : null) +
                    " pos2=" + fmt(sel != null ? sel.pos2 : null));
        }
        return has;
    }

    public static void setPos1(UUID uuid, Location loc) {
        getSelection(uuid).pos1 = loc;
        Bukkit.getLogger().info("[SelectionDebug] pos1 set for " + uuid + " -> " + fmt(loc));
    }

    public static void setPos2(UUID uuid, Location loc) {
        getSelection(uuid).pos2 = loc;
        Bukkit.getLogger().info("[SelectionDebug] pos2 set for " + uuid + " -> " + fmt(loc));
    }

    private static String fmt(Location loc) {
        if (loc == null) return "null";
        return loc.getWorld().getName() + ":" + loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ();
    }

    public static class Selection {
        public Location pos1;
        public Location pos2;
    }
}
