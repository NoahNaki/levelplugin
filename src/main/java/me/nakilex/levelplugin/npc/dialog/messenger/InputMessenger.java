package me.nakilex.levelplugin.npc.dialog.messenger;

import me.nakilex.levelplugin.npc.dialog.entry.InputDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/** Messenger that captures and validates one chat message as dialogue input. */
public final class InputMessenger extends DialogueMessenger implements Listener {
    private final InputDialogueEntry inputEntry;

    public InputMessenger(Player player, InputDialogueEntry entry, InteractionContext context) {
        super(player, entry, context);
        this.inputEntry = entry;
    }

    @Override
    public void init() {
        super.init();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        player.sendMessage(ChatColor.AQUA + inputEntry.prompt());
    }

    @Override
    public void dispose() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!event.getPlayer().equals(player) || state() != State.RUNNING) return;
        event.setCancelled(true);
        String input = event.getMessage() == null ? "" : event.getMessage().trim();
        Bukkit.getScheduler().runTask(context.plugin(), () -> {
            if (!inputEntry.isValid(input)) {
                player.sendMessage(ChatColor.RED + "That response is not valid. Try again.");
                return;
            }
            context.set(inputEntry.resultKey(), input);
            dispose();
            finish();
        });
    }
}
