package me.nakilex.levelplugin.enchanting.commands;

import me.nakilex.levelplugin.enchanting.gui.EnchantGUI;
import me.nakilex.levelplugin.quests.def.SharpestSecretQuest;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EnchantCommand implements CommandExecutor {
    private final EnchantGUI gui;
    public EnchantCommand(EnchantGUI gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!SharpestSecretQuest.canAccessEnchanting(player.getUniqueId())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Complete \"The Sharpest Secret\" to unlock Osiris' enchanting table.");
            return true;
        }
        gui.open(player);
        return true;
    }
}
