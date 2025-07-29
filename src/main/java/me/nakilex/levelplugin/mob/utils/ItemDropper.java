package me.nakilex.levelplugin.mob.utils;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.mob.config.MobRewardsConfig;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.mob.config.ModelSetManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
    private final LevelManager levelManager;
    private final MobRewardsConfig rewardsConfig;
    private final ModelSetManager modelSetManager;

    public ItemDropper(LevelManager levelManager, MobRewardsConfig rewardsConfig, ModelSetManager modelSetManager) {
        this.levelManager = levelManager;
        this.rewardsConfig = rewardsConfig;
        this.modelSetManager = modelSetManager;
    }

    /**
     * Drop configured custom items for the given MythicMob type.
     */
    public void dropCustomItems(Player player, String mobType, String modelSet) {
        String path = "mobs." + mobType + ".items";
        if (!rewardsConfig.getConfig().contains(path)) {
            return;
        }
        List<Map<?, ?>> itemList = rewardsConfig.getConfig().getMapList(path);
        if (itemList == null || itemList.isEmpty()) {
            return;
        }
        for (Map<?, ?> entry : itemList) {
            if (!entry.containsKey("itemid")) continue;
            int itemId = (int) entry.get("itemid");
            double dropRate = entry.containsKey("drop_rate") ? (double) entry.get("drop_rate") : 100.0;
            double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
            if (roll > dropRate) continue;
            String qtyRange = entry.containsKey("quantity") ? (String) entry.get("quantity") : "1-1";
            String[] rangeSplit = qtyRange.split("-");
            int minQty = Integer.parseInt(rangeSplit[0]);
            int maxQty = Integer.parseInt(rangeSplit[1]);
            int quantity = ThreadLocalRandom.current().nextInt(minQty, maxQty + 1);

            if (itemId == -1) {
                for (int i = 0; i < quantity; i++) {
                    int lvl = levelManager.getLevel(player);
                    CustomItem ci = ItemManager.getInstance().generateItem(mobType, lvl);
                    String nexo = modelSet != null ? modelSetManager.getModelId(modelSet, ci.getMaterial()) : null;
                    ItemStack stack = ItemUtil.createItemStackFromCustomItem(ci, 1, player, nexo);
                    ItemUtil.updateTooltip(stack, player);
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
                }
                continue;
            }

            CustomItem template = ItemManager.getInstance().getTemplateById(itemId);
            if (template == null) {
                player.sendMessage("§c[Warning] No CustomItem found with ID: " + itemId);
                continue;
            }
            for (int i = 0; i < quantity; i++) {
                CustomItem newInstance = new CustomItem(
                        template.getId(),
                        template.getBaseName(),
                        template.getRarity(),
                        template.getLevelRequirement(),
                        template.getClassRequirement(),
                        template.getMaterial(),
                        template.getHpRange(),
                        template.getDefRange(),
                        template.getStrRange(),
                        template.getAgiRange(),
                        template.getIntelRange(),
                        template.getDexRange(),
                        template.isEgo(),
                        template.getEgoKey()
                );
                ItemManager.getInstance().addInstance(newInstance);
                String nexo = modelSet != null ? modelSetManager.getModelId(modelSet, newInstance.getMaterial()) : null;
                ItemStack dropStack = ItemUtil.createItemStackFromCustomItem(newInstance, 1, player, nexo);
                ItemUtil.updateTooltip(dropStack, player);
                player.getWorld().dropItemNaturally(player.getLocation(), dropStack);
            }
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
                ChatColor.BLUE + "Scroll of Fortitude"
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
