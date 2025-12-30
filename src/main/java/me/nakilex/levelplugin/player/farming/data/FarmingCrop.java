package me.nakilex.levelplugin.player.farming.data;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;

import java.util.HashMap;
import java.util.Map;

public enum FarmingCrop {
    WHEAT(Material.WHEAT, Material.WHEAT, 1, 12, 4, "WHEAT"),
    POTATO(Material.POTATOES, Material.POTATO, 5, 15, 6, "POTATO"),
    CARROT(Material.CARROTS, Material.CARROT, 10, 18, 7, "CARROT"),
    BEETROOT(Material.BEETROOTS, Material.BEETROOT, 15, 22, 9, "BEETROOT"),
    SWEET_BERRIES(Material.SWEET_BERRY_BUSH, Material.SWEET_BERRIES, 20, 26, 11, "SWEET_BERRIES"),
    PUMPKIN(Material.PUMPKIN, Material.PUMPKIN, 24, 30, 14, "PUMPKIN");

    private static final Map<Material, FarmingCrop> BY_BLOCK = new HashMap<>();
    private static final Map<Material, FarmingCrop> BY_ITEM = new HashMap<>();

    static {
        for (FarmingCrop crop : values()) {
            BY_BLOCK.put(crop.blockMaterial, crop);
            BY_ITEM.put(crop.itemMaterial, crop);
        }
    }

    private final Material blockMaterial;
    private final Material itemMaterial;
    private final int levelRequirement;
    private final int xpReward;
    private final int sellValue;
    private final String questId;

    FarmingCrop(Material blockMaterial, Material itemMaterial, int levelRequirement, int xpReward, int sellValue,
                String questId) {
        this.blockMaterial = blockMaterial;
        this.itemMaterial = itemMaterial;
        this.levelRequirement = levelRequirement;
        this.xpReward = xpReward;
        this.sellValue = sellValue;
        this.questId = questId;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public int getLevelRequirement() {
        return levelRequirement;
    }

    public int getXpReward() {
        return xpReward;
    }

    public int getSellValue() {
        return sellValue;
    }

    public String getQuestId() {
        return questId;
    }

    public boolean isMature(Block block) {
        if (block == null) {
            return false;
        }
        if (block.getType() != blockMaterial) {
            return false;
        }
        if (block.getBlockData() instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return true;
    }

    public void replant(Block block) {
        if (block == null) {
            return;
        }
        if (block.getBlockData() instanceof Ageable ageable) {
            block.setType(blockMaterial);
            Ageable replanted = (Ageable) block.getBlockData();
            replanted.setAge(0);
            block.setBlockData(replanted);
            return;
        }
        if (blockMaterial == Material.PUMPKIN) {
            block.setType(Material.AIR);
        }
    }

    public static FarmingCrop fromBlock(Block block) {
        return block == null ? null : BY_BLOCK.get(block.getType());
    }

    public static FarmingCrop fromItem(Material material) {
        return material == null ? null : BY_ITEM.get(material);
    }
}
