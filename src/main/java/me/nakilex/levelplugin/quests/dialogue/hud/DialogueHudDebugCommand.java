package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.resourcepack.ResourcePackFragmentStatus;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Admin diagnostics for dialogue HUD pack and glyph activation state. */
public final class DialogueHudDebugCommand implements TabExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("levelplugin.admin")) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "You do not have permission to do that.");
            return true;
        }

        DialogueHudResourcePackManager manager = DialogueHudResourcePackManager.getInstance();
        if (manager == null) {
            ChatMessageUtil.send(sender, MessageType.WARNING, "Dialogue HUD integration has not initialized yet.");
            return true;
        }

        Player target = args.length >= 1 ? sender.getServer().getPlayerExact(args[0])
                : sender instanceof Player player ? player : null;
        sendDebug(sender, manager, target);
        return true;
    }

    public static void sendDebug(CommandSender sender, DialogueHudResourcePackManager manager, Player target) {
        ResourcePackFragmentStatus status = manager.status();
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.AQUA + "Dialogue HUD debug:");
        sendStatus(sender, "renderer enabled", manager.rendererEnabled());
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "renderer mode: " + ChatColor.WHITE + manager.rendererMode());
        sendStatus(sender, "use-resource-pack-glyphs", manager.useResourcePackGlyphs());
        sendStatus(sender, "require-client-pack-loaded", manager.requireClientPackLoaded());
        sendStatus(sender, "debug-force-glyphs", manager.debugForceGlyphs());
        sendStatus(sender, "server glyph files available", manager.serverGlyphFilesReady());
        sendStatus(sender, "Nexo external_packs exists", status.nexoExternalPacksExists());
        sendStatus(sender, "levelplugin-dialogue-hud installed", status.installed());
        status.requiredFiles().forEach((file, exists) -> sendStatus(sender, file + " exists", exists));
        if (target != null) {
            sendStatus(sender, "client pack loaded for " + target.getName(), manager.packStatusListener().hasLoadedPack(target));
            sendStatus(sender, "glyphs usable for " + target.getName(), manager.canRenderGlyphUi(target));
            ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "glyph decision: " + ChatColor.WHITE
                    + manager.glyphDebugReason(target));
        } else {
            ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY
                    + "client pack loaded: " + ChatColor.WHITE + "no player selected");
        }
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "dialogue font key: " + ChatColor.WHITE
                + DialogueHudGlyphs.DIALOGUE_FONT.asString());
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "offset font key: " + ChatColor.WHITE
                + DialogueHudGlyphs.OFFSET_FONT.asString());
    }

    private static void sendStatus(CommandSender sender, String label, boolean enabled) {
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + label + ": "
                + (enabled ? ChatColor.GREEN + "yes" : ChatColor.RED + "no"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(input))
                    .toList();
        }
        return Collections.emptyList();
    }
}
