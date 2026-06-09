package me.nakilex.levelplugin.luxdialogues;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Reflection based bridge into LuxDialogues' public API.
 *
 * This avoids a compile-time LuxDialogues dependency. LevelPlugin can still build
 * without the LuxDialogues jar in Maven, while using it at runtime when installed.
 */
public final class LuxDialoguesBridge {
    private LuxDialoguesBridge() {
    }

    public static boolean isPluginEnabled() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("LuxDialogues");
        return plugin != null && plugin.isEnabled();
    }

    public static boolean isInDialogue(Player player) throws ReflectiveOperationException {
        Object provider = getProvider();
        Method isInDialogue = provider.getClass().getMethod("isInDialogue", Player.class);
        Object result = invokeProvider(isInDialogue, provider, player);
        return Boolean.TRUE.equals(result);
    }

    public static void clearDialogue(Player player) throws ReflectiveOperationException {
        Object provider = getProvider();
        Method clearDialogue = provider.getClass().getMethod("clearDialogue", Player.class);
        invokeProvider(clearDialogue, provider, player);
    }

    public static void sendTestDialogue(Player player) throws ReflectiveOperationException {
        Object provider = getProvider();

        Class<?> dialogueClass = Class.forName("org.aselstudios.luxdialoguesapi.Builders.Dialogue");
        Class<?> dialogueBuilderClass = Class.forName("org.aselstudios.luxdialoguesapi.Builders.Dialogue$Builder");
        Class<?> pageClass = Class.forName("org.aselstudios.luxdialoguesapi.Builders.Page");
        Class<?> pageBuilderClass = Class.forName("org.aselstudios.luxdialoguesapi.Builders.Page$Builder");
        Class<?> answerClass = Class.forName("org.aselstudios.luxdialoguesapi.Builders.Answer");
        Class<?> answerBuilderClass = Class.forName("org.aselstudios.luxdialoguesapi.Builders.Answer$Builder");

        Object apiAnswer = answerBuilderClass.getConstructor().newInstance();
        invoke(answerBuilderClass, apiAnswer, "setAnswerID", new Class<?>[]{String.class}, "api_question");
        invoke(answerBuilderClass, apiAnswer, "setAnswerText", new Class<?>[]{String.class}, "What API is this using?");
        invoke(answerBuilderClass, apiAnswer, "setGoTo", new Class<?>[]{java.util.List.class}, Arrays.asList("api_answer"));
        Object askAboutApi = invoke(answerBuilderClass, apiAnswer, "build", new Class<?>[]{});

        Object leaveAnswerBuilder = answerBuilderClass.getConstructor().newInstance();
        invoke(answerBuilderClass, leaveAnswerBuilder, "setAnswerID", new Class<?>[]{String.class}, "leave");
        invoke(answerBuilderClass, leaveAnswerBuilder, "setAnswerText", new Class<?>[]{String.class}, "End dialogue");
        invoke(answerBuilderClass, leaveAnswerBuilder, "setGoTo", new Class<?>[]{java.util.List.class}, Arrays.asList("end"));
        Object leaveAnswer = invoke(answerBuilderClass, leaveAnswerBuilder, "build", new Class<?>[]{});

        Object startPageBuilder = pageBuilderClass.getConstructor().newInstance();
        invoke(pageBuilderClass, startPageBuilder, "setID", new Class<?>[]{String.class}, "start");
        invoke(pageBuilderClass, startPageBuilder, "addLine", new Class<?>[]{String.class}, "This dialogue was created inside LevelPlugin.");
        invoke(pageBuilderClass, startPageBuilder, "addLine", new Class<?>[]{String.class}, "If you can see this, the LuxDialogues API call worked.");
        invoke(pageBuilderClass, startPageBuilder, "addAnswer", new Class<?>[]{answerClass}, askAboutApi);
        invoke(pageBuilderClass, startPageBuilder, "addAnswer", new Class<?>[]{answerClass}, leaveAnswer);
        Object startPage = invoke(pageBuilderClass, startPageBuilder, "build", new Class<?>[]{});

        Object backAnswerBuilder = answerBuilderClass.getConstructor().newInstance();
        invoke(answerBuilderClass, backAnswerBuilder, "setAnswerID", new Class<?>[]{String.class}, "back");
        invoke(answerBuilderClass, backAnswerBuilder, "setAnswerText", new Class<?>[]{String.class}, "Back");
        invoke(answerBuilderClass, backAnswerBuilder, "setGoTo", new Class<?>[]{java.util.List.class}, Arrays.asList("start"));
        Object backAnswer = invoke(answerBuilderClass, backAnswerBuilder, "build", new Class<?>[]{});

        Object apiPageBuilder = pageBuilderClass.getConstructor().newInstance();
        invoke(pageBuilderClass, apiPageBuilder, "setID", new Class<?>[]{String.class}, "api_answer");
        invoke(pageBuilderClass, apiPageBuilder, "addLine", new Class<?>[]{String.class}, "LevelPlugin is calling org.aselstudios.luxdialoguesapi at runtime.");
        invoke(pageBuilderClass, apiPageBuilder, "addLine", new Class<?>[]{String.class}, "No source copy is needed for this test.");
        invoke(pageBuilderClass, apiPageBuilder, "addAnswer", new Class<?>[]{answerClass}, backAnswer);
        invoke(pageBuilderClass, apiPageBuilder, "addAnswer", new Class<?>[]{answerClass}, leaveAnswer);
        Object apiPage = invoke(pageBuilderClass, apiPageBuilder, "build", new Class<?>[]{});

        Object endPageBuilder = pageBuilderClass.getConstructor().newInstance();
        invoke(pageBuilderClass, endPageBuilder, "setID", new Class<?>[]{String.class}, "end");
        invoke(pageBuilderClass, endPageBuilder, "setTimer", new Class<?>[]{Integer.class}, 30);
        invoke(pageBuilderClass, endPageBuilder, "addLine", new Class<?>[]{String.class}, "Dialogue test finished.");
        Object endPage = invoke(pageBuilderClass, endPageBuilder, "build", new Class<?>[]{});

        Object dialogueBuilder = dialogueBuilderClass.getConstructor().newInstance();
        invoke(dialogueBuilderClass, dialogueBuilder, "setDialogueID", new Class<?>[]{String.class}, "levelplugin_api_test");
        invoke(dialogueBuilderClass, dialogueBuilder, "setEffect", new Class<?>[]{String.class}, "Slowness");
        invoke(dialogueBuilderClass, dialogueBuilder, "setRange", new Class<?>[]{Double.class}, 3.0D);
        invoke(dialogueBuilderClass, dialogueBuilder, "setPreventExit", new Class<?>[]{Boolean.class}, false);
        invoke(dialogueBuilderClass, dialogueBuilder, "setPreventSkip", new Class<?>[]{Boolean.class}, false);
        invoke(dialogueBuilderClass, dialogueBuilder, "setCharacterNameText", new Class<?>[]{String.class, String.class, Integer.class}, "LevelPlugin", "#4f4a3e", 0);
        invoke(dialogueBuilderClass, dialogueBuilder, "setCharacterImage", new Class<?>[]{String.class, String.class, Integer.class}, "character-background", "#ffffff", -16);
        invoke(dialogueBuilderClass, dialogueBuilder, "setArrowImage", new Class<?>[]{String.class, String.class, Integer.class}, "hand", "#cdff29", -7);
        invoke(dialogueBuilderClass, dialogueBuilder, "setDialogueBackgroundImage", new Class<?>[]{String.class, String.class, Integer.class}, "dialogue-background", "#f8ffe0", 0);
        invoke(dialogueBuilderClass, dialogueBuilder, "setAnswerBackgroundImage", new Class<?>[]{String.class, String.class, Integer.class}, "answer-background", "#f8ffe0", 140);
        invoke(dialogueBuilderClass, dialogueBuilder, "setNameImage", new Class<?>[]{String.class, String.class, String.class, String.class, Integer.class}, "name-start", "name-mid", "name-end", "#f8ffe0", 20);
        invoke(dialogueBuilderClass, dialogueBuilder, "setFogImage", new Class<?>[]{String.class, String.class}, "fog", "#000000");
        invoke(dialogueBuilderClass, dialogueBuilder, "setDialogueText", new Class<?>[]{String.class, Integer.class}, "#4f4a3e", 10);
        invoke(dialogueBuilderClass, dialogueBuilder, "setAnswerText", new Class<?>[]{String.class, Integer.class, String.class}, "#4f4a3e", 13, "#4f4a3e");
        invoke(dialogueBuilderClass, dialogueBuilder, "setAnswerNumbers", new Class<?>[]{Boolean.class}, true);
        invoke(dialogueBuilderClass, dialogueBuilder, "setDialogueSpeed", new Class<?>[]{Integer.class}, 1);
        invoke(dialogueBuilderClass, dialogueBuilder, "setTypingSound", new Class<?>[]{String.class, String.class, Double.class, Double.class}, "luxdialogues:luxdialogues.sounds.typing", "MASTER", 1.0D, 1.0D);
        invoke(dialogueBuilderClass, dialogueBuilder, "setSelectionSound", new Class<?>[]{String.class, String.class, Double.class, Double.class}, "luxdialogues:luxdialogues.sounds.selection", "MASTER", 1.0D, 1.0D);
        invoke(dialogueBuilderClass, dialogueBuilder, "addPage", new Class<?>[]{pageClass}, startPage);
        invoke(dialogueBuilderClass, dialogueBuilder, "addPage", new Class<?>[]{pageClass}, apiPage);
        invoke(dialogueBuilderClass, dialogueBuilder, "addPage", new Class<?>[]{pageClass}, endPage);
        Object dialogue = invoke(dialogueBuilderClass, dialogueBuilder, "build", new Class<?>[]{});

        Method sendDialogue = provider.getClass().getMethod("sendDialogue", Player.class, dialogueClass, String.class);
        invokeProvider(sendDialogue, provider, player, dialogue, "start");
    }

    private static Object getProvider() throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName("org.aselstudios.luxdialoguesapi.LuxDialoguesAPI");
        Method getProvider = apiClass.getMethod("getProvider");
        Object provider = getProvider.invoke(null);
        if (provider == null) {
            throw new IllegalStateException("LuxDialoguesAPI.getProvider() returned null. Is LuxDialogues fully enabled?");
        }
        return provider;
    }

    private static Object invoke(Class<?> owner, Object target, String name, Class<?>[] parameterTypes, Object... args)
            throws ReflectiveOperationException {
        Method method = owner.getMethod(name, parameterTypes);
        return method.invoke(target, args);
    }

    private static Object invokeProvider(Method method, Object target, Object... args) throws ReflectiveOperationException {
        try {
            return method.invoke(target, args);
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
}
