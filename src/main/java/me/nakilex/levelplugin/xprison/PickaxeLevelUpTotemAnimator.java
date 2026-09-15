package me.nakilex.levelplugin.xprison;

import me.nakilex.levelplugin.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.EntityEffect;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Plays the client-native totem overlay using LevelPlugin's level-up item model. */
public final class PickaxeLevelUpTotemAnimator {

    private static final String CONFIG_ROOT = "xprison.pickaxe-level-up-totem";
    private static final String DEFAULT_MODEL = "levelplugin:level_up_totem_v3";

    private final Main plugin;

    public PickaxeLevelUpTotemAnimator(Main plugin) {
        this.plugin = plugin;
    }

    public void play(Player player) {
        if (player == null || !player.isOnline()
                || !plugin.getConfig().getBoolean(CONFIG_ROOT + ".enabled", true)) {
            return;
        }

        String configuredModel = plugin.getConfig().getString(CONFIG_ROOT + ".item-model", DEFAULT_MODEL);
        NamespacedKey itemModel = NamespacedKey.fromString(configuredModel == null ? DEFAULT_MODEL : configuredModel);
        if (itemModel == null) {
            plugin.getLogger().warning("Invalid pickaxe level-up totem item-model: " + configuredModel);
            return;
        }

        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();
        meta.setItemModel(itemModel);
        totem.setItemMeta(meta);
        if (!sendPaperAnimation(player, totem)) {
            playEquipmentFallback(player, totem);
        }
        suppressTotemSound(player);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            suppressTotemSound(player);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                    (float) plugin.getConfig().getDouble(CONFIG_ROOT + ".sound-volume", 1.0),
                    (float) plugin.getConfig().getDouble(CONFIG_ROOT + ".sound-pitch", 1.2));
        }, 1L);
    }

    private boolean sendPaperAnimation(Player player, ItemStack totem) {
        try {
            Method method = player.getClass().getMethod("sendTotemAnimation", ItemStack.class);
            method.invoke(player, totem);
            return true;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            return false;
        }
    }

    /** Compatibility path for Paper builds which do not expose the item-aware API at compile time. */
    private void playEquipmentFallback(Player player, ItemStack totem) {
        ItemStack mainHand = player.getInventory().getItemInMainHand().clone();
        ItemStack offHand = player.getInventory().getItemInOffHand().clone();
        player.sendEquipmentChange(player, EquipmentSlot.HAND, totem);
        player.sendEquipmentChange(player, EquipmentSlot.OFF_HAND, totem);
        player.playEffect(EntityEffect.TOTEM_RESURRECT);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            player.sendEquipmentChange(player, EquipmentSlot.HAND, mainHand);
            player.sendEquipmentChange(player, EquipmentSlot.OFF_HAND, offHand);
        });
    }

    private void suppressTotemSound(Player player) {
        player.stopSound(Sound.ITEM_TOTEM_USE);
        player.stopSound(Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS);
        player.stopSound("minecraft:item.totem.use");
        player.stopSound("minecraft:item.totem.use", SoundCategory.PLAYERS);
        // Paper queues the native effect packet; repeat briefly so the queued sound cannot win the race.
        for (long delay = 1L; delay <= 5L; delay++) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.stopSound(Sound.ITEM_TOTEM_USE);
                    player.stopSound(Sound.ITEM_TOTEM_USE, SoundCategory.PLAYERS);
                    player.stopSound("minecraft:item.totem.use");
                    player.stopSound("minecraft:item.totem.use", SoundCategory.PLAYERS);
                }
            }, delay);
        }
    }
}
