package me.nakilex.levelplugin.npc.system.trait;

import me.nakilex.levelplugin.npc.system.NPC;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public class EquipmentTrait implements NpcTrait {
    public enum EquipmentSlot {
        HAND,
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS
    }

    private final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);

    public void set(EquipmentSlot slot, ItemStack item) {
        equipment.put(slot, item);
    }

    @Override
    public void onSpawn(NPC npc) {
        if (npc.getEntity() instanceof org.bukkit.entity.LivingEntity living) {
            equipment.forEach((slot, item) -> {
                if (item == null) {
                    return;
                }
                switch (slot) {
                    case HAND -> living.getEquipment().setItemInMainHand(item);
                    case HELMET -> living.getEquipment().setHelmet(item);
                    case CHESTPLATE -> living.getEquipment().setChestplate(item);
                    case LEGGINGS -> living.getEquipment().setLeggings(item);
                    case BOOTS -> living.getEquipment().setBoots(item);
                }
            });
        }
    }
}
