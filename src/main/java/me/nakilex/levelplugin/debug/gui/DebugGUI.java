package me.nakilex.levelplugin.debug.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.nakilex.levelplugin.chat.games.ChatGameManager;
import me.nakilex.levelplugin.chat.games.ChatGameStatus;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Simple GUI to toggle developer debug features like mob kill info
 * and TPS display, mirroring the style of the player settings menu.
 */
public class DebugGUI implements Listener {
    private static final int GUI_SIZE = 45;
    private static final int MOBINFO_SLOT = 11;
    private static final int TPS_SLOT = 15;
    private static final int SIEGE_SLOT = 13;
    private static final int EXPEDITION_SLOT = 20;
    private static final int[] CHAT_GAME_SLOTS = {28, 30, 32, 34, 22, 24};

    private final PlayerToggleManager mobDebugManager;
    private final PlayerScoreboardManager scoreboardManager;
    private final MercenaryExpeditionManager expeditionManager;
    private final ChatGameManager chatGameManager;
    private final Map<Integer, String> chatGameSlots = new HashMap<>();
    private final Map<String, ChatGameStatus> chatGameStatusById = new HashMap<>();

    public DebugGUI(PlayerToggleManager mobDebugManager,
                    PlayerScoreboardManager scoreboardManager,
                    ChatGameManager chatGameManager,
                    MercenaryExpeditionManager expeditionManager) {
        this.mobDebugManager = mobDebugManager;
        this.scoreboardManager = scoreboardManager;
        this.expeditionManager = expeditionManager;
        this.chatGameManager = chatGameManager;
    }

    /** Open the debug tools menu for the player. */
    public void open(Player player) {
        chatGameSlots.clear();
        chatGameStatusById.clear();

        GuiBuilder builder = GuiBuilder.create(GUI_SIZE, "Debug Tools")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border();

        builder.setItem(MOBINFO_SLOT, GuiUtil.createToggleItem(
                mobDebugManager.isEnabled(player),
                "§bMob Info Debug",
                "§7Show MythicMob rewards on kill"));
        builder.setItem(TPS_SLOT, GuiUtil.createToggleItem(
                scoreboardManager.isTpsEnabled(player),
                "§bShow TPS",
                "§7Display TPS on sidebar"));
        boolean fast = me.nakilex.levelplugin.guild.siege.GuildSiegeManager.getInstance().isFastCapture();
        builder.setItem(SIEGE_SLOT, GuiUtil.createToggleItem(
                fast,
                "§bFast Siege",
                "§750% progress per second"));
        builder.setItem(EXPEDITION_SLOT, GuiUtil.createToggleItem(
                expeditionManager.isInstantExpeditions(),
                "§bInstant Expeditions",
                "§7Expeditions complete instantly"));

        if (chatGameManager != null) {
            List<ChatGameStatus> statuses = chatGameManager.getStatuses();
            for (int i = 0; i < statuses.size() && i < CHAT_GAME_SLOTS.length; i++) {
                ChatGameStatus status = statuses.get(i);
                int slot = CHAT_GAME_SLOTS[i];
                builder.setItem(slot, createChatGameItem(status));
                chatGameSlots.put(slot, status.id());
                recordStatus(status);
            }
        }

        Inventory inv = builder.build();
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("Debug Tools")) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        Inventory inv = event.getInventory();
        if (slot == MOBINFO_SLOT) {
            boolean enabled = mobDebugManager.toggle(player);
            inv.setItem(MOBINFO_SLOT, GuiUtil.createToggleItem(enabled,
                    "§bMob Info Debug",
                    "§7Show MythicMob rewards on kill"));
            ToggleFeedbackUtil.sendToggle(player, "Mob info debug", enabled);
        } else if (slot == TPS_SLOT) {
            boolean enabled = scoreboardManager.toggleTps(player);
            inv.setItem(TPS_SLOT, GuiUtil.createToggleItem(enabled,
                    "§bShow TPS",
                    "§7Display TPS on sidebar"));
            ToggleFeedbackUtil.sendToggle(player, "TPS display", enabled);
        } else if (slot == SIEGE_SLOT) {
            boolean enabled = me.nakilex.levelplugin.guild.siege.GuildSiegeManager.getInstance().toggleFastCapture();
            inv.setItem(SIEGE_SLOT, GuiUtil.createToggleItem(enabled,
                    "§bFast Siege",
                    "§750% progress per second"));
            ToggleFeedbackUtil.sendToggle(player, "Fast siege mode", enabled);
        } else if (slot == EXPEDITION_SLOT) {
            boolean enabled = !expeditionManager.isInstantExpeditions();
            expeditionManager.setInstantExpeditions(enabled);
            inv.setItem(EXPEDITION_SLOT, GuiUtil.createToggleItem(enabled,
                    "§bInstant Expeditions",
                    "§7Expeditions complete instantly"));
            ToggleFeedbackUtil.sendToggle(player, "Instant expeditions", enabled);
        } else if (chatGameManager != null && chatGameSlots.containsKey(slot)) {
            String id = chatGameSlots.get(slot);
            ChatGameStatus status = chatGameStatusById.get(id.toLowerCase(Locale.ROOT));
            boolean enable = status == null || !status.enabled();
            if (!chatGameManager.setGameEnabled(id, enable)) {
                ChatMessageUtil.send(player, MessageType.ERROR,
                        "Unable to toggle chat game '" + id + "'.");
                return;
            }
            refreshChatGameStatus(id);
            ChatGameStatus updated = chatGameStatusById.get(id.toLowerCase(Locale.ROOT));
            if (updated != null) {
                inv.setItem(slot, createChatGameItem(updated));
                ToggleFeedbackUtil.sendToggle(player, updated.displayName() + " chat game", updated.enabled());
            }
        }
    }

    private ItemStack createChatGameItem(ChatGameStatus status) {
        String displayName = "§b" + status.displayName();
        String idLore = "§7ID: §f" + status.id();
        String availability = status.playable()
                ? "§7Click to toggle this chat game."
                : "§cUnavailable - check chat_games.yml.";
        return GuiUtil.createToggleItem(status.enabled(), displayName, idLore, availability);
    }

    private void recordStatus(ChatGameStatus status) {
        chatGameStatusById.put(status.id().toLowerCase(Locale.ROOT), status);
    }

    private void refreshChatGameStatus(String id) {
        if (chatGameManager == null || id == null) {
            return;
        }
        chatGameManager.getStatuses().stream()
                .filter(status -> status.id().equalsIgnoreCase(id))
                .findFirst()
                .ifPresent(this::recordStatus);
    }
}
