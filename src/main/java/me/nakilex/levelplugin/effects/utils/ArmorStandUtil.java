package me.nakilex.levelplugin.effects.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.World;

public class ArmorStandUtil {

    public static ArmorStand createSwordArmorStand(Location location) {
        World world = location.getWorld();
        if (world == null) return null;

        ArmorStand armorStand = (ArmorStand) world.spawnEntity(location, EntityType.ARMOR_STAND);
        armorStand.setInvisible(true);
        armorStand.setGravity(false);
        armorStand.setMarker(true);
        armorStand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
        return armorStand;
    }
}
