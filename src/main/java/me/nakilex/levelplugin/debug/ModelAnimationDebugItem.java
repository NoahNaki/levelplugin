package me.nakilex.levelplugin.debug;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.mob.config.ModelSetManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Debug item that cycles through weapon models whenever the player swings.
 */
public final class ModelAnimationDebugItem {
    private static final String KEY_NAME = "model_animation_debug";
    private static final String MODEL_LIST_KEY_NAME = "model_animation_ids";
    private static final String MODEL_INDEX_KEY_NAME = "model_animation_index";

    private ModelAnimationDebugItem() {
    }

    public static void give(Player player) {
        List<String> modelIds = getAvailableWeaponModelIds();
        if (modelIds.isEmpty()) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "No weapon models are loaded in model_sets.yml.");
            return;
        }
        ItemStack item = create(modelIds);
        player.getInventory().addItem(item);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Model animation test weapon added. Left click to cycle " + modelIds.size() + " models.");
    }

    public static ItemStack create(List<String> modelIds) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.GOLD + "Model Animation Debug");
        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add(ChatColor.GRAY + "Swing to cycle weapon models.");
        lore.addAll(TooltipUtil.bulletList(
                "Cycles to the next model on each left click",
                "Useful for quick handheld animation testing"
        ));
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(getKey(), PersistentDataType.BYTE, (byte) 1);
        pdc.set(getModelListKey(), PersistentDataType.STRING, String.join(",", modelIds));
        pdc.set(getModelIndexKey(), PersistentDataType.INTEGER, 0);
        item.setItemMeta(meta);

        ItemUtil.applyNexoModel(item, modelIds.getFirst());
        return item;
    }

    public static boolean isDebugItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(getKey(), PersistentDataType.BYTE);
    }

    public static List<String> getModelIds(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return List.of();
        }
        String raw = meta.getPersistentDataContainer().get(getModelListKey(), PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (String id : raw.split(",")) {
            if (!id.isBlank()) {
                ids.add(id.trim());
            }
        }
        return ids;
    }

    public static int getCurrentIndex(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        Integer idx = meta.getPersistentDataContainer().get(getModelIndexKey(), PersistentDataType.INTEGER);
        return idx == null ? 0 : idx;
    }

    public static void setCurrentIndex(ItemStack item, int index) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(getModelIndexKey(), PersistentDataType.INTEGER, index);
        item.setItemMeta(meta);
    }

    public static List<String> getAvailableWeaponModelIds() {
        ModelSetManager modelSetManager = Main.getInstance().getModelSetManager();
        if (modelSetManager == null) {
            return List.of();
        }
        return modelSetManager.getAllWeaponModelIds().stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static NamespacedKey getKey() {
        return new NamespacedKey(Main.getInstance(), KEY_NAME);
    }

    private static NamespacedKey getModelListKey() {
        return new NamespacedKey(Main.getInstance(), MODEL_LIST_KEY_NAME);
    }

    private static NamespacedKey getModelIndexKey() {
        return new NamespacedKey(Main.getInstance(), MODEL_INDEX_KEY_NAME);
    }
}

