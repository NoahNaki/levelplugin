package me.nakilex.levelplugin.pet.commands;

import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.gui.PetGUI;
import me.nakilex.levelplugin.pet.utils.PetChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class PetCommand implements CommandExecutor, TabCompleter {
    private final PetManager petManager;
    private final PetGUI petGUI;

    public PetCommand(PetManager petManager, PetGUI petGUI) {
        this.petManager = petManager;
        this.petGUI = petGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Players only.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            petManager.reload();
            PetChatUtil.send(player, "Reloaded pet definitions.");
            return true;
        }
        petGUI.open(player, 0);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload");
        }
        return List.of();
    }
}
