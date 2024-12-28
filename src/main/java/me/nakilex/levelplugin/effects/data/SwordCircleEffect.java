package me.nakilex.levelplugin.effects.data;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.World;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.List;

public class SwordCircleEffect {

    private final List<ArmorStand> armorStands = new ArrayList<>();
    private final int numSwords = 6;
    private final double radius = 2.0;

    public void spawnSwords(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        for (int i = 0; i < numSwords; i++) {
            double angle = 2 * Math.PI * i / numSwords;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            Location swordLocation = new Location(world, x, center.getY(), z);

            ArmorStand armorStand = (ArmorStand) world.spawnEntity(swordLocation, EntityType.ARMOR_STAND);
            armorStand.setInvisible(false);
            armorStand.setGravity(false);
            armorStand.setMarker(true);
            armorStand.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            armorStand.setLeftArmPose(new EulerAngle(0, 0, Math.toRadians(-90))); // Left arm horizontal
            armorStand.setRightArmPose(new EulerAngle(0, 0, Math.toRadians(90))); // Right arm horizontal

            // Make sword horizontal by tilting head
            armorStand.setHeadPose(armorStand.getHeadPose().setX(Math.toRadians(90)));

            armorStands.add(armorStand);
        }
    }

    public void removeSwords() {
        for (ArmorStand armorStand : armorStands) {
            armorStand.remove();
        }
        armorStands.clear();
    }

    public List<ArmorStand> getArmorStands() {
        return armorStands;
    }
}
