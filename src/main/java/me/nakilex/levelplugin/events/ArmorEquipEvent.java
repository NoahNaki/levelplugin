package me.nakilex.levelplugin.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Fired whenever the player equips or unequips an armor piece.
 */
public final class ArmorEquipEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private boolean cancelled = false;

    private final EquipMethod equipMethod;
    private final ArmorType armorType;
    private ItemStack oldArmorPiece;
    private ItemStack newArmorPiece;

    /**
     * @param player        The player equipping or unequipping the armor.
     * @param equipMethod   The method of equipping (shift-click, drag, hotbar swap, etc.).
     * @param armorType     The armor slot type (helmet, chestplate, leggings, boots).
     * @param oldArmorPiece The item previously in that armor slot (may be null or AIR).
     * @param newArmorPiece The new item going into that slot (may be null or AIR).
     */
    public ArmorEquipEvent(Player player,
                           EquipMethod equipMethod,
                           ArmorType armorType,
                           ItemStack oldArmorPiece,
                           ItemStack newArmorPiece) {
        super(player);
        this.equipMethod = equipMethod;
        this.armorType = armorType;
        this.oldArmorPiece = oldArmorPiece;
        this.newArmorPiece = newArmorPiece;
    }

    /**
     * How the armor was equipped or unequipped.
     */
    public enum EquipMethod {
        SHIFT_CLICK,
        DRAG,
        PICK_DROP,
        HOTBAR,
        HOTBAR_SWAP,
        DISPENSER,
        BROKE,
        DEATH
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

    public ArmorType getArmorType() {
        return armorType;
    }

    public ItemStack getOldArmorPiece() {
        return oldArmorPiece;
    }

    public void setOldArmorPiece(ItemStack oldArmorPiece) {
        this.oldArmorPiece = oldArmorPiece;
    }

    public ItemStack getNewArmorPiece() {
        return newArmorPiece;
    }

    public void setNewArmorPiece(ItemStack newArmorPiece) {
        this.newArmorPiece = newArmorPiece;
    }

    /**
     * Standard Bukkit event boilerplate.
     */
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
