package me.nakilex.levelplugin.mob.utils;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.mob.utils.CombatRewardCalculator.GearTarget;
import me.nakilex.levelplugin.mob.config.ModelSetManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles dropping custom items and rare reroll scrolls when a mob dies.
 */
public class ItemDropper {
    private final ModelSetManager modelSetManager;

    public ItemDropper(ModelSetManager modelSetManager) {
        this.modelSetManager = modelSetManager;
    }

    /**
     * Drop configured custom items for the given MythicMob type.
     */
    public void dropCustomItems(Player player, ConfigurationSection node, String modelSet, int combatPower) {
        if (node == null) {
            return;
        }
        List<Map<?, ?>> itemList = node.getMapList("items");
        if (itemList == null || itemList.isEmpty()) {
            return;
        }
        String mobType = node.getName();
        for (Map<?, ?> entry : itemList) {
            double dropRate = entry.containsKey("drop_rate") ? (double) entry.get("drop_rate") : 100.0;
            dropRate = Math.min(10.0, dropRate);
            double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
            if (roll > dropRate) continue;

            String qtyRange = entry.containsKey("quantity") ? (String) entry.get("quantity") : "1-1";
            String[] rangeSplit = qtyRange.split("-");
            int minQty = Integer.parseInt(rangeSplit[0]);
            int maxQty = Integer.parseInt(rangeSplit[1]);
            int quantity = ThreadLocalRandom.current().nextInt(minQty, maxQty + 1);

            for (int i = 0; i < quantity; i++) {
                dropGeneratedItem(player, mobType, modelSet, combatPower);
            }
        }
    }

    private void dropGeneratedItem(Player player, String mobType, String modelSet, int combatPower) {
        GearTarget target = CombatRewardCalculator.rollGearTarget(combatPower);
        CustomItem ci = ItemManager.getInstance().generateItemForGearScore(mobType, target.targetGearScore(), target.rarity());
        String nexo = modelSet != null ? modelSetManager.getModelId(modelSet, ci.getMaterial()) : null;
        ItemStack stack = ItemUtil.createItemStackFromCustomItem(ci, 1, player, nexo);
        ItemUtil.updateTooltip(stack, player);
        player.getWorld().dropItemNaturally(player.getLocation(), stack);
    }

    /** Possibly drop a configured class essence. */
    public void maybeDropEssence(Player player, ConfigurationSection node) {
        if (node == null) return;
        String spec = node.getString("essence");
        if (spec == null || spec.isEmpty()) return;
        String[] parts = spec.split(",");
        if (parts.length < 2) return;
        String className = parts[0].trim();
        double chance;
        try {
            chance = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException ex) {
            return;
        }
        double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
        if (roll > chance) return;
        PlayerClass clazz = PlayerClass.fromString(className);
        if (clazz != null) {
            ItemStack ess = ClassEssence.generateEssence(clazz);
            player.getInventory().addItem(ess).values()
                    .forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        }
    }

    /** Chance to drop a random reroll scroll (0.1%) */
    public void maybeDropRerollScroll(Player player) {
        double chance = 0.001;
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            ItemStack scroll = createRandomRerollScroll();
            player.getWorld().dropItemNaturally(player.getLocation(), scroll);
        }
    }

    private ItemStack createRandomRerollScroll() {
        Material[] mats = {
                Material.BORDURE_INDENTED_BANNER_PATTERN,
                Material.FLOWER_BANNER_PATTERN,
                Material.FLOW_BANNER_PATTERN,
                Material.SKULL_BANNER_PATTERN,
                Material.GUSTER_BANNER_PATTERN,
                Material.GLOBE_BANNER_PATTERN
        };
        String[] names = {
                ChatColor.GREEN + "Scroll of Might",
                ChatColor.AQUA + "Scroll of Knowledge",
                ChatColor.LIGHT_PURPLE + "Scroll of Swiftness",
                ChatColor.RED + "Scroll of Vitality",
                ChatColor.YELLOW + "Scroll of Precision",
                ChatColor.BLUE + "Scroll of Willpower"
        };
        int idx = ThreadLocalRandom.current().nextInt(mats.length);
        ItemStack item = new ItemStack(mats[idx]);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(names[idx]);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            String statName = names[idx].replace("Scroll of ", "");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Reroll the " + statName + " of your item at a blacksmith");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
