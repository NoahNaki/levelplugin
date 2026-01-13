package me.nakilex.levelplugin.pathfinding.npc;

import me.nakilex.levelplugin.npc.system.NPC;
import me.nakilex.levelplugin.npc.system.trait.EquipmentTrait;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Base mercenary profile providing common speed and netherite armor.
 * Subclasses supply their weapon and combat behaviour.
 */
public abstract class AbstractMercenary implements PathNpc {
    @Override
    public float speedMultiplier() {
        return 4.5f;
    }

    @Override
    public void equip(NPC npc) {
        EquipmentTrait equip = npc.getOrAddTrait(EquipmentTrait.class);
        equip.set(EquipmentTrait.EquipmentSlot.HAND, weapon());
        equip.set(EquipmentTrait.EquipmentSlot.HELMET, new ItemStack(Material.NETHERITE_HELMET));
        equip.set(EquipmentTrait.EquipmentSlot.CHESTPLATE, new ItemStack(Material.NETHERITE_CHESTPLATE));
        equip.set(EquipmentTrait.EquipmentSlot.LEGGINGS, new ItemStack(Material.NETHERITE_LEGGINGS));
        equip.set(EquipmentTrait.EquipmentSlot.BOOTS, new ItemStack(Material.NETHERITE_BOOTS));
    }

    /** Return the weapon to place in the NPC's main hand. */
    protected abstract ItemStack weapon();
}
