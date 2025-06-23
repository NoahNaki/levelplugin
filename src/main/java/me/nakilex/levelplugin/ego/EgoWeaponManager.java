package me.nakilex.levelplugin.ego;

import me.nakilex.levelplugin.items.managers.ItemManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.items.data.CustomItem;
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

        EgoWeapon phoenix = new EgoWeapon("phoenix_ego", "Ego Phoenix Bow", EgoRarity.COMMON);
        phoenix.addRankSkill(1, "Blazing_Feathers");
        phoenix.addRankSkill(5, "Flameburst_Convergence");
        phoenix.addRankSkill(8, "Phoenix_Rebirth");
        prototypes.put("phoenix", phoenix);
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
        EgoWeapon weapon = getWeapon(player.getUniqueId());
        if (weapon == null) return;
        boolean leveled = weapon.addExp(xp);
        if (leveled) {
            player.sendMessage("§aYour weapon ranked up to " + weapon.getRank() + "!");
        }
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
            meta.setDisplayName(weapon.getRarity().getColor() + weapon.getName());
            stack.setItemMeta(meta);
        }
        ItemUtil.updateCustomItemTooltip(stack, null);
        return stack;
    }
}
