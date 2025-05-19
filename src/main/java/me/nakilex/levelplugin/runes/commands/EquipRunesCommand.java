package me.nakilex.levelplugin.runes.commands;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.runes.gui.EquipRunesGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EquipRunesCommand implements CommandExecutor {
    private final EquipRunesGUI gui;

    public EquipRunesCommand(EquipRunesGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command cmd,
                             String label,
                             String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players may equip runes.");
            return true;
        }
        gui.open((Player) sender);
        return true;
    }
}
