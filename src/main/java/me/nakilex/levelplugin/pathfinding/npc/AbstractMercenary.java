package me.nakilex.levelplugin.pathfinding.npc;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
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
        Equipment equip = npc.getOrAddTrait(Equipment.class);
        equip.set(Equipment.EquipmentSlot.HAND, weapon());
        equip.set(Equipment.EquipmentSlot.HELMET, new ItemStack(Material.NETHERITE_HELMET));
        equip.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.NETHERITE_CHESTPLATE));
        equip.set(Equipment.EquipmentSlot.LEGGINGS, new ItemStack(Material.NETHERITE_LEGGINGS));
        equip.set(Equipment.EquipmentSlot.BOOTS, new ItemStack(Material.NETHERITE_BOOTS));
    }

    /** Return the weapon to place in the NPC's main hand. */
    protected abstract ItemStack weapon();
}
