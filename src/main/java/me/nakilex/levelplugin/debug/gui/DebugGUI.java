package me.nakilex.levelplugin.debug.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.chat.games.ChatGameManager;
import me.nakilex.levelplugin.chat.games.ChatGameStatus;
import me.nakilex.levelplugin.debug.AutoCastManager;
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
import org.bukkit.inventory.meta.ItemMeta;

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
    private static final int AUTOCAST_SLOT = 31;
    private static final int REWARD_BOMB_SLOT = 33;
    private static final int[] CHAT_GAME_SLOTS = {28, 30, 32, 34, 22, 24};

    private final PlayerToggleManager mobDebugManager;
    private final PlayerScoreboardManager scoreboardManager;
    private final MercenaryExpeditionManager expeditionManager;
    private final DropDebugManager dropDebugManager;
    private final AutoCastManager autoCastManager;
    private final ChatGameManager chatGameManager;
    private final CooldownManager cooldownManager;
    private final Map<Integer, String> chatGameSlots = new HashMap<>();
    private final Map<String, ChatGameStatus> chatGameStatusById = new HashMap<>();

    public DebugGUI(PlayerToggleManager mobDebugManager,
                    PlayerScoreboardManager scoreboardManager,
                    ChatGameManager chatGameManager,
                    MercenaryExpeditionManager expeditionManager,
                    DropDebugManager dropDebugManager,
                    AutoCastManager autoCastManager,
                    CooldownManager cooldownManager) {
        this.mobDebugManager = mobDebugManager;
        this.scoreboardManager = scoreboardManager;
        this.expeditionManager = expeditionManager;
        this.dropDebugManager = dropDebugManager;
        this.autoCastManager = autoCastManager;
        this.chatGameManager = chatGameManager;
        this.cooldownManager = cooldownManager;
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
        builder.setItem(CHEST_RESPAWN_SLOT, createRespawnItem());
        builder.setItem(DROP_RATE_SLOT, createDropRateItem());
        builder.setItem(FORCE_DROP_SLOT, GuiUtil.createToggleItem(
                dropDebugManager.isForceMobDrops(),
                "§bGuaranteed Mob Drops",
                "§7Force MythicMob loot and chests",
                "§7to drop every time."));
        builder.setItem(AUTOCAST_SLOT, GuiUtil.createToggleItem(
                autoCastManager.isAutoCasting(player),
                "§bMage Autocast",
                "§7Auto-cast Fireball using TEC",
                "§7Requires Mage class."));
        builder.setItem(REWARD_BOMB_SLOT, createActionItem(
                Material.TNT,
                "§dReward Bomb",
                "§7Spawn debug loot at your",
                "§7targeted block (20 blocks)."));

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
        } else if (slot == CHEST_RESPAWN_SLOT) {
            openRespawnChatInput(player);
        } else if (slot == DROP_RATE_SLOT) {
            openDropRateChatInput(player);
        } else if (slot == FORCE_DROP_SLOT) {
            boolean enabled = dropDebugManager.toggleForceMobDrops();
            inv.setItem(FORCE_DROP_SLOT, GuiUtil.createToggleItem(enabled,
                    "§bGuaranteed Mob Drops",
                    "§7Force MythicMob loot and chests",
                    "§7to drop every time."));
            ToggleFeedbackUtil.sendToggle(player, "Guaranteed mob drops", enabled);
        } else if (slot == AUTOCAST_SLOT) {
            AutoCastManager.ToggleOutcome outcome = autoCastManager.toggleMageFireball(player);
            if (!outcome.success()) {
                ChatMessageUtil.send(player, MessageType.ERROR, outcome.errorMessage());
                return;
            }
            inv.setItem(AUTOCAST_SLOT, GuiUtil.createToggleItem(outcome.enabled(),
                    "§bMage Autocast",
                    "§7Auto-cast Fireball using TEC",
                    "§7Requires Mage class."));
            ToggleFeedbackUtil.sendToggle(player, "Mage autocast", outcome.enabled());
        } else if (slot == REWARD_BOMB_SLOT) {
            var target = player.getTargetBlockExact(20);
            if (target == null) {
                ChatMessageUtil.send(player, MessageType.ERROR,
                        "Look at a block within 20 blocks to start the reward bomb.");
                return;
            }
            RewardBombUtil.startRewardBomb(me.nakilex.levelplugin.Main.getInstance(), target.getLocation(),
                    me.nakilex.levelplugin.debug.DebugRewardUtil::rollDebugReward, 100);
            inv.setItem(REWARD_BOMB_SLOT, createActionItem(
                    Material.TNT,
                    "§dReward Bomb",
                    "§7Spawn debug loot at your",
                    "§7targeted block (20 blocks)."));
            ChatMessageUtil.send(player, MessageType.SUCCESS, ChatColor.LIGHT_PURPLE + "Reward bomb triggered.");
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

    private ItemStack createActionItem(Material material, String displayName, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            if (lore != null && lore.length > 0) {
                meta.setLore(java.util.Arrays.asList(lore));
            }
            stack.setItemMeta(meta);
        }
        return stack;
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
