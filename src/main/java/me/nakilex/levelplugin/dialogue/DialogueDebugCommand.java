package me.nakilex.levelplugin.dialogue;

import me.nakilex.levelplugin.dialogue.model.DialogueDefinition;
import me.nakilex.levelplugin.dialogue.model.DialoguePage;
import me.nakilex.levelplugin.dialogue.render.ActionBarDialogueRenderer;
import me.nakilex.levelplugin.dialogue.render.DialogueRenderContext;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Temporary developer command for manually previewing static Lux-style dialogue pages.
 */
public class DialogueDebugCommand implements TabExecutor {
    private final DialogueManager dialogueManager;
    private final ActionBarDialogueRenderer renderer;

    public DialogueDebugCommand(DialogueManager dialogueManager) {
        this(dialogueManager, new ActionBarDialogueRenderer());
    }

    public DialogueDebugCommand(DialogueManager dialogueManager, ActionBarDialogueRenderer renderer) {
        this.dialogueManager = dialogueManager;
        this.renderer = renderer == null ? new ActionBarDialogueRenderer() : renderer;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> reload(sender);
            case "render" -> render(sender, label, args);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private void reload(CommandSender sender) {
        dialogueManager.reload();
        ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.SUCCESS,
                "Reloaded dialogues (" + dialogueManager.getDialogues().size() + " loaded).");
    }

    private void render(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatMessageUtil.send(sender, ChatMessageUtil.MessageType.ERROR,
                    "Only players can render dialogue previews.");
            return;
        }
        if (args.length < 3) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Usage: /" + label + " render <dialogueId> <pageId>");
            return;
        }

        String dialogueId = args[1];
        String pageId = args[2];
        DialogueDefinition dialogue = dialogueManager.getDialogue(dialogueId);
        if (dialogue == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Dialogue '" + dialogueId + "' does not exist.");
            return;
        }

        DialoguePage page = dialogue.pages().get(pageId);
        if (page == null) {
            page = findPageIgnoreCase(dialogue, pageId);
        }
        if (page == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Page '" + pageId + "' does not exist in dialogue '" + dialogue.id() + "'.");
            return;
        }

        renderer.render(player, DialogueRenderContext.of(dialogue, page));
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Rendered dialogue " + ChatColor.WHITE + dialogue.id() + ChatColor.GREEN
                        + " page " + ChatColor.WHITE + page.id() + ChatColor.GREEN + ".");
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
                "/" + label + " render <dialogueId> <pageId>" + ChatColor.GRAY
                        + " - Preview a static dialogue page.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matching(List.of("render", "reload"), args[0]);
        }
        if (args.length == 2 && "render".equalsIgnoreCase(args[0])) {
            return matching(dialogueManager.getDialogues().stream().map(DialogueDefinition::id).toList(), args[1]);
        }
        if (args.length == 3 && "render".equalsIgnoreCase(args[0])) {
            DialogueDefinition dialogue = dialogueManager.getDialogue(args[1]);
            if (dialogue == null) {
                return List.of();
            }
            return matching(dialogue.pages().keySet().stream().toList(), args[2]);
        }
        return List.of();
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
}
