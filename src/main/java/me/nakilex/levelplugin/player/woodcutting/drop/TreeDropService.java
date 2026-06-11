package me.nakilex.levelplugin.player.woodcutting.drop;

import me.nakilex.levelplugin.player.woodcutting.tree.TreeDetectionResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;

public class TreeDropService {
    public void drop(Player player, TreeDetectionResult tree, DropMode mode) {
        ItemStack axe = player.getInventory().getItemInMainHand();
        for (TreeDetectionResult.CapturedBlock snapshot : tree.snapshots()) {
            Collection<ItemStack> drops = snapshot.state().getDrops(axe, player);
            switch (mode) {
                case LOCAL -> dropAt(snapshot.originalLocation(), drops);
                case ORIGIN -> dropAt(tree.root().getLocation().add(0.5, 0.5, 0.5), drops);
                case INVENTORY -> giveOrDrop(player, drops);
                case TURN_INTO_BLOCKS -> placeOrDrop(snapshot, drops);
            }
        }
    }

    private void dropAt(Location location, Collection<ItemStack> drops) {
        if (location.getWorld() == null) return;
        for (ItemStack drop : filterDrops(drops)) {
            location.getWorld().dropItemNaturally(location, drop);
        }
    }

    private void giveOrDrop(Player player, Collection<ItemStack> drops) {
        Collection<ItemStack> filteredDrops = filterDrops(drops);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(filteredDrops.toArray(ItemStack[]::new));
        dropAt(player.getLocation(), overflow.values());
    }

    private Collection<ItemStack> filterDrops(Collection<ItemStack> drops) {
        if (drops == null || drops.isEmpty()) {
            return java.util.List.of();
        }
        return drops.stream()
                .filter(drop -> drop != null && drop.getType() != Material.AIR && drop.getAmount() > 0)
                .filter(drop -> !Tag.SAPLINGS.isTagged(drop.getType()))
                .toList();
    }

    private void placeOrDrop(TreeDetectionResult.CapturedBlock snapshot, Collection<ItemStack> drops) {
        Block block = snapshot.originalLocation().getBlock();
        if (block.getType().isAir()) {
            block.setBlockData(snapshot.blockData(), false);
        } else {
            dropAt(snapshot.originalLocation(), drops);
        }
    }
}
