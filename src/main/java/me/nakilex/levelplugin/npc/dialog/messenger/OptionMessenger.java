package me.nakilex.levelplugin.npc.dialog.messenger;

import java.util.List;
import me.nakilex.levelplugin.npc.dialog.PlaceholderResolver;
import me.nakilex.levelplugin.npc.dialog.entry.OptionDialogueEntry;
import me.nakilex.levelplugin.npc.dialog.model.ContextKeys;
import me.nakilex.levelplugin.npc.dialog.model.DialogueModifier;
import me.nakilex.levelplugin.npc.dialog.model.DialogueTrigger;
import me.nakilex.levelplugin.npc.dialog.model.InteractionContext;
import me.nakilex.levelplugin.utils.ChatFormatter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Messenger that owns scroll-wheel option selection events. Internal option indices are zero-based. */
public final class OptionMessenger extends DialogueMessenger implements Listener {
    private final OptionDialogueEntry optionEntry;
    private List<OptionDialogueEntry.Option> usableOptions = List.of();
    private int selectedIndex;
    private long openedAt;

    public OptionMessenger(Player player, OptionDialogueEntry entry, InteractionContext context) {
        super(player, entry, context);
        this.optionEntry = entry;
    }

    @Override public void init() {
        super.init();
        usableOptions = optionEntry.options().stream().filter(option -> option.matches(context)).toList();
        if (usableOptions.isEmpty()) { finish(); return; }
        openedAt = System.currentTimeMillis();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        sendChoice();
    }

    @Override public void dispose() { HandlerList.unregisterAll(this); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onScroll(PlayerItemHeldEvent event) {
        if (!event.getPlayer().equals(player) || state() != State.RUNNING) return;
        event.setCancelled(true);
        selectedIndex += event.getNewSlot() > event.getPreviousSlot() ? 1 : -1;
        if (selectedIndex < 0) selectedIndex = usableOptions.size() - 1;
        if (selectedIndex >= usableOptions.size()) selectedIndex = 0;
        sendChoice();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getPlayer().equals(player) || state() != State.RUNNING) return;
        if (context.npc() != null && !context.npc().matches(event.getRightClicked())) return;
        event.setCancelled(true);
        requestNextOrSkip();
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { if (event.getPlayer().equals(player)) cancel(); }

    @Override public void requestNextOrSkip() {
        if (state() != State.RUNNING || usableOptions.isEmpty()) return;
        if (System.currentTimeMillis() - openedAt < 400) { sendChoice(); return; }
        OptionDialogueEntry.Option selected = usableOptions.get(selectedIndex);
        int originalIndex = optionEntry.options().indexOf(selected);
        context.set(optionEntry.resultKey(), originalIndex);
        context.set(ContextKeys.SELECTED_OPTION, originalIndex);
        for (DialogueModifier modifier : selected.modifiers()) modifier.apply(context);
        if (optionEntry.selectionCallback() != null) optionEntry.selectionCallback().accept(originalIndex);
        for (DialogueTrigger trigger : selected.triggers()) trigger.execute(context);
        dispose();
        finish();
    }

    private void sendChoice() {
        String question = PlaceholderResolver.resolve(optionEntry.question(), context);
        if (optionEntry.speaker() != null) {
            question = PlaceholderResolver.resolve(optionEntry.speaker().displayName(), context) + ": " + question;
        }
        ChatFormatter.sendCenteredMessage(player, ChatColor.AQUA + question);
        ChatFormatter.constructDivider(player, " ", 45);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < usableOptions.size(); i++) {
            if (i > 0) builder.append(ChatColor.GRAY).append(" / ");
            ChatColor color = i == selectedIndex ? ChatColor.GREEN : ChatColor.WHITE;
            builder.append(ChatColor.DARK_GRAY).append("[").append(color)
                    .append(i == selectedIndex ? ChatColor.UNDERLINE : "")
                    .append(PlaceholderResolver.resolve(usableOptions.get(i).text(), context))
                    .append(ChatColor.DARK_GRAY).append("]");
        }
        ChatFormatter.sendCenteredMessage(player, builder.toString());
        ChatFormatter.sendCenteredMessage(player, ChatColor.GRAY + "(Scroll to cycle)");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}
