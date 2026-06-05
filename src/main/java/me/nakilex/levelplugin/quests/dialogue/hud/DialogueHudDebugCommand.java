package me.nakilex.levelplugin.quests.dialogue.hud;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dialogue.DialogueAnswer;
import me.nakilex.levelplugin.resourcepack.ResourcePackFragmentStatus;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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

        if (args.length >= 1 && "background".equalsIgnoreCase(args[0])) {
            Player target = args.length >= 2 ? sender.getServer().getPlayerExact(args[1])
                    : sender instanceof Player player ? player : null;
            sendDebug(sender, manager, target);
            sendBackgroundTest(sender, target);
            return true;
        }

        if (args.length >= 1 && "layout".equalsIgnoreCase(args[0])) {
            Player target = args.length >= 2 ? sender.getServer().getPlayerExact(args[1])
                    : sender instanceof Player player ? player : null;
            sendDebug(sender, manager, target);
            sendLayoutTest(sender, manager, target);
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
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "background-offset: " + ChatColor.WHITE + manager.backgroundOffset());
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "text-offset-after-background: " + ChatColor.WHITE
                + manager.textOffsetAfterBackground());
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
        sendOffsetDebug(sender, manager.offsetGlyphDebug());
        sendBackgroundDebug(sender, manager.backgroundGlyphDebug());
    }

    private static void sendOffsetDebug(CommandSender sender, DialogueHudResourcePackManager.OffsetGlyphDebug debug) {
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.AQUA + "Dialogue offset glyph debug:");
        sendStatus(sender, "offset_chars.json exists", debug.fontExists());
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "offset_chars.json path: " + ChatColor.WHITE + debug.fontPath());
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "active negative offset glyph: " + ChatColor.WHITE
                + DialogueHudGlyphs.unicode(debug.activeNegative()));
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "active positive offset glyph: " + ChatColor.WHITE
                + DialogueHudGlyphs.unicode(debug.activePositive()));
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "offset glyph source: " + ChatColor.WHITE + debug.source());
        if (debug.detectedPair() != null) {
            ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "detected offset pair: " + ChatColor.WHITE
                    + DialogueHudGlyphs.unicode(debug.detectedPair().negative()) + " / "
                    + DialogueHudGlyphs.unicode(debug.detectedPair().positive()));
        }
    }

    private static void sendBackgroundDebug(CommandSender sender, DialogueHudResourcePackManager.BackgroundGlyphDebug debug) {
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.AQUA + "Dialogue background glyph debug:");
        sendStatus(sender, "dialogue_background.png exists", debug.textureExists());
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "dialogue_background.png path: " + ChatColor.WHITE
                + debug.texturePath());
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "dialogue_background.png size: " + ChatColor.WHITE
                + debug.size());
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "dialogue_background.png fully transparent: "
                + (Boolean.TRUE.equals(debug.fullyTransparent()) ? ChatColor.RED + "yes" : ChatColor.GREEN + "no"));
        sendStatus(sender, "dialogue.json exists", debug.fontExists());
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "dialogue.json path: " + ChatColor.WHITE + debug.fontPath());
        DialogueHudResourcePackManager.FontProviderDebug provider = debug.provider();
        if (provider == null) {
            ChatMessageUtil.send(sender, MessageType.WARNING, "dialogue.json provider for \\uE100: missing");
        } else {
            ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "provider file path: " + ChatColor.WHITE
                    + provider.providerFilePath());
            ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "provider file: " + ChatColor.WHITE + provider.file());
            ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "provider ascent: " + ChatColor.WHITE + provider.ascent());
            ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "provider height: " + ChatColor.WHITE + provider.height());
            ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + "provider chars: " + ChatColor.WHITE + provider.chars());
        }
        for (String warning : debug.warnings()) {
            ChatMessageUtil.send(sender, MessageType.WARNING, "Dialogue HUD background warning: " + warning);
        }
    }

    private static void sendBackgroundTest(CommandSender sender, Player target) {
        if (target == null) {
            ChatMessageUtil.send(sender, MessageType.WARNING, "Use /dialoguehuddebug background <player> from console.");
            return;
        }
        ChatMessageUtil.send(sender, MessageType.INFO, "Sending dialogue background glyph action-bar test to " + target.getName() + ".");
        target.sendActionBar(DialogueHudGlyphs.background());
        Main plugin = Main.getInstance();
        if (plugin == null) return;
        target.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                target.sendActionBar(Component.empty()
                        .append(DialogueHudGlyphs.background())
                        .append(DialogueHudGlyphs.background())
                        .append(DialogueHudGlyphs.background()));
            }
        }, 40L);
        target.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                target.sendActionBar(DialogueHudGlyphs.defaultText("Dialogue HUD background glyph test complete.", NamedTextColor.YELLOW));
            }
        }, 80L);
    }

    private static void sendLayoutTest(CommandSender sender, DialogueHudResourcePackManager manager, Player target) {
        if (target == null) {
            ChatMessageUtil.send(sender, MessageType.WARNING, "Use /dialoguehuddebug layout <player> from console.");
            return;
        }
        Main plugin = Main.getInstance();
        if (plugin == null) return;

        int dialogueWidth = plugin.getConfig().getInt("dialogue-hud.layout.dialogue.width", 209);
        int textX = plugin.getConfig().getInt("dialogue-hud.layout.dialogue.text-x", 22);
        int nameplateX = plugin.getConfig().getInt("dialogue-hud.layout.nameplate.x", 18);
        int answerWidth = plugin.getConfig().getInt("dialogue-hud.layout.answers.background-width", 134);
        int answerTextX = plugin.getConfig().getInt("dialogue-hud.layout.answers.text-x", 14);

        ChatMessageUtil.send(sender, MessageType.INFO, "Sending dialogue HUD layered layout test to " + target.getName() + ".");

        // 0 ticks: dialogue box only.
        target.sendActionBar(DialogueHudGlyphs.background());

        // 20 ticks: dialogue box + text layered inside the box.
        target.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                target.sendActionBar(DialogueHudGlyphs.background()
                        .append(DialogueHudGlyphs.offset(-dialogueWidth + textX))
                        .append(DialogueHudGlyphs.defaultText("Text inside box", NamedTextColor.WHITE)));
            }
        }, 20L);

        // 40 ticks: dialogue box + nameplate layer returned to the dialogue start.
        target.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                target.sendActionBar(DialogueHudGlyphs.background()
                        .append(DialogueHudGlyphs.offset(-dialogueWidth + nameplateX))
                        .append(DialogueHudGlyphs.nameplateLeft())
                        .append(DialogueHudGlyphs.nameplateMiddle())
                        .append(DialogueHudGlyphs.nameplateMiddle())
                        .append(DialogueHudGlyphs.nameplateRight()));
            }
        }, 40L);

        // 60 ticks: answer box only.
        target.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                target.sendActionBar(DialogueHudGlyphs.answerBackground());
            }
        }, 60L);

        // 80 ticks: answer box + selected arrow/text layered inside the box.
        target.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                target.sendActionBar(DialogueHudGlyphs.answerBackground()
                        .append(DialogueHudGlyphs.offset(-answerWidth + answerTextX))
                        .append(DialogueHudGlyphs.selector())
                        .append(DialogueHudGlyphs.offset(6))
                        .append(DialogueHudGlyphs.defaultText("1. Test Answer", NamedTextColor.WHITE)));
            }
        }, 80L);

        // 100 ticks: full configured layout with dialogue, nameplate, and answers.
        target.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (target.isOnline()) {
                DialogueHudLayout layout = new DialogueHudLayout(plugin, manager);
                target.sendActionBar(layout.compose(
                        Component.text("Janitor Ilta", NamedTextColor.YELLOW),
                        List.of(Component.text("Text inside the dialogue box.", NamedTextColor.WHITE)),
                        null,
                        List.of(DialogueAnswer.of("continue", "Continue", null),
                                DialogueAnswer.of("leave", "Leave", null)),
                        0));
            }
        }, 100L);
    }

    private static void sendStatus(CommandSender sender, String label, boolean enabled) {
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + label + ": "
                + (enabled ? ChatColor.GREEN + "yes" : ChatColor.RED + "no"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            List<String> completions = new ArrayList<>();
            if ("background".startsWith(input)) completions.add("background");
            if ("layout".startsWith(input)) completions.add("layout");
            sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(input))
                    .forEach(completions::add);
            return completions;
        }
        if (args.length == 2 && ("background".equalsIgnoreCase(args[0]) || "layout".equalsIgnoreCase(args[0]))) {
            String input = args[1].toLowerCase(Locale.ROOT);
            return sender.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(input))
                    .toList();
        }
        return Collections.emptyList();
    }
}
