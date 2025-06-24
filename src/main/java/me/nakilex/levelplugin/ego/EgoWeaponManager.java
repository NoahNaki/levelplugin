package me.nakilex.levelplugin.ego;

import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.items.data.CustomItem;
import me.nakilex.levelplugin.items.listeners.WeaponStatsListener;
import me.nakilex.levelplugin.player.attributes.managers.StatsManager;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EgoWeaponManager {
    private static final EgoWeaponManager instance = new EgoWeaponManager();

    public static EgoWeaponManager getInstance() {
        return instance;
    }

    private final Map<UUID, EgoWeapon> weaponMap = new HashMap<>();
    private final Map<String, EgoWeapon> prototypes = new HashMap<>();

    private EgoWeaponManager() {
        EgoWeapon archer = new EgoWeapon("archer_ego", "Ego Archer Bow", EgoRarity.COMMON);
        archer.addRankSkill(1, "Quick_Shot");
        archer.addRankSkill(5, "Backstep");
        archer.addRankSkill(8, "Dragon_Piercer");
        prototypes.put("archer", archer);

        EgoWeapon phoenix = new EgoWeapon("phoenix_ego", "Ego Phoenix Bow", EgoRarity.RARE);
        phoenix.addRankSkill(1, "Blazing_Feathers");
        phoenix.addRankSkill(5, "Flameburst_Convergence");
        phoenix.addRankSkill(8, "Phoenix_Rebirth");
        prototypes.put("phoenix", phoenix);

        EgoWeapon warrior = new EgoWeapon("warrior_ego", "Ego Warrior Axe", EgoRarity.COMMON);
        warrior.addRankSkill(1, "Brutal_Strike");
        warrior.addRankSkill(5, "Charge");
        warrior.addRankSkill(8, "Rampage");
        prototypes.put("warrior", warrior);

        // New classes
        EgoWeapon barbarian = new EgoWeapon("barbarian_ego", "Ego Barbarian Shovel", EgoRarity.UNCOMMON);
        barbarian.addRankSkill(1, "Rageblade");
        barbarian.addRankSkill(5, "Primal_Axe");
        barbarian.addRankSkill(8, "Eternal_Fury");
        prototypes.put("barbarian", barbarian);

        EgoWeapon paladin = new EgoWeapon("paladin_ego", "Ego Paladin Sword", EgoRarity.RARE);
        paladin.addRankSkill(1, "Holy_Strike");
        paladin.addRankSkill(5, "Bound_Seal");
        paladin.addRankSkill(8, "Last_Stand");
        prototypes.put("paladin", paladin);
    }

    public EgoWeapon getPrototype(String key) {
        return prototypes.get(key);
    }

    public EgoWeapon getWeapon(UUID uuid) {
        return weaponMap.get(uuid);
    }

    public void setWeapon(UUID uuid, EgoWeapon weapon) {
        weaponMap.put(uuid, weapon);
    }

    public void addXp(Player player, int xp) {
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || !heldItem.hasItemMeta()) {
            return;
        }

        ItemMeta meta = heldItem.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING)) {
            return;
        }

        String id = pdc.get(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING);
        String key = id.split("_")[0];
        EgoWeapon proto = prototypes.get(key);
        if (proto == null) {
            return;
        }

        EgoWeapon weapon = getWeapon(player.getUniqueId());
        if (weapon == null || !weapon.getId().equals(id)) {
            weapon = proto.copy();
            weapon.addExp(0);
            int r = pdc.getOrDefault(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER, 1);
            int e = pdc.getOrDefault(ItemUtil.EGO_EXP_KEY, PersistentDataType.INTEGER, 0);
            if (pdc.has(ItemUtil.EGO_RARITY_KEY, PersistentDataType.STRING)) {
                try {
                    me.nakilex.levelplugin.ego.EgoRarity rar = me.nakilex.levelplugin.ego.EgoRarity.valueOf(pdc.get(ItemUtil.EGO_RARITY_KEY, PersistentDataType.STRING));
                    weapon.setRarity(rar);
                } catch (Exception ignored) {}
            }
            while (weapon.getRank() < r) weapon.addExp(weapon.expToNextRank());
            weapon.addExp(e);
            setWeapon(player.getUniqueId(), weapon);
        }

        boolean leveled = weapon.addExp(xp);
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && hand.hasItemMeta()) {
            ItemMeta meta2 = hand.getItemMeta();
            PersistentDataContainer pdc2 = meta2.getPersistentDataContainer();
            if (pdc2.has(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING)) {
                pdc2.set(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER, weapon.getRank());
                pdc2.set(ItemUtil.EGO_EXP_KEY, PersistentDataType.INTEGER, weapon.getExp());
                pdc2.set(ItemUtil.EGO_RARITY_KEY, PersistentDataType.STRING, weapon.getRarity().name());
                hand.setItemMeta(meta2);
                ItemUtil.updateEgoWeaponTooltip(hand, player);
                if (leveled) {
                    CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(hand);
                    if (ci != null) {
                        WeaponStatsListener wsl = new WeaponStatsListener();
                        wsl.removeWeaponStats(player, ci, hand);
                        wsl.addWeaponStats(player, ci, hand);
                        StatsManager.getInstance().recalcDerivedStats(player);
                    }
                }
            }
        }
        if (leveled) {
            player.sendMessage("§aYour weapon " + weapon.getName() + " is now Rank " + weapon.getRank() + "!");
        }
    }

    /**
     * Evolve the player's currently held ego weapon if possible.
     * Returns true if evolution succeeded.
     */
    public boolean evolveWeapon(Player player, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING)) return false;

        int rank = pdc.getOrDefault(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER, 1);
        if (rank < 10) {
            player.sendMessage("§cWeapon must be Rank 10 to evolve!");
            return false;
        }

        // Load weapon instance
        EgoWeapon weapon = getWeapon(player.getUniqueId());
        if (weapon == null) {
            // reconstruct from PDC using prototype
            String id = pdc.get(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING);
            String key = id.split("_")[0];
            EgoWeapon proto = prototypes.get(key);
            if (proto != null) {
                weapon = proto.copy();
                weapon.addExp(0);
            } else {
                return false;
            }
        }

        weapon.evolve();
        setWeapon(player.getUniqueId(), weapon);

        // Reset upgrade level so costs scale with new rarity
        CustomItem ci = ItemManager.getInstance().getCustomItemFromItemStack(stack);
        if (ci != null) {
            ci.setUpgradeLevel(0);
            ItemUtil.updateUpgradeLevel(stack, 0);
        }

        pdc.set(ItemUtil.EGO_RARITY_KEY, PersistentDataType.STRING, weapon.getRarity().name());
        pdc.set(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER, weapon.getRank());
        pdc.set(ItemUtil.EGO_EXP_KEY, PersistentDataType.INTEGER, weapon.getExp());
        meta.setDisplayName(weapon.getRarity().getColor() + weapon.getName());
        stack.setItemMeta(meta);
        ItemUtil.updateEgoWeaponTooltip(stack, player);

        // Reapply stats
        if (ci != null) {
            WeaponStatsListener wsl = new WeaponStatsListener();
            wsl.removeWeaponStats(player, ci, stack);
            wsl.addWeaponStats(player, ci, stack);
            StatsManager.getInstance().recalcDerivedStats(player);
        }

        player.sendMessage("§aYour weapon has evolved to " + weapon.getRarity().name() + " rarity!");
        return true;
    }

    /**
     * Create an ItemStack representing this Ego Weapon using an existing item template.
     */
    public ItemStack createWeaponItem(EgoWeapon weapon, int templateId) {
        CustomItem base = ItemManager.getInstance().rollNewInstance(templateId);
        ItemStack stack = ItemUtil.createItemStackFromCustomItem(base, 1, null);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(ItemUtil.EGO_ID_KEY, PersistentDataType.STRING, weapon.getId());
            pdc.set(ItemUtil.EGO_RANK_KEY, PersistentDataType.INTEGER, weapon.getRank());
            pdc.set(ItemUtil.EGO_EXP_KEY, PersistentDataType.INTEGER, weapon.getExp());
            pdc.set(ItemUtil.EGO_RARITY_KEY, PersistentDataType.STRING, weapon.getRarity().name());
            meta.setDisplayName(weapon.getRarity().getColor() + weapon.getName());
            stack.setItemMeta(meta);
        }
        ItemUtil.updateEgoWeaponTooltip(stack, null);

        // Apply nexo model based on weapon prefix
        String prefix = weapon.getId().split("_")[0];
        String nexoId = switch (prefix) {
            case "archer" -> "archer_bow";
            case "warrior" -> "warrior_sword";
            case "barbarian" -> "axe_babarian";
            case "paladin" -> "paladin_hammer";
            default -> null;
        };
        if (nexoId != null) {
            ItemBuilder builder = NexoItems.itemFromId(nexoId);
            if (builder != null) {
                ItemStack model = builder.build();
                ItemMeta mMeta = model.getItemMeta();
                if (mMeta != null) {
                    meta.setCustomModelData(mMeta.getCustomModelData());
                    stack.setType(model.getType());
                    stack.setItemMeta(meta);
                }
            }
        }
        return stack;
    }
}
