package me.nakilex.levelplugin.effects;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.World;
import java.util.ArrayList;
import java.util.List;

public class SwordFireEffect {

    private final List<ArmorStand> armorStands = new ArrayList<>();
    private final int numSwords = 5;

    public void spawnSwords(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        for (int i = 0; i < numSwords; i++) {
            Location loc = center.clone();
            double angle = Math.toRadians((360.0 / numSwords) * i); // Calculate spawn angle
            loc.add(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5); // Offset position

            // Set direction for movement
            loc.setDirection(loc.toVector().subtract(center.toVector()).normalize());

            ArmorStand armorStand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setMarker(true);
            armorStand.getEquipment().setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));

            armorStands.add(armorStand);
        }

    }

    public List<ArmorStand> getArmorStands() {
        return armorStands;
    }
}