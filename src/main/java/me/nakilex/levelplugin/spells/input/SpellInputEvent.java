package me.nakilex.levelplugin.spells.input;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class SpellInputEvent extends PlayerEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final SpellInputType inputType;
    private final SpellInputMode inputMode;
    private final String inputSequence;

    public SpellInputEvent(Player player, SpellInputType inputType, SpellInputMode inputMode, String inputSequence) {
        super(player);
        this.inputType = inputType;
        this.inputMode = inputMode;
        this.inputSequence = inputSequence;
    }

    public SpellInputType getInputType() {
        return inputType;
    }

    public SpellInputMode getInputMode() {
        return inputMode;
    }

    public String getInputSequence() {
        return inputSequence;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
