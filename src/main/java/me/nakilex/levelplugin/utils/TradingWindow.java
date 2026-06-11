package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.advancement.AdvancementToastUtil;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.items.listeners.StaticItemListener;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.trade.utils.Translations;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CurrencyMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.IntegerInputPrompt;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.gui.GuiBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.*;

public class TradingWindow implements Listener {

    private static final NamespacedKey FIRST_TRADE_ACHIEVEMENT_KEY = new NamespacedKey(Main.getPlugin(), "first_trade_achievement");
    private static final int PLAYER_COIN_SLOT = 0;  // now slot 0
    private static final int OPPONENT_COIN_SLOT = 8;  // now slot 8
    private static final int INFO_SLOT = 4;

    // Tracks players waiting for sign input and their respective TradingWindow
    private static final java.util.Map<UUID, TradingWindow> awaitingSignInput = new java.util.HashMap<>();
    private static final java.util.Set<UUID> activeSignInputs = new java.util.HashSet<>();
    private final Map<UUID, Location> activeSignLocations = new HashMap<>();
    private static final Set<UUID> awaitingChatInput = new HashSet<>();


    // Stores the coin offers for both players
    private int playerCoinOffer = 0;     // Coins offered by the main player
    private int opponentCoinOffer = 0;  // Coins offered by the opponent

    private EconomyManager economyManager;


    MessageStrings messageStrings = Main.getPlugin().getMessageStrings();
    final int ROWS = 6;
    final int CHEST_SIZE = 9 * ROWS;
    final String OPPOSITE_FIELD_GLASS_NAME = messageStrings.getTranslation(Translations.DEAL_PARTNERS_FIELD);
    int slots;

    Player player;
    Player opposite;

    Inventory playerInventory;
    Inventory oppositeInventory;

    ItemStack[] playerSlots;
    ItemStack[] oppositeSlots;

    ItemStack opponentPendingItem;
    ItemStack opponentReadyItem;
    ItemStack ownCancelItem;
    ItemStack ownReadyItem;
    ItemStack separator;

    Item droppedItemByPlayer;
    Item droppedItemByOpponent;
    int cursorPlayer;
    int cursorOpponent;

    boolean playerAcceptedDeal;
    boolean oppositeAcceptedDeal;
    boolean paidAfterClose;

    public TradingWindow() {
    }

    public TradingWindow(Player player, Player oppositeDealPartner) {
        this.player = player;
        this.opposite = oppositeDealPartner;
        playerAcceptedDeal = false;
        oppositeAcceptedDeal = false;
        paidAfterClose = false;

        // Initialize the economy manager here
        this.economyManager = Main.getPlugin().getEconomyManager();

        this.playerInventory = createInventory(oppositeDealPartner.getName());
        this.oppositeInventory = createInventory(player.getName());

        prepareInventory(playerInventory, oppositeDealPartner);
        prepareInventory(oppositeInventory, player);

        this.slots = this.countOwnSlots();
        this.playerSlots = new ItemStack[slots];
        this.oppositeSlots = new ItemStack[slots];

        DealMaker dm = Main.getPlugin().getDealMaker();
        dm.addTradingWindow(this);
        player.openInventory(playerInventory);
        if (!this.paidAfterClose)
            oppositeDealPartner.openInventory(oppositeInventory);
        player.playNote(player.getLocation(), Instrument.SNARE_DRUM, Note.natural(1, Note.Tone.D));
        opposite.playNote(opposite.getLocation(), Instrument.SNARE_DRUM, Note.natural(1, Note.Tone.D));
    }


    private Inventory createInventory(String partnerName) {
        String formatted = String.format(messageStrings.getTranslation(Translations.DEAL_WITH), partnerName);
        String plainTitle = ChatColor.stripColor(formatted);
        return GuiBuilder.create(CHEST_SIZE, plainTitle)
            .filler(Material.GRAY_STAINED_GLASS_PANE)
            .fillEmptySlots(false)
            .border()
            .build();
    }


    private void openCoinChatInput(Player p, TradingWindow tw) {
        // 1. Snapshot the current trade-item slots so we can restore them after chat input
        tw.playerSlots = tw.projectToItemField(tw.playerInventory);
        tw.oppositeSlots = tw.projectToItemField(tw.oppositeInventory);

        // 2. Mark awaiting chat so onInventoryClose won’t cancel
        awaitingChatInput.add(p.getUniqueId());
        p.closeInventory();

        // 3. Show chat prompt
        ConversationFactory factory = new ConversationFactory(Main.getPlugin())
            .withFirstPrompt(IntegerInputPrompt.coinAmountWithinBalance(
                    Main.getPlugin(),
                    p,
                    ChatColor.GOLD + "Please enter the number of coins you want to offer:",
                    () -> tw.getEconomyManager().getBalance(p),
                    false,
                    amt -> {
                        if (tw.getPlayer().equals(p)) {
                            tw.setPlayerCoinOffer(p, amt);
                        } else if (tw.getOpponent().equals(p)) {
                            tw.setOpponentCoinOffer(p, amt);
                        }
                        if (amt == 0) {
                            p.sendMessage(ChatColor.YELLOW + "You cleared your coin offer.");
                        } else {
                            p.sendMessage(ChatColor.GREEN + "Your coin offer has been set to: " + amt);
                        }
                        tw.updateCoinOfferItems();
                        tw.reopenInventories();
                    }
            ))
            .withLocalEcho(false)
            .withTimeout(30)
            .addConversationAbandonedListener(event -> {
                awaitingChatInput.remove(p.getUniqueId());
                Bukkit.getScheduler().runTask(Main.getPlugin(), tw::reopenInventories);
            });
        factory.buildConversation(p).begin();
    }


    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        Player p = e.getPlayer();

        // Check if the player is awaiting input
        if (!awaitingSignInput.containsKey(p.getUniqueId())) {
            return; // Player is not in input mode, ignore
        }

        // Retrieve the TradingWindow instance
        TradingWindow tw = awaitingSignInput.remove(p.getUniqueId());
        activeSignInputs.remove(p.getUniqueId()); // Remove active input status immediately

        // Read the first line of input
        String line0 = e.getLine(0);

        // Check if input is invalid
        boolean isValidInput = line0.matches("\\d+");
        if (!isValidInput) {
            p.sendMessage(ChatColor.RED + "Invalid input! Please enter a valid number.");
        } else {
            int coins = Integer.parseInt(line0); // Parse coin amount

            // Check if player has enough coins
            if (tw.economyManager.getBalance(p) < coins) {
                p.sendMessage(ChatColor.RED + "You do not have enough coins to offer this amount.");
            } else {
                // Update the coin offer for the correct player
                if (tw.player.equals(p)) {
                    tw.setPlayerCoinOffer(p, coins);
                    p.sendMessage(ChatColor.GREEN + "You set your coin offer to: " + coins);
                } else if (tw.opposite.equals(p)) {
                    tw.setOpponentCoinOffer(p, coins);
                    p.sendMessage(ChatColor.GREEN + "You set your coin offer to: " + coins);
                }

                // Update the coin display in inventories
                tw.updateCoinOfferItems();
            }
        }

        // Reopen the trade window regardless of input validity
        Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
            // Clear old inventories and create fresh ones
            tw.playerInventory = tw.createInventory(tw.opposite.getName());
            tw.oppositeInventory = tw.createInventory(tw.player.getName());

            // Reinitialize inventory contents
            tw.prepareInventory(tw.playerInventory, tw.opposite);
            tw.prepareInventory(tw.oppositeInventory, tw.player);

            // Sync slots and updates
            tw.projectToOpponentField(tw.playerSlots, false);
            tw.projectToOpponentField(tw.oppositeSlots, true);
            tw.updateCoinOfferItems();

            // Rebind the inventories to DealMaker to register them properly
            DealMaker dm = Main.getPlugin().getDealMaker();
            dm.addTradingWindow(tw); // Re-register this trade window

            // Open the inventories for both players
            tw.player.openInventory(tw.playerInventory);
            tw.opposite.openInventory(tw.oppositeInventory);

            // Ensure everything is synced
            tw.refreshInventorySwitch();
        }, 1L); // Delay by 1 tick
    }


    void updateCoinOfferItems() {
        updateCoinSlot(playerInventory, PLAYER_COIN_SLOT, playerCoinOffer, true);
        updateCoinSlot(playerInventory, OPPONENT_COIN_SLOT, opponentCoinOffer, false);
        updateCoinSlot(oppositeInventory, PLAYER_COIN_SLOT, opponentCoinOffer, true);
        updateCoinSlot(oppositeInventory, OPPONENT_COIN_SLOT, playerCoinOffer, false);
    }

    private void updateCoinSlot(Inventory inv, int slot, int amount, boolean editable) {
        ItemStack stack = inv.getItem(slot);
        if (stack == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        meta.setLore(buildCoinLore(amount, editable));
        stack.setItemMeta(meta);
    }


    private void prepareInventory(Inventory inv, Player partner) {
        ensureStatusItems();

        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);

        for (int i = 0; i < ROWS * 9; i++) {
            if (isPersonalTradeAccepmentField(i)) {
                inv.setItem(i, ownReadyItem.clone());
            } else if (isOpponentsField(i)) {
                inv.setItem(i, separator.clone());
            } else if (isOpponentsAccepmentField(i)) {
                inv.setItem(i, opponentPendingItem.clone());
            } else if (isFillerIndex(i)) {
                inv.setItem(i, filler.clone());
            } else {
                inv.setItem(i, null);
            }
        }

        inv.setItem(PLAYER_COIN_SLOT, createCoinSlot(true));
        inv.setItem(OPPONENT_COIN_SLOT, createCoinSlot(false));
        inv.setItem(INFO_SLOT, createInfoItem(partner));
    }
    private void initStatusItems() {
        String ownAccept = ChatColor.stripColor(messageStrings.getTranslation(Translations.OWN_ACCEPT_DEAL_ITEM));
        String ownDecline = ChatColor.stripColor(messageStrings.getTranslation(Translations.OWN_DECLINE_DEAL_ITEM));
        String opponentWaiting = ChatColor.stripColor(messageStrings.getTranslation(Translations.OPPOSITE_DID_NOT_ACCEPTED_TRADE_ITEM));
        String opponentAccepted = ChatColor.stripColor(messageStrings.getTranslation(Translations.OPPOSITE_ACCEPTS_DEAL_ITEM));

        ownReadyItem = buildStatusItem("check", ChatColor.GREEN, ownAccept, createAcceptLore());
        ownCancelItem = buildStatusItem("cross", ChatColor.RED, ownDecline, createCancelLore());
        opponentPendingItem = buildStatusItem("cross", ChatColor.RED, opponentWaiting, createOpponentWaitingLore());
        opponentReadyItem = buildStatusItem("check", ChatColor.GREEN, opponentAccepted, createOpponentReadyLore());
        separator = createSeparatorItem();
    }

    private void ensureStatusItems() {
        if (ownReadyItem == null || ownCancelItem == null || opponentPendingItem == null
            || opponentReadyItem == null || separator == null) {
            initStatusItems();
        }
    }

    private ItemStack buildStatusItem(String iconId, ChatColor color, String name, List<String> loreLines) {
        ItemStack item = GuiUtil.getNexoItem(iconId, color + "" + ChatColor.BOLD + name);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setLore(new ArrayList<>(loreLines));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> createAcceptLore() {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Mark yourself ready when you're");
        lore.add(ChatColor.GRAY + "happy with the trade.");
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to ready up", null));
        return lore;
    }

    private List<String> createCancelLore() {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "You're ready to trade.");
        lore.add(ChatColor.GRAY + "Click again to make changes.");
        lore.add(" ");
        lore.addAll(TooltipUtil.clickInstructions("to cancel your readiness", null));
        return lore;
    }

    private List<String> createOpponentWaitingLore() {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Waiting for your partner to");
        lore.add(ChatColor.GRAY + "confirm the trade.");
        return lore;
    }

    private List<String> createOpponentReadyLore() {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Your partner has readied up.");
        lore.add(ChatColor.GRAY + "The trade will complete once you");
        lore.add(ChatColor.GRAY + "are also ready.");
        return lore;
    }

    private ItemStack createSeparatorItem() {
        ItemStack item = GuiUtil.createFiller(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String label = ChatColor.stripColor(OPPOSITE_FIELD_GLASS_NAME);
            meta.setDisplayName(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + label);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Your partner's offering slots.");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCoinSlot(boolean editable) {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = editable ? ChatColor.GOLD + "" + ChatColor.BOLD + "Your Coin Offer"
                : ChatColor.GOLD + "" + ChatColor.BOLD + "Partner Coin Offer";
            meta.setDisplayName(name);
            meta.setLore(buildCoinLore(0, editable));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> buildCoinLore(int amount, boolean editable) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Coins: " + ChatColor.YELLOW + amount + " <glyph:coins_icon>");
        lore.add(" ");
        if (editable) {
            lore.addAll(TooltipUtil.clickInstructions("to set your offer", null));
            lore.add(ChatColor.GRAY + "Enter 0 to clear your offer.");
        } else {
            lore.add(ChatColor.GRAY + "Updates when your partner changes");
            lore.add(ChatColor.GRAY + "their offer.");
        }
        return lore;
    }

    private ItemStack createInfoItem(Player partner) {
        ItemStack info = GuiUtil.getNexoItem("info", ChatColor.YELLOW + "How Trading Works");
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Partner: " + ChatColor.GOLD + partner.getName());
            lore.add(" ");
            lore.add(ChatColor.GRAY + "• Place items in the left slots.");
            lore.add(ChatColor.GRAY + "• Coins can be offered via the");
            lore.add(ChatColor.GRAY + "  sunflower buttons.");
            lore.add(ChatColor.GRAY + "• Both players must ready up.");
            lore.add(" ");
            lore.add(ChatColor.GRAY + "Closing the menu cancels the trade.");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            info.setItemMeta(meta);
        }
        return info;
    }


    // -- Togggler for deal status

    public void toggleOpponentsStatus(TradingWindow tw) {
        tw.ensureStatusItems();
        tw.oppositeAcceptedDeal = !tw.oppositeAcceptedDeal; // Toggle opponent acceptance
        tw.refreshAcceptanceVisuals();

        // Auto-close and finalize the trade if both players have accepted
        if (tw.playerAcceptedDeal && tw.oppositeAcceptedDeal) {
            tw.player.closeInventory();
            tw.opposite.closeInventory();
            tw.closeTrade(tw.player); // Finalize trade
        }
    }


    public void toggleOwnStatus(TradingWindow tw, Inventory inv) {
        tw.ensureStatusItems();
        tw.playerAcceptedDeal = !tw.playerAcceptedDeal; // Toggle acceptance status
        tw.refreshAcceptanceVisuals();

        // Auto-close and finalize the trade if both players have accepted
        if (tw.playerAcceptedDeal && tw.oppositeAcceptedDeal) {
            tw.player.closeInventory();
            tw.opposite.closeInventory();
            tw.closeTrade(tw.player); // Finalize trade
        }
    }


    public void closeTrade(Player player) {
        DealMaker dm = Main.getPlugin().getDealMaker();
        TradingWindow tw = this;

        Player p = tw.player;
        Player o = tw.opposite;

        if (!tw.paidAfterClose) {
            tw.paidAfterClose = true;
            if (tw.playerInventory.getViewers().contains(tw.player))
                tw.playerInventory.close();
            if (tw.oppositeInventory.getViewers().contains(tw.opposite))
                tw.oppositeInventory.close();

            if (tw.oppositeAcceptedDeal && tw.playerAcceptedDeal) {
                // Both accepted the deal and the items to deal get flipped

                // Deduct and add coins based on offers
                economyManager.deductCoins(p, tw.playerCoinOffer);
                economyManager.addCoins(o, tw.playerCoinOffer, false);
                economyManager.deductCoins(o, tw.opponentCoinOffer);
                economyManager.addCoins(p, tw.opponentCoinOffer, false);

                sendCoinSummary(p, tw.playerCoinOffer, tw.opponentCoinOffer, o.getName());
                sendCoinSummary(o, tw.opponentCoinOffer, tw.playerCoinOffer, p.getName());

                // Check, if the items already got moved back to the inventory
                for (int i = 0; i < ROWS * 9; i++) {
                    if (isOwnField(i)) {
                        if (tw.playerInventory.getItem(i) != null) {
                            if (tw.opposite.getInventory().firstEmpty() > -1)
                                tw.opposite.getInventory().addItem(tw.playerInventory.getItem(i));
                            else {
                                tw.opposite.getWorld().dropItem(tw.opposite.getLocation(), tw.playerInventory.getItem(i));
                            }
                        }
                        if (tw.oppositeInventory.getItem(i) != null) {
                            if (tw.player.getInventory().firstEmpty() > -1)
                                tw.player.getInventory().addItem(tw.oppositeInventory.getItem(i));
                            else {
                                tw.player.getWorld().dropItem(tw.player.getLocation(), tw.oppositeInventory.getItem(i));
                            }
                        }
                    }
                }
                showFirstTradeAchievement(p);
                showFirstTradeAchievement(o);

                dm.removeTradingWindow(tw);
            } else {
                // Deal got declined, both players get their own items back
                for (int i = 0; i < ROWS * 9; i++) {
                    if (isOwnField(i)) {
                        if (tw.playerInventory.getItem(i) != null) {
                            if (tw.player.getInventory().firstEmpty() > -1)
                                tw.player.getInventory().addItem(tw.playerInventory.getItem(i));
                            else {
                                tw.player.getWorld().dropItem(tw.player.getLocation(), tw.playerInventory.getItem(i));
                            }
                        }
                        if (tw.oppositeInventory.getItem(i) != null) {
                            if (tw.opposite.getInventory().firstEmpty() > -1)
                                tw.opposite.getInventory().addItem(tw.oppositeInventory.getItem(i));
                            else {
                                tw.opposite.getWorld().dropItem(tw.opposite.getLocation(), tw.oppositeInventory.getItem(i));
                            }
                        }
                    }
                }

                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);
                o.playSound(o.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.0f);

                boolean eventPlayerIsOpponent = player.equals(tw.opposite);
                final String YOU_DECLINED = String.format(Main.PREFIX + messageStrings.getTranslation(
                    Translations.YOU_DECLINED_DEAL), (eventPlayerIsOpponent ? p.getName() : o.getName()));
                final String OTHER_DECLINED = Main.PREFIX + (eventPlayerIsOpponent ? o.getName() : p.getName()) +
                    messageStrings.getTranslation(Translations.OPPONENT_DECLINED_DEAL);

                p.sendMessage(eventPlayerIsOpponent ? OTHER_DECLINED : YOU_DECLINED);
                o.sendMessage(eventPlayerIsOpponent ? YOU_DECLINED : OTHER_DECLINED);
                dm.removeTradingWindow(tw);
            }
        }
        for (Player recipient : Arrays.asList(this.player, this.opposite)) {
            for (ItemStack stack : recipient.getInventory().getContents()) {
                if (stack != null && stack.hasItemMeta()) {
                    boolean isCustomItem = stack.getItemMeta().getPersistentDataContainer()
                            .has(ItemUtil.ITEM_UUID_KEY, PersistentDataType.STRING);
                    boolean isCustomTool = me.nakilex.levelplugin.items.tools.ToolManager.getInstance().isToolMaterial(stack.getType());
                    if (isCustomItem || isCustomTool) {
                        ItemUtil.updateTooltip(stack, recipient);
                    }
                }
            }
            recipient.updateInventory();
        }
    }

    private void sendCoinSummary(Player recipient, int paid, int received, String partnerName) {
        StringBuilder message = new StringBuilder();
        message.append(ChatColor.GRAY).append("Trade with ")
                .append(ChatColor.YELLOW).append(partnerName)
                .append(ChatColor.GRAY).append(": ");

        message.append("Received ")
                .append(CurrencyMessageUtil.formatAmount(CurrencyMessageUtil.Currency.COINS, Math.max(0, received)))
                .append(ChatColor.GRAY);

        message.append(" • Paid ")
                .append(CurrencyMessageUtil.formatAmount(CurrencyMessageUtil.Currency.COINS, Math.max(0, paid)))
                .append(ChatColor.GRAY)
                .append(".");

        ChatMessageUtil.send(recipient, ChatMessageUtil.MessageType.SUCCESS, message.toString());
    }

    private void showFirstTradeAchievement(Player recipient) {
        if (recipient == null) {
            return;
        }
        var data = recipient.getPersistentDataContainer();
        if (data.has(FIRST_TRADE_ACHIEVEMENT_KEY, PersistentDataType.BYTE)) {
            return;
        }
        data.set(FIRST_TRADE_ACHIEVEMENT_KEY, PersistentDataType.BYTE, (byte) 1);
        AdvancementToastUtil.showToast(recipient, Material.EMERALD, "First Trade!",
                "Complete your first player trade.",
                me.nakilex.levelplugin.advancement.model.AdvancementDisplay.FrameType.TASK);
    }

    private boolean isUntradeableItem(ItemStack item) {
        return ItemUtil.isSoulbound(item) || StaticItemListener.isStaticItem(item);
    }

    // --- Slot checker

    private boolean isPersonalTradeAccepmentField(int index) {
        return index > 9 * ROWS - 9 && index < 9 * ROWS - 5;
    }


    private boolean isOpponentsAccepmentField(int index) {
        return index > 9 * ROWS - 5 && index < 9 * ROWS - 1;
    }

    private boolean isOwnField(int index) {
        return index > 9 && index < 9 * ROWS - 9 && (index + 8) % 9 < 3;
    }

    private boolean isOpponentsField(int index) {
        return index >= 13 && index < 9 * ROWS - 9 && (index + 4) % 9 < 3;
    }


    private boolean isFillerIndex(int index) {
        return index % 9 == 0 || (index + 1) % 9 == 0 || index < 9 || index > 9 * ROWS - 9 || (index + 5) % 9 == 0;
    }

    private int countOwnSlots() {
        int count = 0;
        for (int i = 0; i < ROWS * 9; i++) {
            if (isOwnField(i)) count++;
        }
        return count;
    }

    private ItemStack[] projectToItemField(Inventory inv) {
        int pointer = 0; // keeps track of how many slots already inserted to result array
        ItemStack[] result = new ItemStack[this.slots];
        for (int i = 0; i < ROWS * 9; i++) {
            if (isOwnField(i)) {
                ItemStack current = inv.getItem(i);
                result[pointer] = current == null ? null : current.clone();
                pointer++;
            }
        }
        return result;
    }

    private void projectToOpponentField(ItemStack[] playerItems, boolean toPlayersInventory) {
        ensureStatusItems();
        int pointer = 0;
        for (int i = 0; i < ROWS * 9; i++) {
            if (toPlayersInventory) {
                if (isOpponentsField(i)) {
                    if (playerItems[pointer] != null) {
                        ItemStack itemStack = playerItems[pointer].clone();
                        this.playerInventory.setItem(i, itemStack);
                    } else {
                        this.playerInventory.setItem(i, this.separator.clone());
                    }
                    pointer++;
                }
            } else {
                if (isOpponentsField(i)) {
                    if (playerItems[pointer] != null) {
                        ItemStack itemStack = playerItems[pointer].clone();
                        this.oppositeInventory.setItem(i, itemStack);
                    } else {
                        this.oppositeInventory.setItem(i, this.separator.clone());
                    }
                    pointer++;
                }
            }
        }
    }

    private void _refreshInventorySwitchAsyncHelper() {
        // Helper method, submethoded to get calles async with some delay to wait, until the item got stored in inv

        this.playerSlots = this.projectToItemField(this.playerInventory);
        this.projectToOpponentField(this.playerSlots, false);
        this.oppositeSlots = this.projectToItemField(this.oppositeInventory);
        this.projectToOpponentField(this.oppositeSlots, true);
    }

    private void refreshInventorySwitch() {
        //Just callingg the _refreshInventorySwitchAsyncHelper() method with some async delay to wait for item store

        TradingWindow tw = this;
        Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getPlugin(), new Runnable() {
            @Override
            public void run() {
                tw._refreshInventorySwitchAsyncHelper();
            }
        }, 4);
    }

    private int translateOpponentSlotIndexToOwnSlotIndex(int index, boolean invert) {
        // invert parameter makes the method to a "translateOwnSlotIndexToOpponentSlotIndex()-method
        int opponentSlot = 0;
        int ownSlot = -1;
        for (int i = 0; i < ROWS * 9; i++) {
            if ((!invert && isOpponentsField(i)) || (invert && isOwnField(i)) && i < index) {
                opponentSlot++;
            }
        }
        for (int i = 0; i < ROWS * 9; i++) {
            if ((!invert && isOwnField(i)) || (invert && isOpponentsField(i)) && opponentSlot > 0) {
                opponentSlot--;
                ownSlot = i;
            }
        }
        return ownSlot;
    }

    private int translateOpponentSlotIndexToOwnSlotIndex(int index) {
        return translateOpponentSlotIndexToOwnSlotIndex(index, false);
    }

    private int translateOwnSlotIndexToOpponentSlotIndex(int index) {
        return translateOpponentSlotIndexToOwnSlotIndex(index, true);
    }

    // --- EventHandlers

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player p = (Player) e.getWhoClicked();
        DealMaker dm = Main.getPlugin().getDealMaker();

        // Check if the clicked inventory belongs to the trading system
        if (dm.isInventoryInList(e.getClickedInventory())) {
            TradingWindow tw = dm.getTradingWindow(e.getClickedInventory());

            // Handle the main player's inventory view
            if (e.getClickedInventory().equals(tw.playerInventory)) {
                // Handle the player's own coin offer slot
                if (e.getSlot() == PLAYER_COIN_SLOT) {
                    e.setCancelled(true);
                    if (tw.player.equals(p)) {
                        openCoinChatInput(p, tw);
                    }
                    return;
                }
                // Handle the opponent's coin offer slot (from main player's perspective)
                else if (e.getSlot() == OPPONENT_COIN_SLOT) {
                    e.setCancelled(true);
                    if (tw.opposite.equals(p)) {
                        openCoinChatInput(p, tw);
                    }
                    return;
                }
                // Handle clicking the deal acceptance field
                if (isPersonalTradeAccepmentField(e.getSlot())) {
                    e.setCancelled(true);
                    toggleOwnStatus(tw, e.getClickedInventory());
                }
                // Allow interacting with own item fields if neither party has accepted yet
                else if (isOwnField(e.getSlot())) {
                    if (isUntradeableItem(e.getCursor()) || isUntradeableItem(e.getCurrentItem())) {
                        e.setCancelled(true);
                        p.sendMessage(ChatColor.RED + "That item cannot be traded.");
                    } else if (tw.playerAcceptedDeal || tw.oppositeAcceptedDeal) {
                        e.setCancelled(true);
                    } else {
                        e.setCancelled(false);
                    }
                    tw.refreshInventorySwitch();
                } else {
                    e.setCancelled(true);
                }
            }
            // Handle the opponent's inventory view
            else if (e.getClickedInventory().equals(tw.oppositeInventory)) {
                // Handle the coin offer slots from the opponent's perspective
                if (e.getSlot() == PLAYER_COIN_SLOT) {
                    e.setCancelled(true);
                    if (tw.opposite.equals(p)) {
                        openCoinChatInput(p, tw);
                    }
                    return;
                } else if (e.getSlot() == OPPONENT_COIN_SLOT) {
                    e.setCancelled(true);
                    if (tw.player.equals(p)) {
                        openCoinChatInput(p, tw);
                    }
                    return;
                }
                // Handle clicking the acceptance field
                if (isPersonalTradeAccepmentField(e.getSlot())) {
                    e.setCancelled(true);
                    toggleOpponentsStatus(tw);
                }
                // Allow interacting with own item fields if neither party has accepted yet
                else if (isOwnField(e.getSlot())) {
                    if (isUntradeableItem(e.getCursor()) || isUntradeableItem(e.getCurrentItem())) {
                        e.setCancelled(true);
                        p.sendMessage(ChatColor.RED + "That item cannot be traded.");
                    } else if (tw.playerAcceptedDeal || tw.oppositeAcceptedDeal) {
                        e.setCancelled(true);
                    } else {
                        e.setCancelled(false);
                    }
                    tw.refreshInventorySwitch();
                } else {
                    e.setCancelled(true);
                }
            }
        }
        // If the clicked inventory isn't part of the trade but the player is in a trade session…
        else if (dm.isPlayerCurrentlyDealing(p)) {
            TradingWindow tw = dm.getTradingWindowByPlayer(p);
            if (tw.isUntradeableItem(e.getCurrentItem()) || tw.isUntradeableItem(e.getCursor())) {
                e.setCancelled(true);
                p.sendMessage(ChatColor.RED + "That item cannot be traded.");
            } else if (tw.playerAcceptedDeal || tw.oppositeAcceptedDeal) {
                if (e.isShiftClick() || e.getClick().equals(ClickType.DOUBLE_CLICK)) {
                    e.setCancelled(true);
                }
            } else if (e.isShiftClick()) {
                tw.refreshInventorySwitch();
            }
        }
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        DealMaker dm = Main.getPlugin().getDealMaker();

        // Check if the inventory belongs to the trade system
        if (dm.isInventoryInList(e.getInventory())) {
            // If the player is awaiting chat input, don't close the trade.
            if (awaitingChatInput.contains(e.getPlayer().getUniqueId())) {
                return; // Skip trade closure
            }

            TradingWindow tw = dm.getTradingWindowByPlayer((Player) e.getPlayer());

            // Cleanup any active sign (if any are still present)
            UUID playerId = e.getPlayer().getUniqueId();
            if (activeSignLocations.containsKey(playerId)) {
                Location signLocation = activeSignLocations.get(playerId);
                if (signLocation.getBlock().getType().toString().contains("SIGN")) {
                    signLocation.getBlock().setType(Material.AIR);
                }
                activeSignLocations.remove(playerId); // Clean up tracking
            }

            // Handle trade closure only if it's a genuine exit
            if (e.getPlayer() instanceof Player) {
                Player p = (Player) e.getPlayer();
                tw.closeTrade(p);
            }
        }
    }


    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        DealMaker dm = Main.getPlugin().getDealMaker();
        if (dm.isInventoryInList(e.getInventory())) {
            TradingWindow tw = dm.getTradingWindow(e.getInventory());
            if (tw.playerAcceptedDeal || tw.oppositeAcceptedDeal) {
                e.setCancelled(true);
            } else {
                tw.refreshInventorySwitch();
            }
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        DealMaker dm = Main.getPlugin().getDealMaker();
        if (dm.isPlayerCurrentlyDealing(e.getPlayer())) {
            TradingWindow tw = dm.getTradingWindowByPlayer(e.getPlayer());
            tw.closeTrade(e.getPlayer());
        }
    }

    // Getters for necessary fields
    public EconomyManager getEconomyManager() {
        return this.economyManager;
    }

    public Player getPlayer() {
        return this.player;
    }

    public Player getOpponent() {
        return this.opposite;
    }

    // Setters for coin offers
    public void setPlayerCoinOffer(Player changer, int coins) {
        boolean changed = this.playerCoinOffer != coins;
        this.playerCoinOffer = coins;
        if (changed) {
            handleCoinOfferChanged(changer);
        }
    }

    public void setOpponentCoinOffer(Player changer, int coins) {
        boolean changed = this.opponentCoinOffer != coins;
        this.opponentCoinOffer = coins;
        if (changed) {
            handleCoinOfferChanged(changer);
        }
    }

    /**
     * Reopens the trading inventories for both players.
     * This method encapsulates the logic you currently run after the sign input.
     */
    public void reopenInventories() {
        // 1) Create fresh inventories
        this.playerInventory = createInventory(this.opposite.getName());
        this.oppositeInventory = createInventory(this.player.getName());
        prepareInventory(this.playerInventory, this.opposite);
        prepareInventory(this.oppositeInventory, this.player);

        // 2) Restore each player’s _own_ items into their own fields
        //    (playerSlots → playerInventory)
        int pPtr = 0;
        for (int i = 0; i < ROWS * 9; i++) {
            if (isOwnField(i)) {
                ItemStack stack = playerSlots[pPtr++];
                if (stack != null) {
                    playerInventory.setItem(i, stack);
                }
            }
        }
        //    (oppositeSlots → oppositeInventory)
        int oPtr = 0;
        for (int i = 0; i < ROWS * 9; i++) {
            if (isOwnField(i)) {
                ItemStack stack = oppositeSlots[oPtr++];
                if (stack != null) {
                    oppositeInventory.setItem(i, stack);
                }
            }
        }

        // 3) Project those same slots into the opponent-view fields
        projectToOpponentField(this.playerSlots, false);  // your items appear in their GUI
        projectToOpponentField(this.oppositeSlots, true); // their items appear in yours

        // 4) Restore coin displays, re-register and re-open
        updateCoinOfferItems();
        Main.getPlugin().getDealMaker().addTradingWindow(this);
        player.openInventory(playerInventory);
        opposite.openInventory(oppositeInventory);
        refreshAcceptanceVisuals();
        refreshInventorySwitch();
    }

    private void refreshAcceptanceVisuals() {
        if (playerInventory == null || oppositeInventory == null) {
            return;
        }

        ensureStatusItems();
        for (int i = 0; i < ROWS * 9; i++) {
            if (isPersonalTradeAccepmentField(i)) {
                ItemStack playerItem = (playerAcceptedDeal ? ownCancelItem : ownReadyItem).clone();
                ItemStack opponentItem = (oppositeAcceptedDeal ? ownCancelItem : ownReadyItem).clone();
                playerInventory.setItem(i, playerItem);
                oppositeInventory.setItem(i, opponentItem);
            } else if (isOpponentsAccepmentField(i)) {
                ItemStack playerView = (oppositeAcceptedDeal ? opponentReadyItem : opponentPendingItem).clone();
                ItemStack opponentView = (playerAcceptedDeal ? opponentReadyItem : opponentPendingItem).clone();
                playerInventory.setItem(i, playerView);
                oppositeInventory.setItem(i, opponentView);
            }
        }
    }

    private void handleCoinOfferChanged(Player changer) {
        if (!playerAcceptedDeal && !oppositeAcceptedDeal) {
            return;
        }

        playerAcceptedDeal = false;
        oppositeAcceptedDeal = false;
        refreshAcceptanceVisuals();

        Player other = changer.equals(player) ? opposite : player;

        ChatMessageUtil.send(changer, ChatMessageUtil.MessageType.WARNING,
            ChatColor.GRAY + "You changed the coin offer. Both players need to ready up again.");
        ChatMessageUtil.send(other, ChatMessageUtil.MessageType.WARNING,
            ChatColor.YELLOW + changer.getName() + ChatColor.GRAY + " changed the coin offer. Both players need to ready up again.");
    }

}
