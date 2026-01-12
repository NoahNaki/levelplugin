package me.nakilex.npc.core.model;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class NpcEquipment {
    private final Map<EquipmentSlot, ItemStack> items = new EnumMap<>(EquipmentSlot.class);

    public Map<EquipmentSlot, ItemStack> getItems() {
        return Collections.unmodifiableMap(items);
    }

    public void setItem(EquipmentSlot slot, ItemStack stack) {
        if (stack == null) {
            items.remove(slot);
        } else {
            items.put(slot, stack.clone());
        }
    }

    public ItemStack getItem(EquipmentSlot slot) {
        ItemStack stack = items.get(slot);
        return stack == null ? null : stack.clone();
    }

    public void clear() {
        items.clear();
    }

    public NpcEquipment copy() {
        NpcEquipment copy = new NpcEquipment();
        items.forEach((slot, stack) -> copy.items.put(slot, stack.clone()));
        return copy;
    }
}
