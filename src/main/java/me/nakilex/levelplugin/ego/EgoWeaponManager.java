package me.nakilex.levelplugin.ego;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EgoWeaponManager {
    private static final EgoWeaponManager instance = new EgoWeaponManager();

    public static EgoWeaponManager getInstance() {
        return instance;
    }

    private final Map<UUID, EgoWeapon> weaponMap = new HashMap<>();

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
}
