package me.nakilex.levelplugin.luxdialogues;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.NpcSoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Reusable LevelPlugin -> LuxDialogues adapter for NPC dialogue.
 *
 * Keep this class generic. NPC/quest/storage code should provide:
 * - a dialogue id
 * - legacy LevelPlugin lines, optionally formatted as "Speaker|Text"
 * - optional answer choices with callbacks
 *
 * The class intentionally uses reflection so LevelPlugin can still compile without
 * the LuxDialogues API jar on the Maven classpath. At runtime LuxDialogues must be
 * installed and enabled.
 */
public final class LuxNpcDialogueService {
    private static final int DEFAULT_WRAP_CHARS = 32;
    private static final int MAX_LINES_PER_PAGE = 4;

    private static final String DIALOGUE_CLASS = "org.aselstudios.luxdialoguesapi.Builders.Dialogue";
    private static final String DIALOGUE_BUILDER_CLASS = "org.aselstudios.luxdialoguesapi.Builders.Dialogue$Builder";
    private static final String PAGE_CLASS = "org.aselstudios.luxdialoguesapi.Builders.Page";
    private static final String PAGE_BUILDER_CLASS = "org.aselstudios.luxdialoguesapi.Builders.Page$Builder";
    private static final String ANSWER_CLASS = "org.aselstudios.luxdialoguesapi.Builders.Answer";
    private static final String ANSWER_BUILDER_CLASS = "org.aselstudios.luxdialoguesapi.Builders.Answer$Builder";
    private static final String CALLBACK_CLASS = "org.aselstudios.luxdialoguesapi.Builders.DialogueCallback";

    private final Main plugin;

    public LuxNpcDialogueService(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        return LuxDialoguesBridge.isPluginEnabled();
    }

    public boolean sendLinear(Player player, String dialogueId, String fallbackSpeaker, List<String> legacyLines, Runnable finish) {
        return send(player, dialogueId, fallbackSpeaker, legacyLines, List.of(), finish);
    }

    public boolean sendChoice(Player player, String dialogueId, String fallbackSpeaker, List<String> legacyLines,
                              List<LuxNpcDialogueChoice> choices) {
        return send(player, dialogueId, fallbackSpeaker, legacyLines, choices, null);
    }

    public boolean send(Player player, String dialogueId, String fallbackSpeaker, List<String> legacyLines,
                        List<LuxNpcDialogueChoice> choices, Runnable finish) {
        if (!isAvailable()) {
            return false;
        }
        if (player == null || legacyLines == null || legacyLines.isEmpty()) {
            return false;
        }

        try {
            Object dialogue = buildDialogue(player, dialogueId, fallbackSpeaker, legacyLines, choices, finish);
            Object provider = getProvider();
            Method sendDialogue = provider.getClass().getMethod("sendDialogue", Player.class, getClassByName(DIALOGUE_CLASS), String.class);
            invokeProvider(sendDialogue, provider, player, dialogue, "start");
            return true;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[LuxDialogues] Failed to send NPC dialogue '" + dialogueId + "': " + throwable.getMessage());
            throwable.printStackTrace();
            return false;
        }
    }

    private Object buildDialogue(Player player, String dialogueId, String fallbackSpeaker, List<String> legacyLines,
                                 List<LuxNpcDialogueChoice> choices, Runnable finish) throws ReflectiveOperationException {
        String speaker = resolveSpeaker(fallbackSpeaker, legacyLines);
        List<String> textLines = flattenAndWrap(player, legacyLines, speaker);
        if (textLines.isEmpty()) {
            textLines = List.of("...");
        }

        List<List<String>> pages = paginate(textLines, MAX_LINES_PER_PAGE);

        Object dialogueBuilder = newBuilder(DIALOGUE_BUILDER_CLASS);
        configureBaseDialogue(dialogueBuilder, player, safeDialogueId(dialogueId), speaker);

        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            String pageId = pageId(pageIndex);
            boolean lastPage = pageIndex == pages.size() - 1;

            Object pageBuilder = newBuilder(PAGE_BUILDER_CLASS);
            call(pageBuilder, "setID", new Class<?>[]{String.class}, pageId);

            for (String line : pages.get(pageIndex)) {
                call(pageBuilder, "addLine", new Class<?>[]{String.class}, line);
            }

            if (!lastPage) {
                call(pageBuilder, "addAnswer", new Class<?>[]{getClassByName(ANSWER_CLASS)}, buildGotoAnswer(
                        "continue_" + pageIndex,
                        "Continue",
                        pageId(pageIndex + 1)
                ));
            } else if (choices != null && !choices.isEmpty()) {
                for (LuxNpcDialogueChoice choice : choices) {
                    call(pageBuilder, "addAnswer", new Class<?>[]{getClassByName(ANSWER_CLASS)}, buildChoiceAnswer(choice));
                }
            } else if (finish != null) {
                call(pageBuilder, "addPostCallback", new Class<?>[]{getClassByName(CALLBACK_CLASS)}, createCallback(finish));
            }

            call(dialogueBuilder, "addPage", new Class<?>[]{getClassByName(PAGE_CLASS)}, call(pageBuilder, "build", new Class<?>[]{}));
        }

        call(dialogueBuilder, "addPage", new Class<?>[]{getClassByName(PAGE_CLASS)}, buildEndPage());
        return call(dialogueBuilder, "build", new Class<?>[]{});
    }

    private Object buildChoiceAnswer(LuxNpcDialogueChoice choice) throws ReflectiveOperationException {
        Object answerBuilder = newBuilder(ANSWER_BUILDER_CLASS);
        call(answerBuilder, "setAnswerID", new Class<?>[]{String.class}, safeAnswerId(choice.id()));
        call(answerBuilder, "setAnswerText", new Class<?>[]{String.class}, choice.text());
        call(answerBuilder, "setGoTo", new Class<?>[]{List.class}, List.of("end"));
        if (choice.callback() != null) {
            call(answerBuilder, "addCallback", new Class<?>[]{getClassByName(CALLBACK_CLASS)}, createCallback(choice.callback()));
        }
        return call(answerBuilder, "build", new Class<?>[]{});
    }

    private Object buildGotoAnswer(String id, String text, String targetPageId) throws ReflectiveOperationException {
        Object answerBuilder = newBuilder(ANSWER_BUILDER_CLASS);
        call(answerBuilder, "setAnswerID", new Class<?>[]{String.class}, safeAnswerId(id));
        call(answerBuilder, "setAnswerText", new Class<?>[]{String.class}, text);
        call(answerBuilder, "setGoTo", new Class<?>[]{List.class}, List.of(targetPageId));
        return call(answerBuilder, "build", new Class<?>[]{});
    }

    private Object buildEndPage() throws ReflectiveOperationException {
        Object pageBuilder = newBuilder(PAGE_BUILDER_CLASS);
        call(pageBuilder, "setID", new Class<?>[]{String.class}, "end");
        call(pageBuilder, "setTimer", new Class<?>[]{Integer.class}, 1);
        call(pageBuilder, "addLine", new Class<?>[]{String.class}, "");
        return call(pageBuilder, "build", new Class<?>[]{});
    }

    private void configureBaseDialogue(Object dialogueBuilder, Player player, String dialogueId, String speaker) throws ReflectiveOperationException {
        call(dialogueBuilder, "setDialogueID", new Class<?>[]{String.class}, dialogueId + "_" + UUID.randomUUID());
        call(dialogueBuilder, "setEffect", new Class<?>[]{String.class}, "Slowness");
        call(dialogueBuilder, "setRange", new Class<?>[]{Double.class}, 3.0D);
        call(dialogueBuilder, "setPreventExit", new Class<?>[]{Boolean.class}, false);
        call(dialogueBuilder, "setPreventSkip", new Class<?>[]{Boolean.class}, false);
        call(dialogueBuilder, "setCharacterNameText", new Class<?>[]{String.class, String.class, Integer.class}, speaker, "#4f4a3e", 0);
        call(dialogueBuilder, "setCharacterImage", new Class<?>[]{String.class, String.class, Integer.class}, "character-background", "#ffffff", -16);
        call(dialogueBuilder, "setArrowImage", new Class<?>[]{String.class, String.class, Integer.class}, "hand", "#cdff29", -7);
        call(dialogueBuilder, "setDialogueBackgroundImage", new Class<?>[]{String.class, String.class, Integer.class}, "dialogue-background", "#f8ffe0", 0);
        call(dialogueBuilder, "setAnswerBackgroundImage", new Class<?>[]{String.class, String.class, Integer.class}, "answer-background", "#f8ffe0", 140);
        call(dialogueBuilder, "setNameImage", new Class<?>[]{String.class, String.class, String.class, String.class, Integer.class}, "name-start", "name-mid", "name-end", "#f8ffe0", 20);
        call(dialogueBuilder, "setFogImage", new Class<?>[]{String.class, String.class}, "fog", "#000000");
        call(dialogueBuilder, "setDialogueText", new Class<?>[]{String.class, Integer.class}, "#4f4a3e", 10);
        call(dialogueBuilder, "setAnswerText", new Class<?>[]{String.class, Integer.class, String.class}, "#4f4a3e", 13, "#4f4a3e");
        call(dialogueBuilder, "setAnswerNumbers", new Class<?>[]{Boolean.class}, true);
        call(dialogueBuilder, "setDialogueSpeed", new Class<?>[]{Integer.class}, 1);
        if (NpcSoundUtil.canHearNpcSound(player)) {
            call(dialogueBuilder, "setTypingSound", new Class<?>[]{String.class, String.class, Double.class, Double.class}, "luxdialogues:luxdialogues.sounds.typing", "MASTER", 1.0D, 1.0D);
            call(dialogueBuilder, "setSelectionSound", new Class<?>[]{String.class, String.class, Double.class, Double.class}, "luxdialogues:luxdialogues.sounds.selection", "MASTER", 1.0D, 1.0D);
        }
    }

    private Object createCallback(Runnable runnable) throws ReflectiveOperationException {
        Class<?> callbackClass = getClassByName(CALLBACK_CLASS);
        InvocationHandler handler = (proxy, method, args) -> {
            if ("execute".equals(method.getName())) {
                Bukkit.getScheduler().runTask(plugin, runnable);
                return null;
            }
            if ("toString".equals(method.getName())) {
                return "LevelPluginLuxDialogueCallback";
            }
            return null;
        };
        return Proxy.newProxyInstance(callbackClass.getClassLoader(), new Class<?>[]{callbackClass}, handler);
    }

    private List<String> flattenAndWrap(Player player, List<String> legacyLines, String fallbackSpeaker) {
        List<String> result = new ArrayList<>();
        for (String legacyLine : legacyLines) {
            ParsedLine parsed = parseLegacyLine(legacyLine, fallbackSpeaker);
            String text = ChatColor.stripColor(parsed.text()).replace("<player>", player.getName());
            for (String wrapped : wrap(text, DEFAULT_WRAP_CHARS)) {
                if (!wrapped.isBlank()) {
                    result.add(wrapped);
                }
            }
        }
        return result;
    }

    private String resolveSpeaker(String fallbackSpeaker, List<String> legacyLines) {
        if (legacyLines != null) {
            for (String line : legacyLines) {
                ParsedLine parsed = parseLegacyLine(line, fallbackSpeaker);
                if (parsed.speaker() != null && !parsed.speaker().isBlank()) {
                    return ChatColor.stripColor(parsed.speaker());
                }
            }
        }
        return fallbackSpeaker == null || fallbackSpeaker.isBlank() ? "NPC" : ChatColor.stripColor(fallbackSpeaker);
    }

    private ParsedLine parseLegacyLine(String line, String fallbackSpeaker) {
        if (line == null) {
            return new ParsedLine(fallbackSpeaker, "");
        }
        int separator = line.indexOf('|');
        if (separator > 0) {
            return new ParsedLine(line.substring(0, separator), line.substring(separator + 1));
        }
        return new ParsedLine(fallbackSpeaker, line);
    }

    private List<String> wrap(String text, int maxChars) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.isEmpty()) {
            return List.of("");
        }

        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String word : cleaned.split("\\s+")) {
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() > maxChars) {
                addWrappedLine(lines, current);
                current.append(word);
            } else {
                current.append(' ').append(word);
            }

            if (endsSentence(word)) {
                addWrappedLine(lines, current);
            }
        }

        addWrappedLine(lines, current);
        return lines;
    }

    private void addWrappedLine(List<String> lines, StringBuilder current) {
        if (current.length() == 0) {
            return;
        }
        String line = current.toString().trim();
        current.setLength(0);
        if (!line.isBlank()) {
            lines.add(line);
        }
    }

    private boolean endsSentence(String word) {
        String cleaned = word == null ? "" : word.replaceAll("[\\\"')\\]}>]+$", "");
        return cleaned.endsWith(".") || cleaned.endsWith("!") || cleaned.endsWith("?");
    }

    private List<List<String>> paginate(List<String> lines, int maxLinesPerPage) {
        List<List<String>> pages = new ArrayList<>();
        List<String> currentPage = new ArrayList<>();
        int safeMaxLines = Math.max(1, maxLinesPerPage);

        for (String line : lines) {
            if (currentPage.size() >= safeMaxLines) {
                pages.add(currentPage);
                currentPage = new ArrayList<>();
            }
            currentPage.add(line);
        }

        if (!currentPage.isEmpty()) {
            pages.add(currentPage);
        }
        if (pages.isEmpty()) {
            pages.add(List.of("..."));
        }
        return pages;
    }

    private String pageId(int pageIndex) {
        return pageIndex == 0 ? "start" : "page_" + pageIndex;
    }

    private String safeDialogueId(String id) {
        if (id == null || id.isBlank()) {
            return "levelplugin_npc_dialogue";
        }
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }

    private String safeAnswerId(String id) {
        if (id == null || id.isBlank()) {
            return "answer";
        }
        return id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]", "_");
    }

    private Object getProvider() throws ReflectiveOperationException {
        Class<?> apiClass = getClassByName("org.aselstudios.luxdialoguesapi.LuxDialoguesAPI");
        Method getProvider = apiClass.getMethod("getProvider");
        Object provider = getProvider.invoke(null);
        if (provider == null) {
            throw new IllegalStateException("LuxDialoguesAPI.getProvider() returned null.");
        }
        return provider;
    }

    private Object newBuilder(String className) throws ReflectiveOperationException {
        return getClassByName(className).getDeclaredConstructor().newInstance();
    }

    private Object call(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(target, args);
    }

    private void invokeProvider(Method method, Object target, Object... args) throws ReflectiveOperationException {
        try {
            method.invoke(target, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    private Class<?> getClassByName(String className) throws ClassNotFoundException {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException ignored) {
            Plugin luxDialogues = Bukkit.getPluginManager().getPlugin("LuxDialogues");
            if (luxDialogues != null) {
                return Class.forName(className, true, luxDialogues.getClass().getClassLoader());
            }
            throw ignored;
        }
    }

    private record ParsedLine(String speaker, String text) {
    }
}
