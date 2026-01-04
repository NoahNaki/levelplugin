package me.nakilex.levelplugin.items.managers;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.generator.ProceduralItemGenerator;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.mob.utils.CombatRewardCalculator.GearTarget;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ItemManager {

    private static ItemManager instance;

    public static ItemManager getInstance() {
        return instance;
    }

    private final Map<UUID, CustomItem> itemsMap     = new HashMap<>(); // Instances by UUID
    private final Map<Integer, UUID> holderMap = new HashMap<>();

    /** Procedurally generated items all receive unique negative IDs. */
    private int nextGeneratedId = -1;

    private final ProceduralItemGenerator generator;

    public ItemManager(Plugin plugin) {
        instance = this;
        generator = new ProceduralItemGenerator(Main.getInstance());
    }

    /** Register a freshly‐rolled instance */
    public void addInstance(CustomItem instance) {
        itemsMap.put(instance.getUuid(), instance);
    }

    /** Lookup a live instance by its UUID */
    public CustomItem getItemByUUID(UUID uuid) {
        return itemsMap.get(uuid);
    }

    /**
     * Given an ItemStack with our PDC UUID tag, pull out the matching
     * CustomItem instance.
     */
    public CustomItem getCustomItemFromItemStack(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return null;

        ItemMeta meta = itemStack.getItemMeta();
        UUID uuid = ItemUtil.getItemUUID(itemStack);
        if (uuid == null) return null;

        CustomItem ci = itemsMap.get(uuid);
        if (ci == null) {
            Main.getInstance().getLogger()
                .info("No custom item found for UUID: " + uuid);
        }
        return ci;
    }


    /**
     * Obtain the next unique ID for a procedurally generated item. IDs start at
     * -1 and move downward to avoid colliding with positive template IDs.
     */
    public synchronized int getNextGeneratedId() {
        return nextGeneratedId--;
    }

    /**
     * Generate a brand new procedural item using the generator utility.
     */
    public CustomItem generateItem(String mobType, int level) {
        return generator.generate(mobType, level);
    }

    public CustomItem generateItemWithMaxRarity(String mobType, int level, ItemRarity maxRarity) {
        return generator.generateWithMaxRarity(mobType, level, maxRarity);
    }

    public CustomItem generateItemForGearScore(String mobType, int targetGearScore, ItemRarity rarity) {
        return generator.generateForGearScore(mobType, new GearTarget(targetGearScore, rarity));
    }

    public CustomItem generateItemForGearScore(String mobType, int targetGearScore, ItemRarity rarity, int levelRequirement) {
        return generator.generateForGearScore(mobType, new GearTarget(targetGearScore, rarity), levelRequirement);
    }

    public Map<UUID, CustomItem> getAllItems() {
        return new HashMap<>(itemsMap);
    }

    public void registerHolder(int itemID, UUID puuid) {
        holderMap.put(itemID, puuid);
    }

    public void unregisterHolder(int itemID) {
        holderMap.remove(itemID);
    }

    public Player getHolderOf(int itemID) {
        UUID puuid = holderMap.get(itemID);
        if (puuid == null) return null;
        return Bukkit.getPlayer(puuid);
    }
}
