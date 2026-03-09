package me.nakilex.levelplugin.utils.gui.flow;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Generic GUI action flow contract used by upgrade/repair/reroll style operations.
 * Implementations are expected to follow: validate -> apply -> feedback.
 */
public interface GuiActionOperation {

    ItemStack createActionButton(Player player, Inventory gui);

    void execute(Player player, Inventory gui);
}
