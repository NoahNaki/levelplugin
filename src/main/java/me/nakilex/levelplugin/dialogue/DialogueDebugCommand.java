package me.nakilex.levelplugin.dialogue;

import me.nakilex.levelplugin.dialogue.model.DialogueDefinition;
import me.nakilex.levelplugin.dialogue.resourcepack.DialogueResourcePackManager;
import me.nakilex.levelplugin.dialogue.model.DialoguePage;
import me.nakilex.levelplugin.dialogue.render.ActionBarDialogueRenderer;
import me.nakilex.levelplugin.dialogue.render.DialogueActionBarSender;
import me.nakilex.levelplugin.dialogue.render.DialogueGlyphs;
import me.nakilex.levelplugin.dialogue.render.DialogueOffsetGlyphs;
import me.nakilex.levelplugin.dialogue.render.DialogueRenderContext;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Temporary developer command for manually previewing static Lux-style dialogue pages.
 */
public class DialogueDebugCommand implements TabExecutor {
    private static final long RENDER_PERIOD_TICKS = 1L;
    private static final int RENDER_DURATION_TICKS = 20 * 10;
    private static final List<String> TUNING_KEYS = List.of(
            DialogueRenderContext.TUNE_DIALOGUE_BACKGROUND_OFFSET,
            DialogueRenderContext.TUNE_DIALOGUE_TEXT_OFFSET,
            DialogueRenderContext.TUNE_CHARACTER_OFFSET,
            DialogueRenderContext.TUNE_NAME_BACKGROUND_OFFSET,
            DialogueRenderContext.TUNE_NAME_TEXT_OFFSET,
            DialogueRenderContext.TUNE_INFO_TEXT_OFFSET,
            DialogueRenderContext.TUNE_ARROW_OFFSET,
            DialogueRenderContext.TUNE_ANSWER_BACKGROUND_OFFSET,
            DialogueRenderContext.TUNE_ANSWER_LINE_OFFSET,
            DialogueRenderContext.TUNE_ANSWER_ARROW_OFFSET
    );
    private static final List<String> FONT_TESTS = List.of(
            "offset", "dialogue", "dialogue_background", "answer_background", "character_background", "hand", "fog",
            "name_box", "default", "line1", "line2", "line3", "line4", "line5", "answer1", "answer2",
            "answer3", "character_name", "info", "all"
    );
    private static final List<String> NEXO_DIALOGUE_PACK_FILES = DialogueResourcePackManager.EXPECTED_ASSET_FILES;

    private final JavaPlugin plugin;
    private final DialogueManager dialogueManager;
    private final ActionBarDialogueRenderer renderer;
    private final DialogueActionBarSender actionBarSender;
    private final Map<UUID, BukkitTask> activeRenderTasks = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> tuningByPlayer = new HashMap<>();

    public DialogueDebugCommand(JavaPlugin plugin, DialogueManager dialogueManager) {
        this(plugin, dialogueManager, new DialogueActionBarSender());
    }

    public DialogueDebugCommand(JavaPlugin plugin, DialogueManager dialogueManager, DialogueActionBarSender actionBarSender) {
        this.plugin = plugin;
        this.dialogueManager = dialogueManager;
        this.actionBarSender = actionBarSender == null ? new DialogueActionBarSender() : actionBarSender;
        this.renderer = new ActionBarDialogueRenderer(this.actionBarSender);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "render" -> render(sender, label, args, true);
            case "renderonce" -> render(sender, label, args, false);
            case "inspect" -> inspect(sender, label, args);
            case "fontinspect" -> fontInspect(sender);
            case "fonttest" -> fontTest(sender, label, args);
            case "tune" -> tune(sender, label, args);
            case "stop" -> stop(sender);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        dialogueManager.reload();
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                "Reloaded dialogues (" + dialogueManager.getDialogues().size() + " loaded).");
    }

    private void stop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Only players can stop dialogue previews.");
            return;
        }

        if (cancelRenderTask(player.getUniqueId())) {
            actionBarSender.clear(player);
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Stopped dialogue preview.");
        } else {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "No active dialogue preview.");
        }
    }

    private void render(CommandSender sender, String label, String[] args, boolean repeat) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Only players can render dialogue previews.");
            return;
        }
        if (args.length < 3) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /" + label + " " + args[0].toLowerCase(Locale.ROOT) + " <dialogueId> <pageId>");
            return;
        }

        cancelRenderTask(player.getUniqueId());
        PageLookup lookup = resolvePage(player, args[1], args[2]);
        if (lookup == null) {
            return;
        }

        if (repeat) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Rendering dialogue " + ChatColor.WHITE + lookup.dialogue().id() + ChatColor.GREEN
                            + " page " + ChatColor.WHITE + lookup.page().id() + ChatColor.GREEN + " for 10 seconds.");
            startRenderTask(player, lookup.dialogue(), lookup.page());
        } else {
            renderer.render(player, renderContext(player, lookup.dialogue(), lookup.page()));
        }
    }

    private void inspect(CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /" + label + " inspect <dialogueId> <pageId>");
            return;
        }

        PageLookup lookup = resolvePage(sender, args[1], args[2]);
        if (lookup == null) {
            return;
        }

        DialogueRenderContext context = sender instanceof Player player
                ? renderContext(player, lookup.dialogue(), lookup.page())
                : DialogueRenderContext.of(lookup.dialogue(), lookup.page());
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                ChatColor.YELLOW + "Dialogue HUD Inspect");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Dialogue: " + ChatColor.WHITE + lookup.dialogue().id());
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Page: " + ChatColor.WHITE + lookup.page().id());
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Offsets: " + ChatColor.WHITE
                        + "dialogueBackground=" + context.dialogueBackgroundOffsetPixels()
                        + ", dialogueText=" + context.dialogueTextOffsetPixels()
                        + ", character=" + context.characterOffsetPixels()
                        + ", nameBackground=" + context.nameBackgroundOffsetPixels()
                        + ", nameText=" + context.nameTextOffsetPixels()
                        + ", infoText=" + context.infoTextOffsetPixels()
                        + ", arrow=" + context.arrowOffsetPixels()
                        + ", answerBackground=" + context.answerBackgroundOffsetPixels()
                        + ", answerLine=" + context.answerLineOffsetPixels()
                        + ", answerArrow=" + context.answerArrowOffsetPixels());
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Glyph widths: " + ChatColor.WHITE
                        + "dialogue=" + DialogueGlyphs.DIALOGUE_WIDTH
                        + ", answer=" + DialogueGlyphs.ANSWER_WIDTH
                        + ", character=" + DialogueGlyphs.CHARACTER_WIDTH
                        + ", arrow=" + DialogueGlyphs.ARROW_WIDTH
                        + ", nameStart=" + DialogueGlyphs.NAME_START_WIDTH
                        + ", nameMid=" + DialogueGlyphs.NAME_MID_WIDTH
                        + ", nameEnd=" + DialogueGlyphs.NAME_END_WIDTH
                        + ", fog=" + DialogueGlyphs.FOG_WIDTH);
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Flags: " + ChatColor.WHITE
                        + "fog=" + context.fogEnabled()
                        + ", characterBox=" + context.characterBoxEnabled()
                        + ", nameBox=" + context.nameBoxEnabled());

        String miniMessage = renderer.renderMiniMessage(context);
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "Render MiniMessage length: " + ChatColor.WHITE + miniMessage.length());
        String preview = miniMessage.substring(0, Math.min(300, miniMessage.length()));
        plugin.getLogger().info("Dialogue render preview (first 300 chars): " + preview);
    }

    private void fontTest(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Only players can run dialogue font tests.");
            return;
        }
        if (args.length < 2) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /" + label + " fonttest <" + String.join("|", FONT_TESTS) + ">");
            return;
        }

        String test = args[1].toLowerCase(Locale.ROOT);
        FontTestMessage message = fontTestMessage(test);
        if (message == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Unknown font test '" + args[1] + "'.");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Tests: " + ChatColor.WHITE + String.join(", ", FONT_TESTS));
            return;
        }

        cancelRenderTask(player.getUniqueId());
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, message.description() + " Repeating for 10 seconds.");
        startMiniMessageTask(player, message.miniMessage());
    }

    private void fontInspect(CommandSender sender) {
        Path assetsRoot = dialoguePackAssetsRoot();
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                ChatColor.YELLOW + "Dialogue font pack inspect: " + ChatColor.WHITE + assetsRoot);
        plugin.getLogger().info("Dialogue font pack inspect: " + assetsRoot.toAbsolutePath());
        for (String relativePath : NEXO_DIALOGUE_PACK_FILES) {
            Path path = assetsRoot.resolve(relativePath);
            boolean exists = Files.isRegularFile(path);
            String status = exists ? ChatColor.GREEN + "FOUND" : ChatColor.RED + "MISSING";
            ChatMessageUtil.send(sender, exists ? ChatMessageUtil.MessageType.SUCCESS : ChatMessageUtil.MessageType.ERROR,
                    status + ChatColor.GRAY + " - " + ChatColor.WHITE + relativePath);
            plugin.getLogger().info((exists ? "FOUND" : "MISSING") + " dialogue pack file: " + path.toAbsolutePath());
        }
    }

    private Path dialoguePackAssetsRoot() {
        DialogueResourcePackManager manager = DialogueResourcePackManager.getInstance();
        return manager == null ? DialogueResourcePackManager.defaultAssetsRoot() : manager.assetsRoot();
    }

    private FontTestMessage fontTestMessage(String test) {
        return switch (test) {
            case "offset" -> new FontTestMessage(
                    "A<font:" + DialogueGlyphs.OFFSET_FONT_TAG + ">"
                            + DialogueOffsetGlyphs.POSITIVE_ONE_PIXEL.repeat(5)
                            + "</font>B",
                    "Sent offset font test. A and B should have a tiny gap with no visible boxes.");
            case "dialogue" -> imageFontTestMessage(DialogueGlyphs.DIALOGUE_FONT_TAG,
                    DialogueGlyphs.DIALOGUE_BACKGROUND, "legacy dialogue");
            case "dialogue_background" -> imageFontTestMessage(DialogueGlyphs.DIALOGUE_BACKGROUND_FONT,
                    DialogueGlyphs.DIALOGUE_BACKGROUND, "dialogue background");
            case "answer_background" -> imageFontTestMessage(DialogueGlyphs.ANSWER_BACKGROUND_FONT,
                    DialogueGlyphs.ANSWER_BACKGROUND, "answer background");
            case "character_background" -> imageFontTestMessage(DialogueGlyphs.CHARACTER_BACKGROUND_FONT,
                    DialogueGlyphs.CHARACTER_BACKGROUND, "character background");
            case "hand" -> imageFontTestMessage(DialogueGlyphs.HAND_FONT, DialogueGlyphs.HAND, "hand/arrow");
            case "fog" -> imageFontTestMessage(DialogueGlyphs.FOG_FONT, DialogueGlyphs.FOG, "fog");
            case "name_box" -> imageFontTestMessage(DialogueGlyphs.NAME_BOX_FONT,
                    DialogueGlyphs.NAME_START + DialogueGlyphs.NAME_MID.repeat(16) + DialogueGlyphs.NAME_END,
                    "name box");
            case "default" -> new FontTestMessage(
                    "<font:" + DialogueGlyphs.DEFAULT_TEXT_FONT + ">Hello default font</font>",
                    "Sent default dialogue text font test. Text should be readable with no boxes.");
            case "line1" -> lineFontTestMessage(1, "Hello line one");
            case "line2" -> lineFontTestMessage(2, "Hello line two");
            case "line3" -> lineFontTestMessage(3, "Hello line three");
            case "line4" -> lineFontTestMessage(4, "Hello line four");
            case "line5" -> lineFontTestMessage(5, "Hello line five");
            case "answer1" -> answerFontTestMessage(1, "Hello answer one");
            case "answer2" -> answerFontTestMessage(2, "Hello answer two");
            case "answer3" -> answerFontTestMessage(3, "Hello answer three");
            case "character_name" -> new FontTestMessage(
                    "<font:" + DialogueGlyphs.CHARACTER_NAME_FONT + ">Noah</font>",
                    "Sent character-name font test. Text should sit on the Lux name baseline with no boxes.");
            case "info" -> new FontTestMessage(
                    "<font:" + DialogueGlyphs.INFO_FONT + ">Press shift to continue</font>",
                    "Sent info-line font test. Text should sit on the Lux info baseline with no boxes.");
            case "all" -> new FontTestMessage(
                    "<font:" + DialogueGlyphs.OFFSET_FONT_TAG + ">"
                            + DialogueOffsetGlyphs.POSITIVE_ONE_PIXEL.repeat(8)
                            + "</font>"
                            + "<font:" + DialogueGlyphs.DEFAULT_TEXT_FONT + ">offset test</font> "
                            + "<font:" + DialogueGlyphs.DIALOGUE_BACKGROUND_FONT + ">"
                            + DialogueGlyphs.DIALOGUE_BACKGROUND
                            + "</font>"
                            + "<font:" + DialogueGlyphs.CHARACTER_BACKGROUND_FONT + ">"
                            + DialogueGlyphs.CHARACTER_BACKGROUND
                            + "</font>"
                            + "<font:" + DialogueGlyphs.HAND_FONT + ">"
                            + DialogueGlyphs.HAND
                            + "</font>"
                            + "<font:" + DialogueGlyphs.LINE_FONT_PREFIX + "1>line_1 font test</font> "
                            + "<font:" + DialogueGlyphs.LINE_FONT_PREFIX + "2>line_2 font test</font> "
                            + "<font:" + DialogueGlyphs.ANSWER_FONT_PREFIX + "1>answer_1 font test</font>",
                    "Sent combined dialogue font test. If offset glyphs are visible boxes, fix the resource pack fonts first.");
            default -> null;
        };
    }

    private FontTestMessage lineFontTestMessage(int lineNumber, String text) {
        return new FontTestMessage(
                "<font:" + DialogueGlyphs.LINE_FONT_PREFIX + lineNumber + ">" + text + "</font>",
                "Sent line " + lineNumber + " dialogue font test. Text should be readable with no boxes.");
    }


    private FontTestMessage answerFontTestMessage(int answerNumber, String text) {
        return new FontTestMessage(
                "<font:" + DialogueGlyphs.ANSWER_FONT_PREFIX + answerNumber + ">" + text + "</font>",
                "Sent answer " + answerNumber + " dialogue font test. Text should be readable with no boxes.");
    }

    private FontTestMessage imageFontTestMessage(String font, String glyph, String label) {
        return new FontTestMessage(
                "<font:" + font + ">" + glyph + "</font>",
                "Sent " + label + " image font test. The glyph should appear with no placeholder boxes.");
    }

    private void tune(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Only players can tune dialogue preview offsets.");
            return;
        }
        if (args.length < 3) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /" + label + " tune <key> <value>");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Keys: " + ChatColor.WHITE + String.join(", ", TUNING_KEYS));
            return;
        }

        String key = findTuningKey(args[1]);
        if (key == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Unknown tuning key '" + args[1] + "'.");
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "Keys: " + ChatColor.WHITE + String.join(", ", TUNING_KEYS));
            return;
        }

        int value;
        try {
            value = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Tuning value must be a whole number.");
            return;
        }

        tuningByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> new HashMap<>()).put(key, value);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Set " + ChatColor.WHITE + key + ChatColor.GREEN + " to " + ChatColor.WHITE + value + ChatColor.GREEN + ".");
    }

    private DialogueRenderContext renderContext(Player player, DialogueDefinition dialogue, DialoguePage page) {
        return DialogueRenderContext.of(dialogue, page)
                .withTuning(tuningByPlayer.get(player.getUniqueId()));
    }

    private void startRenderTask(Player player, DialogueDefinition dialogue, DialoguePage page) {
        UUID playerId = player.getUniqueId();
        DialogueRenderContext context = renderContext(player, dialogue, page);
        String miniMessage = renderer.renderMiniMessage(context);
        BukkitTask task = new BukkitRunnable() {
            private int ticksRemaining = RENDER_DURATION_TICKS;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelActiveTask(playerId, this, false);
                    return;
                }

                actionBarSender.sendMiniMessage(player, miniMessage);
                ticksRemaining -= RENDER_PERIOD_TICKS;
                if (ticksRemaining <= 0) {
                    cancelActiveTask(playerId, this, true);
                }
            }
        }.runTaskTimer(plugin, 0L, RENDER_PERIOD_TICKS);
        activeRenderTasks.put(playerId, task);
    }

    private void startMiniMessageTask(Player player, String miniMessage) {
        UUID playerId = player.getUniqueId();
        BukkitTask task = new BukkitRunnable() {
            private int ticksRemaining = RENDER_DURATION_TICKS;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelActiveTask(playerId, this, false);
                    return;
                }

                actionBarSender.sendMiniMessage(player, miniMessage);
                ticksRemaining -= RENDER_PERIOD_TICKS;
                if (ticksRemaining <= 0) {
                    cancelActiveTask(playerId, this, true);
                }
            }
        }.runTaskTimer(plugin, 0L, RENDER_PERIOD_TICKS);
        activeRenderTasks.put(playerId, task);
    }

    private boolean cancelRenderTask(UUID playerId) {
        BukkitTask task = activeRenderTasks.remove(playerId);
        if (task == null) {
            return false;
        }
        task.cancel();
        return true;
    }

    private void cancelActiveTask(UUID playerId, BukkitRunnable runnable, boolean clearActionBar) {
        runnable.cancel();
        activeRenderTasks.remove(playerId);
        if (clearActionBar) {
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                actionBarSender.clear(player);
            }
        }
    }

    private PageLookup resolvePage(CommandSender sender, String dialogueId, String pageId) {
        DialogueDefinition dialogue = dialogueManager.getDialogue(dialogueId);
        if (dialogue == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Dialogue '" + dialogueId + "' does not exist.");
            return null;
        }

        DialoguePage page = dialogue.pages().get(pageId);
        if (page == null) {
            page = findPageIgnoreCase(dialogue, pageId);
        }
        if (page == null) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Page '" + pageId + "' does not exist in dialogue '" + dialogue.id() + "'.");
            return null;
        }
        return new PageLookup(dialogue, page);
    }

    private DialoguePage findPageIgnoreCase(DialogueDefinition dialogue, String pageId) {
        for (DialoguePage page : dialogue.pages().values()) {
            if (page.id().equalsIgnoreCase(pageId)) {
                return page;
            }
        }
        return null;
    }

    private void sendUsage(CommandSender sender, String label) {
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " reload" + ChatColor.GRAY + " - Reload Lux-style dialogue YAML.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " inspect <dialogueId> <pageId>" + ChatColor.GRAY
                        + " - Print dialogue HUD diagnostics.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " fontinspect" + ChatColor.GRAY
                        + " - Check Nexo dialogue font pack files.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " fonttest <" + String.join("|", FONT_TESTS) + ">" + ChatColor.GRAY
                        + " - Repeat isolated dialogue font diagnostics for 10 seconds.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " render <dialogueId> <pageId>" + ChatColor.GRAY
                        + " - Preview a static dialogue page for 10 seconds.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " renderonce <dialogueId> <pageId>" + ChatColor.GRAY
                        + " - Send a static dialogue page once.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " tune <key> <value>" + ChatColor.GRAY
                        + " - Tune your preview offsets in memory.");
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.INFO,
                "/" + label + " stop" + ChatColor.GRAY + " - Stop your active dialogue preview.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matching(List.of("render", "renderonce", "inspect", "fontinspect", "fonttest", "reload", "stop", "tune"), args[0]);
        }
        if (args.length == 2 && "tune".equalsIgnoreCase(args[0])) {
            return matching(TUNING_KEYS, args[1]);
        }
        if (args.length == 2 && "fonttest".equalsIgnoreCase(args[0])) {
            return matching(FONT_TESTS, args[1]);
        }
        if (args.length == 2 && usesDialoguePage(args[0])) {
            return matching(dialogueManager.getDialogues().stream().map(DialogueDefinition::id).toList(), args[1]);
        }
        if (args.length == 3 && usesDialoguePage(args[0])) {
            DialogueDefinition dialogue = dialogueManager.getDialogue(args[1]);
            if (dialogue == null) {
                return List.of();
            }
            return matching(dialogue.pages().keySet().stream().toList(), args[2]);
        }
        return List.of();
    }

    private String findTuningKey(String input) {
        for (String key : TUNING_KEYS) {
            if (key.equalsIgnoreCase(input)) {
                return key;
            }
        }
        return null;
    }

    private boolean usesDialoguePage(String subcommand) {
        return "render".equalsIgnoreCase(subcommand)
                || "renderonce".equalsIgnoreCase(subcommand)
                || "inspect".equalsIgnoreCase(subcommand);
    }

    private List<String> matching(List<String> values, String input) {
        String prefix = input == null ? "" : input.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(value);
            }
        }
        return matches;
    }

    private record PageLookup(DialogueDefinition dialogue, DialoguePage page) {
    }

    private record FontTestMessage(String miniMessage, String description) {
    }
}
