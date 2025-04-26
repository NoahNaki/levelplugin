package me.nakilex.levelplugin.items.listeners;

import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.EnumSet;
import java.util.UUID;

public class ItemNameDisplayListener implements Listener {

    // Which materials get the glow team treatment
    private static final EnumSet<Material> ARMOR_AND_WEAPONS = EnumSet.of(
        Material.DIAMOND_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.NETHERITE_SWORD, Material.WOODEN_SWORD, Material.STONE_SWORD,
        Material.DIAMOND_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.NETHERITE_AXE, Material.WOODEN_AXE, Material.STONE_AXE,
        Material.BOW, Material.CROSSBOW, Material.TRIDENT,
        Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
        Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
        Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
        Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
        Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
        Material.DIAMOND_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.NETHERITE_SHOVEL, Material.WOODEN_SHOVEL, Material.STONE_SHOVEL
    );

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        Item itemEntity = event.getEntity();
        ItemStack stack = itemEntity.getItemStack();

        // 1) Pull the UUID we wrote on the ItemStack
        UUID itemUUID = ItemUtil.getItemUUID(stack);
        if (itemUUID == null) return;

        // 2) Fetch the live instance (so we use its rolled stats & upgrade level)
        CustomItem customItem = ItemManager.getInstance().getItemByUUID(itemUUID);
        if (customItem == null) return;

        // 3) Color + name
        ChatColor rarityColor = customItem.getRarity().getColor();
        itemEntity.setCustomName(rarityColor + customItem.getName());
        itemEntity.setCustomNameVisible(true);

        // 4) Glow if it's armor/weapon/shovel
        if (ARMOR_AND_WEAPONS.contains(customItem.getMaterial())) {
            applyGlowWithColor(itemEntity, rarityColor);
        }
    }

    private void applyGlowWithColor(Item itemEntity, ChatColor rarityColor) {
        ScoreboardManager manager = itemEntity.getServer().getScoreboardManager();
        if (manager == null) return;

        Scoreboard board = manager.getMainScoreboard();
        String teamName = "glow_" + rarityColor.name().toLowerCase();

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
            team.setColor(rarityColor);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.COLLISION_RULE,    Team.OptionStatus.NEVER);
        }

        // Add the entity by its unique entry
        team.addEntry(itemEntity.getUniqueId().toString());
        itemEntity.setGlowing(true);
    }
}
