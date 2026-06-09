package me.nakilex.levelplugin.luxdialogues;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Collections;

/**
 * Small smoke-test command for calling LuxDialogues' public API from LevelPlugin.
 *
 * This intentionally uses reflection so LevelPlugin can still compile when the
 * LuxDialogues jar is not on the Maven compile classpath. At runtime, the server
 * must have LuxDialogues installed and enabled.
 */
public final class LuxDialoguesApiTestCommand implements CommandExecutor {

    private static final String API_CLASS = "org.aselstudios.luxdialoguesapi.LuxDialoguesAPI";
    private static final String DIALOGUE_BUILDER_CLASS = "org.aselstudios.luxdialoguesapi.Builders.Dialogue$Builder";
    private static final String PAGE_BUILDER_CLASS = "org.aselstudios.luxdialoguesapi.Builders.Page$Builder";
    private static final String ANSWER_BUILDER_CLASS = "org.aselstudios.luxdialoguesapi.Builders.Answer$Builder";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can run this command.");
            return true;
        }

        Plugin luxDialogues = Bukkit.getPluginManager().getPlugin("LuxDialogues");
        if (luxDialogues == null || !luxDialogues.isEnabled()) {
            player.sendMessage(ChatColor.RED + "LuxDialogues is not installed/enabled, so the API test cannot run.");
            return true;
        }

        try {
            Object provider = getProvider();
            if (provider == null) {
                player.sendMessage(ChatColor.RED + "LuxDialogues API provider is null. Check LuxDialogues startup logs.");
                return true;
            }

            Object dialogue = buildTestDialogue(player);
            Class<?> providerInterface = getClassByName("org.aselstudios.luxdialoguesapi.DialogueProvider");
            Method sendDialogue = providerInterface.getMethod("sendDialogue", Player.class, getClassByName("org.aselstudios.luxdialoguesapi.Builders.Dialogue"), String.class);
            sendDialogue.invoke(provider, player, dialogue, "start");

            player.sendMessage(ChatColor.GREEN + "Sent LuxDialogues API test dialogue.");
            return true;
        } catch (ReflectiveOperationException ex) {
            player.sendMessage(ChatColor.RED + "Failed to call LuxDialogues API. Check console for details.");
            Bukkit.getLogger().severe("[LevelPlugin] LuxDialogues API test failed: " + ex.getMessage());
            ex.printStackTrace();
            return true;
        }
    }

    private Object getProvider() throws ReflectiveOperationException {
        Class<?> apiClass = getClassByName(API_CLASS);
        Method getProvider = apiClass.getMethod("getProvider");
        return getProvider.invoke(null);
    }

    private Object buildTestDialogue(Player player) throws ReflectiveOperationException {
        Object firstAnswer = newBuilder(ANSWER_BUILDER_CLASS)
                .call("setAnswerID", new Class[]{String.class}, "who_are_you")
                .call("setAnswerText", new Class[]{String.class}, "Who are you?")
                .call("setGoTo", new Class[]{java.util.List.class}, Collections.singletonList("who"))
                .build();

        Object secondAnswer = newBuilder(ANSWER_BUILDER_CLASS)
                .call("setAnswerID", new Class[]{String.class}, "goodbye")
                .call("setAnswerText", new Class[]{String.class}, "Goodbye.")
                .call("setGoTo", new Class[]{java.util.List.class}, Collections.singletonList("end"))
                .build();

        Object startPage = newBuilder(PAGE_BUILDER_CLASS)
                .call("setID", new Class[]{String.class}, "start")
                .call("addLine", new Class[]{String.class}, "Hey " + player.getName() + ".")
                .call("addLine", new Class[]{String.class}, "This dialogue was created inside LevelPlugin and sent through the LuxDialogues API.")
                .call("addAnswer", new Class[]{getClassByName("org.aselstudios.luxdialoguesapi.Builders.Answer")}, firstAnswer)
                .call("addAnswer", new Class[]{getClassByName("org.aselstudios.luxdialoguesapi.Builders.Answer")}, secondAnswer)
                .build();

        Object whoPage = newBuilder(PAGE_BUILDER_CLASS)
                .call("setID", new Class[]{String.class}, "who")
                .call("addLine", new Class[]{String.class}, "I am a LevelPlugin test NPC.")
                .call("addLine", new Class[]{String.class}, "If you can see this, the LuxDialogues API bridge works.")
                .call("setGoTo", new Class[]{java.util.List.class}, Collections.singletonList("end"))
                .build();

        Object endPage = newBuilder(PAGE_BUILDER_CLASS)
                .call("setID", new Class[]{String.class}, "end")
                .call("addLine", new Class[]{String.class}, "API test finished.")
                .build();

        return newBuilder(DIALOGUE_BUILDER_CLASS)
                .call("setDialogueID", new Class[]{String.class}, "levelplugin_api_test")
                .call("setCharacterNameText", new Class[]{String.class, String.class, Integer.class}, "LevelPlugin", "#FFFFFF", 0)
                .call("setDialogueText", new Class[]{String.class, Integer.class}, "#FFFFFF", 0)
                .call("setAnswerText", new Class[]{String.class, Integer.class, String.class}, "#FFFFFF", 0, "#FFFF55")
                .call("setDialogueSpeed", new Class[]{Integer.class}, 2)
                .call("addPage", new Class[]{getClassByName("org.aselstudios.luxdialoguesapi.Builders.Page")}, startPage)
                .call("addPage", new Class[]{getClassByName("org.aselstudios.luxdialoguesapi.Builders.Page")}, whoPage)
                .call("addPage", new Class[]{getClassByName("org.aselstudios.luxdialoguesapi.Builders.Page")}, endPage)
                .build();
    }

    private ReflectiveBuilder newBuilder(String className) throws ReflectiveOperationException {
        return new ReflectiveBuilder(getClassByName(className).getDeclaredConstructor().newInstance());
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

    private static final class ReflectiveBuilder {
        private Object target;

        private ReflectiveBuilder(Object target) {
            this.target = target;
        }

        private ReflectiveBuilder call(String methodName, Class<?>[] parameterTypes, Object... args) throws ReflectiveOperationException {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            Object result = method.invoke(target, args);
            if (result != null) {
                target = result;
            }
            return this;
        }

        private Object build() throws ReflectiveOperationException {
            return target.getClass().getMethod("build").invoke(target);
        }
    }
}
