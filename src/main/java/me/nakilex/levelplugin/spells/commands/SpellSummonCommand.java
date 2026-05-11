package me.nakilex.levelplugin.spells.commands;

import me.nakilex.levelplugin.spells.gui.SpellSummonGUI;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpellSummonCommand implements CommandExecutor {
    private final SpellSummonGUI summonGUI;

    public SpellSummonCommand(SpellSummonGUI summonGUI) {
        this.summonGUI = summonGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (summonGUI == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "Spell summons are currently unavailable.");
            return true;
        }
        summonGUI.open(player);
        return true;
    }
}
