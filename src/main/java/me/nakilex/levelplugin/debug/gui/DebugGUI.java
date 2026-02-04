package me.nakilex.levelplugin.debug.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.chat.games.ChatGameManager;
import me.nakilex.levelplugin.chat.games.ChatGameStatus;
import me.nakilex.levelplugin.debug.DropDebugManager;
import me.nakilex.levelplugin.lootchests.managers.CooldownManager;
import me.nakilex.levelplugin.mob.managers.PlayerToggleManager;
import me.nakilex.levelplugin.mercenary.MercenaryExpeditionManager;
import me.nakilex.levelplugin.scoreboard.PlayerScoreboardManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import me.nakilex.levelplugin.utils.DoubleInputPrompt;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.ToggleFeedbackUtil;
import me.nakilex.levelplugin.utils.RewardBombUtil;
import me.nakilex.levelplugin.utils.NumericInputPrompt;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import me.nakilex.levelplugin.utils.gui.widgets.ActionWidget;
import me.nakilex.levelplugin.utils.gui.widgets.GuiContext;
import me.nakilex.levelplugin.utils.gui.widgets.GuiLayout;
import me.nakilex.levelplugin.utils.gui.widgets.GuiWidget;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.conversations.ConversationFactory;
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
    private static final int DROP_RATE_SLOT = 27;
    private static final int CHEST_RESPAWN_SLOT = 25;
    private static final int FORCE_DROP_SLOT = 29;
    private static final int REWARD_BOMB_SLOT = 33;
    private static final int[] CHAT_GAME_SLOTS = {28, 30, 32, 34, 22, 24};

    private final PlayerToggleManager mobDebugManager;
    private final PlayerScoreboardManager scoreboardManager;
    private final MercenaryExpeditionManager expeditionManager;
    private final DropDebugManager dropDebugManager;
    private final ChatGameManager chatGameManager;
    private final CooldownManager cooldownManager;
    private final Map<Integer, String> chatGameSlots = new HashMap<>();
    private final Map<String, ChatGameStatus> chatGameStatusById = new HashMap<>();
    private final List<GuiWidget> widgets;

    public DebugGUI(PlayerToggleManager mobDebugManager,
                    PlayerScoreboardManager scoreboardManager,
                    ChatGameManager chatGameManager,
                    MercenaryExpeditionManager expeditionManager,
                    DropDebugManager dropDebugManager,
                    CooldownManager cooldownManager) {
        this.mobDebugManager = mobDebugManager;
        this.scoreboardManager = scoreboardManager;
        this.expeditionManager = expeditionManager;
        this.dropDebugManager = dropDebugManager;
        this.chatGameManager = chatGameManager;
        this.cooldownManager = cooldownManager;
        this.widgets = buildWidgets();
    }

    /** Open the debug tools menu for the player. */
    public void open(Player player) {
        chatGameSlots.clear();
        chatGameStatusById.clear();

        GuiBuilder builder = GuiBuilder.create(GUI_SIZE, "Debug Tools")
                .filler(Material.GRAY_STAINED_GLASS_PANE)
                .fillEmptySlots(false)
                .border();

        Inventory inv = builder.build();
        renderWidgets(inv, player);

        if (chatGameManager != null) {
            List<ChatGameStatus> statuses = chatGameManager.getStatuses();
            for (int i = 0; i < statuses.size() && i < CHAT_GAME_SLOTS.length; i++) {
                ChatGameStatus status = statuses.get(i);
                int slot = CHAT_GAME_SLOTS[i];
                inv.setItem(slot, createChatGameItem(status));
                chatGameSlots.put(slot, status.id());
                recordStatus(status);
            }
        }

        player.openInventory(inv);
    }

    private List<GuiWidget> buildWidgets() {
        List<GuiWidget> widgetList = new java.util.ArrayList<>();
        widgetList.add(new ActionWidget(MOBINFO_SLOT, context -> createMobInfoItem(context.player()),
                (click, context) -> {
                    boolean enabled = mobDebugManager.toggle(context.player());
                    context.inventory().setItem(MOBINFO_SLOT, createMobInfoItem(context.player()));
                    ToggleFeedbackUtil.sendToggle(context.player(), "Mob info debug", enabled);
                }));
        widgetList.add(new ActionWidget(TPS_SLOT, context -> createTpsItem(context.player()),
                (click, context) -> {
                    boolean enabled = scoreboardManager.toggleTps(context.player());
                    context.inventory().setItem(TPS_SLOT, createTpsItem(context.player()));
                    ToggleFeedbackUtil.sendToggle(context.player(), "TPS display", enabled);
                }));
        widgetList.add(new ActionWidget(SIEGE_SLOT, context -> createFastSiegeItem(),
                (click, context) -> {
                    boolean enabled = me.nakilex.levelplugin.guild.siege.GuildSiegeManager.getInstance().toggleFastCapture();
                    context.inventory().setItem(SIEGE_SLOT, createFastSiegeItem());
                    ToggleFeedbackUtil.sendToggle(context.player(), "Fast siege mode", enabled);
                }));
        widgetList.add(new ActionWidget(EXPEDITION_SLOT, context -> createExpeditionItem(),
                (click, context) -> {
                    boolean enabled = !expeditionManager.isInstantExpeditions();
                    expeditionManager.setInstantExpeditions(enabled);
                    context.inventory().setItem(EXPEDITION_SLOT, createExpeditionItem());
                    ToggleFeedbackUtil.sendToggle(context.player(), "Instant expeditions", enabled);
                }));
        widgetList.add(new ActionWidget(CHEST_RESPAWN_SLOT, context -> createRespawnItem(),
                (click, context) -> openRespawnChatInput(context.player())));
        widgetList.add(new ActionWidget(DROP_RATE_SLOT, context -> createDropRateItem(),
                (click, context) -> openDropRateChatInput(context.player())));
        widgetList.add(new ActionWidget(FORCE_DROP_SLOT, context -> createForceDropItem(),
                (click, context) -> {
                    boolean enabled = dropDebugManager.toggleForceMobDrops();
                    context.inventory().setItem(FORCE_DROP_SLOT, createForceDropItem());
                    ToggleFeedbackUtil.sendToggle(context.player(), "Guaranteed mob drops", enabled);
                }));
        widgetList.add(new ActionWidget(REWARD_BOMB_SLOT, context -> createRewardBombItem(),
                (click, context) -> triggerRewardBomb(context.player(), context.inventory())));
        return widgetList;
    }

    private void renderWidgets(Inventory inventory, Player player) {
        GuiLayout layout = new GuiLayout(inventory);
        GuiContext context = new GuiContext(player, inventory);
        for (GuiWidget widget : widgets) {
            widget.contribute(layout, context);
        }
    }

    private void handleWidgetClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        GuiWidget widget = widgets.stream()
                .filter(w -> w.handlesSlot(slot))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return;
        }
        widget.onClick(slot, event.getClick(), new GuiContext(player, event.getView().getTopInventory()));
    }

    private ItemStack createMobInfoItem(Player player) {
        return GuiUtil.createToggleItem(
                mobDebugManager.isEnabled(player),
                "§bMob Info Debug",
                "§7Show MythicMob rewards on kill");
    }

    private ItemStack createTpsItem(Player player) {
        return GuiUtil.createToggleItem(
                scoreboardManager.isTpsEnabled(player),
                "§bShow TPS",
                "§7Display TPS on sidebar");
    }

    private ItemStack createFastSiegeItem() {
        boolean fast = me.nakilex.levelplugin.guild.siege.GuildSiegeManager.getInstance().isFastCapture();
        return GuiUtil.createToggleItem(
                fast,
                "§bFast Siege",
                "§750% progress per second");
    }

    private ItemStack createExpeditionItem() {
        return GuiUtil.createToggleItem(
                expeditionManager.isInstantExpeditions(),
                "§bInstant Expeditions",
                "§7Expeditions complete instantly");
    }

    private ItemStack createForceDropItem() {
        return GuiUtil.createToggleItem(
                dropDebugManager.isForceMobDrops(),
                "§bGuaranteed Mob Drops",
                "§7Force MythicMob loot and chests",
                "§7to drop every time.");
    }

    private ItemStack createRewardBombItem() {
        return createActionItem(
                Material.TNT,
                "§dReward Bomb",
                "§7Spawn debug loot at your",
                "§7targeted block (20 blocks).");
    }

    private void triggerRewardBomb(Player player, Inventory inventory) {
        var target = player.getTargetBlockExact(20);
        if (target == null) {
            ChatMessageUtil.send(player, MessageType.ERROR,
                    "Look at a block within 20 blocks to start the reward bomb.");
            return;
        }
        RewardBombUtil.startRewardBomb(Main.getInstance(), target.getLocation(),
                me.nakilex.levelplugin.debug.DebugRewardUtil::rollDebugReward, 100);
        inventory.setItem(REWARD_BOMB_SLOT, createRewardBombItem());
        ChatMessageUtil.send(player, MessageType.SUCCESS, ChatColor.LIGHT_PURPLE + "Reward bomb triggered.");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals("Debug Tools")) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) {
            return;
        }
        if (chatGameManager != null && chatGameSlots.containsKey(slot)) {
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
                event.getInventory().setItem(slot, createChatGameItem(updated));
                ToggleFeedbackUtil.sendToggle(player, updated.displayName() + " chat game", updated.enabled());
            }
            return;
        }
        handleWidgetClick(event, player);
    }

    private ItemStack createChatGameItem(ChatGameStatus status) {
        String displayName = "§b" + status.displayName();
        String idLore = "§7ID: §f" + status.id();
        String availability = status.playable()
                ? "§7Click to toggle this chat game."
                : "§cUnavailable - check chat_games.yml.";
        return GuiUtil.createToggleItem(status.enabled(), displayName, idLore, availability);
    }

    private ItemStack createActionItem(Material material, String displayName, String... lore) {
        List<String> loreLines = lore != null && lore.length > 0
                ? java.util.Arrays.asList(lore)
                : java.util.Collections.emptyList();
        return GuiUtil.createGuiItem(material, displayName, loreLines);
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

    private ItemStack createDropRateItem() {
        double dropRate = dropDebugManager.getGlobalGearDropRate();
        return createActionItem(
                Material.ENCHANTED_BOOK,
                "§bGlobal Gear Drop Rate",
                "§7Current: §f" + String.format("%.2f%%", dropRate),
                "§7Click to enter a new chance in chat."
        );
    }

    private ItemStack createRespawnItem() {
        int cooldown = cooldownManager.getDefaultCooldownSeconds();
        return createActionItem(
                Material.CLOCK,
                "§bLoot Chest Respawn",
                "§7Current: §f" + cooldown + "s",
                "§7Click to enter a new cooldown in chat."
        );
    }

    private void openDropRateChatInput(Player player) {
        player.closeInventory();
        ConversationFactory factory = new ConversationFactory(Main.getInstance())
                .withFirstPrompt(DoubleInputPrompt.percentagePrompt(
                        Main.getInstance(),
                        player,
                        ChatColor.GOLD + "Enter the global gear drop chance (%):",
                        value -> {
                            dropDebugManager.setGlobalGearDropRate(value);
                            ChatMessageUtil.send(player, MessageType.SUCCESS,
                                    ChatColor.GREEN + String.format("Global gear drop rate set to %.2f%%.", value));
                            Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player));
                        }
                ))
                .withLocalEcho(false)
                .withTimeout(30)
                .addConversationAbandonedListener(event ->
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player)));

        factory.buildConversation(player).begin();
    }

    private void openRespawnChatInput(Player player) {
        player.closeInventory();
        ConversationFactory factory = new ConversationFactory(Main.getInstance())
                .withFirstPrompt(new NumericInputPrompt<>(
                        Main.getInstance(),
                        player,
                        ChatColor.GOLD + "Enter loot chest respawn time in seconds:",
                        "Invalid input! Please enter a whole number of seconds.",
                        ChatColor.RED + "Please enter a value between 5 and 7200 seconds.",
                        Integer::parseInt,
                        value -> value >= 5 && value <= 7200,
                        value -> {
                            cooldownManager.setDefaultCooldownSeconds(value);
                            ChatMessageUtil.send(player, MessageType.SUCCESS,
                                    ChatColor.GREEN + "Loot chest respawn set to " + value + "s.");
                            Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player));
                        }
                ))
                .withLocalEcho(false)
                .withTimeout(30)
                .addConversationAbandonedListener(event ->
                        Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player)));

        factory.buildConversation(player).begin();
    }
}
