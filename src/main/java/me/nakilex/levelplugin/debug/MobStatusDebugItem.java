package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.mob.custom.CustomMobStatus;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MobStatusDebugItem {
    private static final String KEY_NAME = "mob_status_debug";

    private MobStatusDebugItem() {}

    public static ItemStack create(CustomMobStatus status) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + getDisplayName(status));
            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(ChatColor.GRAY + getDescription(status));
            lore.addAll(TooltipUtil.bulletList(getDetails(status)));
            meta.setLore(lore);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(getKey(), PersistentDataType.STRING, status.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    public static Optional<CustomMobStatus> resolveStatus(ItemStack item) {
        if (item == null) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        String stored = meta.getPersistentDataContainer().get(getKey(), PersistentDataType.STRING);
        if (stored == null || stored.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(CustomMobStatus.valueOf(stored));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String getDisplayName(CustomMobStatus status) {
        return switch (status) {
            case STUNNED -> "Stun Stick";
            case POISONED -> "Poison Stick";
            case TAUNTED -> "Taunt Stick";
            case FEARED -> "Fear Stick";
        };
    }

    private static String getDescription(CustomMobStatus status) {
        return switch (status) {
            case STUNNED -> "Hit a custom mob to stun it.";
            case POISONED -> "Hit a custom mob to poison it.";
            case TAUNTED -> "Hit a custom mob to taunt it.";
            case FEARED -> "Hit a custom mob to fear it.";
        };
    }

    private static String[] getDetails(CustomMobStatus status) {
        return switch (status) {
            case STUNNED -> new String[] { "Disables movement and AI", "Shows a crit ring indicator" };
            case POISONED -> new String[] { "Deals 1% max HP per second", "Single stack on bosses" };
            case TAUNTED -> new String[] { "Forces aggro to you", "Shows an angry ring indicator" };
            case FEARED -> new String[] { "Moves away from you", "Shows a smoke ring indicator" };
        };
    }

    private static NamespacedKey getKey() {
        return new NamespacedKey(Main.getInstance(), KEY_NAME);
    }
}
