package me.nakilex.levelplugin.commands;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import me.nakilex.levelplugin.player.fishing.minigame.FishingDifficultyProfile;
import me.nakilex.levelplugin.player.fishing.minigame.FishingMiniGameManager;
import me.nakilex.levelplugin.player.fishing.resourcepack.FishingResourcePackManager;
import me.nakilex.levelplugin.quests.dialogue.hud.DialogueHudResourcePackManager;
import me.nakilex.levelplugin.quests.dialogue.hud.DialogueHudDebugCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * Administrative command for LevelPlugin maintenance tasks such as
 * reloading configuration files at runtime.
 */
public class LevelPluginCommand implements TabExecutor {

    private static final String SUB_RELOAD = "reload";
    private static final String SUB_FISHING_PACK = "fishingpack";
    private static final String SUB_INFO = "info";
    private static final String SUB_FISHING = "fishing";
    private static final String SUB_DIALOGUE_HUD = "dialoguehud";
    private static final String SUB_MINIGAME = "minigame";
    private static final String SUB_TEST = "test";

    private final Main plugin;

    public LevelPluginCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }
        if (!sender.hasPermission("levelplugin.admin")) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "You do not have permission to do that.");
            return true;
        }

        if (SUB_RELOAD.equalsIgnoreCase(args[0])) {
            long start = System.currentTimeMillis();
            plugin.reloadConfigValues();
            long elapsed = System.currentTimeMillis() - start;
            ChatMessageUtil.send(sender, MessageType.SUCCESS,
                    "Reloaded LevelPlugin configuration in " + elapsed + "ms.");
            return true;
        }
        if (SUB_FISHING_PACK.equalsIgnoreCase(args[0]) && args.length >= 2 && SUB_INFO.equalsIgnoreCase(args[1])) {
            sendFishingPackInfo(sender);
            return true;
        }
        if (SUB_DIALOGUE_HUD.equalsIgnoreCase(args[0]) && args.length >= 2 && SUB_INFO.equalsIgnoreCase(args[1])) {
            Player target = args.length >= 3 ? plugin.getServer().getPlayerExact(args[2]) : sender instanceof Player player ? player : null;
            sendDialogueHudInfo(sender, target);
            return true;
        }
        if (SUB_FISHING.equalsIgnoreCase(args[0]) && args.length >= 4
                && SUB_MINIGAME.equalsIgnoreCase(args[1]) && SUB_TEST.equalsIgnoreCase(args[2])) {
            startFishingMiniGameTest(sender, args[3]);
            return true;
        }
        sendUsage(sender, label);
        return true;
    }

    private void sendFishingPackInfo(CommandSender sender) {
        FishingResourcePackManager manager = FishingResourcePackManager.getInstance();
        if (manager == null) {
            ChatMessageUtil.send(sender, MessageType.WARNING, "Fishing resource-pack integration has not initialized yet.");
            return;
        }
        FishingResourcePackManager.FishingPackStatus status = manager.status();
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.AQUA + "Fishing pack integration status:");
        sendStatus(sender, "Nexo external_packs exists", status.nexoExternalPacksExists());
        sendStatus(sender, "levelplugin-fishing-games installed", status.installed());
        sendStatus(sender, "pack.mcmeta exists", status.packMetadataExists());
        sendStatus(sender, "assets/customfishing/font/default.json exists", status.defaultFontExists());
        sendStatus(sender, "assets/customfishing/font/icons.json exists", status.iconsFontExists());
        sendStatus(sender, "assets/customfishing/font/offset_chars.json exists", status.offsetFontExists());
        sendStatus(sender, "Glyph UI enabled", status.glyphUiEnabled());
        sendStatus(sender, "Text fallback enabled", status.textFallbackEnabled());
    }

    private void sendDialogueHudInfo(CommandSender sender, Player target) {
        DialogueHudResourcePackManager manager = DialogueHudResourcePackManager.getInstance();
        if (manager == null) {
            ChatMessageUtil.send(sender, MessageType.WARNING, "Dialogue HUD integration has not initialized yet.");
            return;
        }
        DialogueHudDebugCommand.sendDebug(sender, manager, target);
    }

    private void startFishingMiniGameTest(CommandSender sender, String type) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, MessageType.ERROR, "Only players can test fishing mini-games.");
            return;
        }
        FishingMiniGameManager manager = FishingMiniGameManager.getInstance();
        if (manager == null) {
            ChatMessageUtil.send(sender, MessageType.WARNING, "Fishing mini-games have not initialized yet.");
            return;
        }
        boolean started = manager.start(player, type, FishingDifficultyProfile.normal(), success -> ChatMessageUtil.send(player,
                success ? MessageType.SUCCESS : MessageType.WARNING,
                "Fishing mini-game test " + (success ? "completed successfully." : "failed.")));
        if (!started) {
            ChatMessageUtil.send(sender, MessageType.WARNING,
                    "Unknown fishing mini-game type. Supported types: " + String.join(", ", FishingMiniGameManager.supportedTypes()) + ".");
        }
    }

    private void sendStatus(CommandSender sender, String label, boolean enabled) {
        ChatMessageUtil.send(sender, MessageType.INFO, ChatColor.GRAY + label + ": "
                + (enabled ? ChatColor.GREEN + "yes" : ChatColor.RED + "no"));
    }

    private void sendUsage(CommandSender sender, String label) {
        ChatMessageUtil.send(sender, MessageType.INFO,
                "Usage: /" + label + " <reload|fishingpack info|dialoguehud info [player]|fishing minigame test <type>>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return List.of(SUB_RELOAD, SUB_FISHING_PACK, SUB_DIALOGUE_HUD, SUB_FISHING).stream()
                    .filter(option -> option.startsWith(input))
                    .toList();
        }
        if (args.length == 2 && (SUB_FISHING_PACK.equalsIgnoreCase(args[0]) || SUB_DIALOGUE_HUD.equalsIgnoreCase(args[0]))) {
            String input = args[1].toLowerCase(Locale.ROOT);
            return SUB_INFO.startsWith(input) ? List.of(SUB_INFO) : Collections.emptyList();
        }
        if (args.length == 3 && SUB_DIALOGUE_HUD.equalsIgnoreCase(args[0]) && SUB_INFO.equalsIgnoreCase(args[1])) {
            return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        if (args.length == 2 && SUB_FISHING.equalsIgnoreCase(args[0])) {
            return matching(args[1], List.of(SUB_MINIGAME));
        }
        if (args.length == 3 && SUB_FISHING.equalsIgnoreCase(args[0]) && SUB_MINIGAME.equalsIgnoreCase(args[1])) {
            return matching(args[2], List.of(SUB_TEST));
        }
        if (args.length == 4 && SUB_FISHING.equalsIgnoreCase(args[0]) && SUB_MINIGAME.equalsIgnoreCase(args[1])
                && SUB_TEST.equalsIgnoreCase(args[2])) {
            return matching(args[3], FishingMiniGameManager.supportedTypes());
        }
        return Collections.emptyList();
    }

    private List<String> matching(String input, List<String> options) {
        String normalizedInput = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(normalizedInput)).toList();
    }
}
