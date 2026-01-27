package me.nakilex.levelplugin.utils.gui.widgets;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public record GuiContext(Player player, Inventory inventory) {}
