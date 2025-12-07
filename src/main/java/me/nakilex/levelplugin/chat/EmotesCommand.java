package me.nakilex.levelplugin.chat;

import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Command to display available chat emotes and their glyph previews. */
public class EmotesCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        List<String> shortcodes = ChatUtil.getEmojiShortcodes();
        if (shortcodes.isEmpty()) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO, "No emotes are currently available.");
            return true;
        }

        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Available emotes (" + shortcodes.size() + "):");

        Map<String, String> glyphs = ChatUtil.getEmojiGlyphs();
        if (sender instanceof Player player) {
            for (String code : shortcodes) {
                String preview = ChatColor.GRAY + ":" + code + ": " + ChatColor.WHITE + glyphs.get(code);
                ChatFormatter.sendIndentedMessage(player, preview);
            }
        } else {
            for (String code : shortcodes) {
                sender.sendMessage(ChatColor.GRAY + ":" + code + ": " + ChatColor.WHITE + glyphs.get(code));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
