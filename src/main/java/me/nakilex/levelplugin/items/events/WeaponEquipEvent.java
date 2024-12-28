package me.nakilex.levelplugin.items.events;

import me.nakilex.levelplugin.items.data.WeaponType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Fired whenever the player equips or unequips a "weapon" item
 * in their main hand or off-hand.
 */
public final class WeaponEquipEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;

    private final EquipMethod equipMethod;
    private final WeaponType weaponType;
    private final HandSlot handSlot;

    private ItemStack oldWeapon;
    private ItemStack newWeapon;

    /**
     * @param player      The player wielding or unwielding the weapon.
     * @param equipMethod The method of equipping (hotbar swap, manual, etc.).
     * @param weaponType  The type of weapon recognized (SWORD, AXE, etc.).
     * @param handSlot    Main hand or Off-hand.
     * @param oldWeapon   The previously held item in that hand (null or AIR if empty).
     * @param newWeapon   The new item going into that hand.
     */
    public WeaponEquipEvent(Player player,
                            EquipMethod equipMethod,
                            WeaponType weaponType,
                            HandSlot handSlot,
                            ItemStack oldWeapon,
                            ItemStack newWeapon) {
        super(player);
        this.equipMethod = equipMethod;
        this.weaponType = weaponType;
        this.handSlot = handSlot;
        this.oldWeapon = oldWeapon;
        this.newWeapon = newWeapon;
    }

    public enum EquipMethod {
        HOTBAR_SWAP,
        MANUAL,     // For drag & drop inside inventory
        HELD_ITEM_CHANGE,
        OFFHAND_SWAP,
        DEATH,      // If you want to handle weapon drops on death (optional)
        OTHER
    }

    /**
     * Which hand is changing? Main or Off-hand
     */
    public enum HandSlot {
        MAIN_HAND,
        OFF_HAND
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public EquipMethod getEquipMethod() {
        return equipMethod;
    }

    public WeaponType getWeaponType() {
        return weaponType;
    }

    public HandSlot getHandSlot() {
        return handSlot;
    }

    public ItemStack getOldWeapon() {
        return oldWeapon;
    }

    public void setOldWeapon(ItemStack oldWeapon) {
        this.oldWeapon = oldWeapon;
    }

    public ItemStack getNewWeapon() {
        return newWeapon;
    }

    public void setNewWeapon(ItemStack newWeapon) {
        this.newWeapon = newWeapon;
    }

    /**
     * Standard event boilerplate
     */
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
