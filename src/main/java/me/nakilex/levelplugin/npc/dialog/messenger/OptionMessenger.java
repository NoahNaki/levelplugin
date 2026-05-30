package me.nakilex.levelplugin.npc.dialog.messenger;

import me.nakilex.levelplugin.npc.dialog.entry.OptionDialogueEntry;
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

/** Messenger that owns scroll-wheel option selection events. */
public final class OptionMessenger extends DialogueMessenger implements Listener {
    private final OptionDialogueEntry optionEntry;
    private int selectedIndex;
    private long openedAt;

    public OptionMessenger(Player player, OptionDialogueEntry entry, InteractionContext context) {
        super(player, entry, context);
        this.optionEntry = entry;
    }

    @Override
    public void init() {
        super.init();
        openedAt = System.currentTimeMillis();
        Bukkit.getPluginManager().registerEvents(this, context.plugin());
        sendChoice();
    }

    @Override
    public void dispose() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onScroll(PlayerItemHeldEvent event) {
        if (!event.getPlayer().equals(player) || state() != State.RUNNING) return;
        event.setCancelled(true);
        selectedIndex += event.getNewSlot() > event.getPreviousSlot() ? 1 : -1;
        if (selectedIndex < 0) selectedIndex = optionEntry.options().size() - 1;
        if (selectedIndex >= optionEntry.options().size()) selectedIndex = 0;
        sendChoice();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!event.getPlayer().equals(player) || state() != State.RUNNING) return;
        if (context.npc() != null && !context.npc().matches(event.getRightClicked())) return;
        event.setCancelled(true);
        requestNextOrSkip();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer().equals(player)) {
            cancel();
        }
    }

    @Override
    public void requestNextOrSkip() {
        if (System.currentTimeMillis() - openedAt < 400) {
            sendChoice();
            return;
        }
        context.set(optionEntry.resultKey(), selectedIndex);
        OptionDialogueEntry.Option selected = optionEntry.options().get(selectedIndex);
        for (DialogueModifier modifier : selected.modifiers()) {
            modifier.apply(context);
        }
        if (optionEntry.callback() != null) {
            optionEntry.callback().accept(selectedIndex);
        }
        for (DialogueTrigger trigger : selected.triggers()) {
            trigger.execute(context);
        }
        dispose();
        finish();
    }

    private void sendChoice() {
        ChatFormatter.sendCenteredMessage(player, ChatColor.AQUA + optionEntry.question());
        ChatFormatter.constructDivider(player, " ", 45);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < optionEntry.options().size(); i++) {
            if (i > 0) builder.append(ChatColor.GRAY).append(" / ");
            ChatColor color = i == selectedIndex ? ChatColor.GREEN : ChatColor.WHITE;
            builder.append(ChatColor.DARK_GRAY).append("[")
                    .append(color).append(i == selectedIndex ? ChatColor.UNDERLINE : "")
                    .append(optionEntry.options().get(i).text())
                    .append(ChatColor.DARK_GRAY).append("]");
        }
        ChatFormatter.sendCenteredMessage(player, builder.toString());
        ChatFormatter.sendCenteredMessage(player, ChatColor.GRAY + "(Scroll to cycle)");
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}
