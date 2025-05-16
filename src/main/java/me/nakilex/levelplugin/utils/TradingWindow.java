package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.trade.utils.Translations;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class TradingWindow implements Listener {

    private static final int PLAYER_COIN_SLOT = 0;  // now slot 0
    private static final int OPPONENT_COIN_SLOT = 8;  // now slot 8

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

    ItemStack oppositeRedGlass;
    ItemStack oppositeGreenGlass;
    ItemStack ownRedGlass;
    ItemStack ownGreenGlass;
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

    ;

    public TradingWindow(Player player, Player oppositeDealPartner) {
        this.player = player;
        this.opposite = oppositeDealPartner;
        playerAcceptedDeal = false;
        oppositeAcceptedDeal = false;
        paidAfterClose = false;

        // Initialize the economy manager here
        this.economyManager = Main.getPlugin().getEconomyManager();

        this.playerInventory = Bukkit.createInventory(null, CHEST_SIZE,
            String.format(messageStrings.getTranslation(Translations.DEAL_WITH), oppositeDealPartner.getName()));
        this.oppositeInventory = Bukkit.createInventory(null, CHEST_SIZE,
            String.format(messageStrings.getTranslation(Translations.DEAL_WITH), player.getName()));

        prepareInventory(playerInventory);
        prepareInventory(oppositeInventory);

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


//    private void openCoinSignGUI(Player p, TradingWindow tw) {
//        // Mark the player as having an active sign input
//        awaitingSignInput.put(p.getUniqueId(), tw);
//        activeSignInputs.add(p.getUniqueId()); // Add to active sign input set
//
//        // Define the specific location where the sign will be spawned
//        Location loc = p.getLocation().clone().add(0, -1, 0);
//        Block block = loc.getBlock();
//        block.setType(Material.OAK_SIGN);
//
//        Sign sign = (Sign) block.getState();
//        sign.setLine(0, "Enter coins");
//        sign.update(true, false);
//
//        // Track the sign's location for cleanup
//        activeSignLocations.put(p.getUniqueId(), loc);
//
//        p.openSign(sign);
//    }

    private void openCoinChatInput(Player p, TradingWindow tw) {
        // 1. Snapshot the current trade‐item slots
        tw.playerSlots = tw.projectToItemField(tw.playerInventory);
        tw.oppositeSlots = tw.projectToItemField(tw.oppositeInventory);

        // 1b. Debug log how many items you’ve captured
        int playerNonNull = Arrays.stream(tw.playerSlots).filter(Objects::nonNull).toArray().length;
        int oppNonNull = Arrays.stream(tw.oppositeSlots).filter(Objects::nonNull).toArray().length;
        Bukkit.getLogger().info("[TradeDebug] Captured “playerSlots” count = " + playerNonNull);
        Bukkit.getLogger().info("[TradeDebug] Captured “oppositeSlots” count = " + oppNonNull);

        // 2. Mark awaiting chat so onInventoryClose won’t cancel
        awaitingChatInput.add(p.getUniqueId());
        p.closeInventory();

        // 3. Show chat prompt
        ConversationFactory factory = new ConversationFactory(Main.getPlugin())
            .withFirstPrompt(new CoinInputPrompt(tw, p))
            .withLocalEcho(false)
            .withTimeout(30)
            .addConversationAbandonedListener(event -> {
                // Always reopen, whether they typed or timed out
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
                    tw.playerCoinOffer = coins;
                    p.sendMessage(ChatColor.GREEN + "You set your coin offer to: " + coins);
                } else if (tw.opposite.equals(p)) {
                    tw.opponentCoinOffer = coins;
                    p.sendMessage(ChatColor.GREEN + "You set your coin offer to: " + coins);
                }

                // Update the coin display in inventories
                tw.updateCoinOfferItems();
            }
        }

        // Reopen the trade window regardless of input validity
        Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
            // Clear old inventories and create fresh ones
            tw.playerInventory = Bukkit.createInventory(null, tw.CHEST_SIZE,
                String.format(tw.messageStrings.getTranslation(Translations.DEAL_WITH), tw.opposite.getName()));
            tw.oppositeInventory = Bukkit.createInventory(null, tw.CHEST_SIZE,
                String.format(tw.messageStrings.getTranslation(Translations.DEAL_WITH), tw.player.getName()));

            // Reinitialize inventory contents
            tw.prepareInventory(tw.playerInventory);
            tw.prepareInventory(tw.oppositeInventory);

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
        // Update the player's coin offer
        ItemStack yourCoinIngot = playerInventory.getItem(PLAYER_COIN_SLOT);
        if (yourCoinIngot != null && yourCoinIngot.getType() == Material.GOLD_INGOT) {
            ItemMeta meta = yourCoinIngot.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Your Coin Offer");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Coins: " + playerCoinOffer);
            meta.setLore(lore);
            yourCoinIngot.setItemMeta(meta);
        }

        // Update the opponent's coin offer
        ItemStack opponentCoinIngot = playerInventory.getItem(OPPONENT_COIN_SLOT);
        if (opponentCoinIngot != null && opponentCoinIngot.getType() == Material.GOLD_INGOT) {
            ItemMeta meta = opponentCoinIngot.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Opponent's Coin Offer");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Coins: " + opponentCoinOffer);
            meta.setLore(lore);
            opponentCoinIngot.setItemMeta(meta);
        }

        // Mirror the same updates on the opposite's inventory
        ItemStack oppCoinIngot = oppositeInventory.getItem(PLAYER_COIN_SLOT);
        if (oppCoinIngot != null && oppCoinIngot.getType() == Material.GOLD_INGOT) {
            ItemMeta meta = oppCoinIngot.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Your Coin Offer");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Coins: " + opponentCoinOffer); // From their perspective
            meta.setLore(lore);
            oppCoinIngot.setItemMeta(meta);
        }

        ItemStack yourOppCoinIngot = oppositeInventory.getItem(OPPONENT_COIN_SLOT);
        if (yourOppCoinIngot != null && yourOppCoinIngot.getType() == Material.GOLD_INGOT) {
            ItemMeta meta = yourOppCoinIngot.getItemMeta();
            meta.setDisplayName(ChatColor.GOLD + "Opponent's Coin Offer");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Coins: " + playerCoinOffer); // From their perspective
            meta.setLore(lore);
            yourOppCoinIngot.setItemMeta(meta);
        }
    }


    private void prepareInventory(Inventory inv) {
        // 1) Create “filler” & other standard items:
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta im = filler.getItemMeta();
        im.setDisplayName(messageStrings.getTranslation(Translations.FILLER_ITEM));
        filler.setItemMeta(im);

        separator = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta imSep = separator.getItemMeta();
        imSep.setDisplayName(OPPOSITE_FIELD_GLASS_NAME);
        separator.setItemMeta(imSep);

        // Prepare personal trade acceptance (green glass)
        ItemStack personalTradeAccepment = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta imPTA = personalTradeAccepment.getItemMeta();
        imPTA.setDisplayName(messageStrings.getTranslation(Translations.ACCEPT_TRADE_ITEM));
        personalTradeAccepment.setItemMeta(imPTA);

        // 2) Prepare our red & green glass items for toggling accept status:
        this.initGlassConfig();

        // 3) Fill the entire inventory layout with filler, accept fields, etc.
        for (int i = 0; i < ROWS * 9; i++) {
            if (isPersonalTradeAccepmentField(i)) {
                inv.setItem(i, ownGreenGlass);
            } else if (isOpponentsField(i)) {
                inv.setItem(i, separator);
            } else if (isOpponentsAccepmentField(i)) {
                inv.setItem(i, oppositeRedGlass);
            } else if (isFillerIndex(i)) {
                inv.setItem(i, filler);
            }
        }

        // ----------------------------------------------------------
        // 4) Place the coin-offer ingots in slots 0 (player) & 8 (opponent).
        // ----------------------------------------------------------
        if (inv.equals(playerInventory)) {
            // This is the main player's view
            ItemStack yourCoinIngot = new ItemStack(Material.GOLD_INGOT);
            ItemMeta yourCoinMeta = yourCoinIngot.getItemMeta();
            yourCoinMeta.setDisplayName("Your Coin Offer");
            yourCoinIngot.setItemMeta(yourCoinMeta);

            // Place it in slot 0
            inv.setItem(PLAYER_COIN_SLOT, yourCoinIngot);

            ItemStack opponentCoinIngot = new ItemStack(Material.GOLD_INGOT);
            ItemMeta oppCoinMeta = opponentCoinIngot.getItemMeta();
            oppCoinMeta.setDisplayName("Opponent's Coin Offer");
            opponentCoinIngot.setItemMeta(oppCoinMeta);

            // Place it in slot 8
            inv.setItem(OPPONENT_COIN_SLOT, opponentCoinIngot);

        } else if (inv.equals(oppositeInventory)) {
            // This is the opposite player's view
            ItemStack yourCoinIngot = new ItemStack(Material.GOLD_INGOT);
            ItemMeta yourCoinMeta = yourCoinIngot.getItemMeta();
            yourCoinMeta.setDisplayName("Your Coin Offer");
            yourCoinIngot.setItemMeta(yourCoinMeta);

            inv.setItem(PLAYER_COIN_SLOT, yourCoinIngot);

            ItemStack opponentCoinIngot = new ItemStack(Material.GOLD_INGOT);
            ItemMeta oppCoinMeta = opponentCoinIngot.getItemMeta();
            oppCoinMeta.setDisplayName("Opponent's Coin Offer");
            opponentCoinIngot.setItemMeta(oppCoinMeta);

            inv.setItem(OPPONENT_COIN_SLOT, opponentCoinIngot);
        }
    }


    public void initGlassConfig() {
        oppositeRedGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta imRed = oppositeRedGlass.getItemMeta();
        imRed.setDisplayName(messageStrings.getTranslation(Translations.OPPOSITE_DID_NOT_ACCEPTED_TRADE_ITEM));
        oppositeRedGlass.setItemMeta(imRed);

        oppositeGreenGlass = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta imOppGreen = oppositeGreenGlass.getItemMeta();
        imOppGreen.setDisplayName(messageStrings.getTranslation(Translations.OPPOSITE_ACCEPTS_DEAL_ITEM));
        oppositeGreenGlass.setItemMeta(imOppGreen);

        ownRedGlass = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta imOwnRed = ownRedGlass.getItemMeta();
        imOwnRed.setDisplayName(messageStrings.getTranslation(Translations.OWN_DECLINE_DEAL_ITEM));
        ownRedGlass.setItemMeta(imOwnRed);

        ownGreenGlass = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta imOwnGreen = ownGreenGlass.getItemMeta();
        imOwnGreen.setDisplayName(messageStrings.getTranslation(Translations.OWN_ACCEPT_DEAL_ITEM));
        ownGreenGlass.setItemMeta(imOwnGreen);
    }


    // -- Togggler for deal status

    public void toggleOpponentsStatus(TradingWindow tw) {
        tw.oppositeAcceptedDeal = !tw.oppositeAcceptedDeal; // Toggle opponent acceptance

        for (int i = 0; i < ROWS * 9; i++) {
            if (tw.oppositeAcceptedDeal) {
                if (isOpponentsAccepmentField(i)) {
                    tw.playerInventory.setItem(i, tw.oppositeGreenGlass);
                }
                if (isPersonalTradeAccepmentField(i)) {
                    tw.oppositeInventory.setItem(i, tw.ownRedGlass);
                }
            } else {
                if (isOpponentsAccepmentField(i)) {
                    tw.playerInventory.setItem(i, tw.oppositeRedGlass);
                }
                if (isPersonalTradeAccepmentField(i)) {
                    tw.oppositeInventory.setItem(i, tw.ownGreenGlass);
                }
            }
        }

        // Auto-close and finalize the trade if both players have accepted
        if (tw.playerAcceptedDeal && tw.oppositeAcceptedDeal) {
            tw.player.closeInventory();
            tw.opposite.closeInventory();
            tw.closeTrade(tw.player); // Finalize trade
        }
    }


    public void toggleOwnStatus(TradingWindow tw, Inventory inv) {
        tw.playerAcceptedDeal = !tw.playerAcceptedDeal; // Toggle acceptance status

        for (int i = 0; i < ROWS * 9; i++) {
            if (tw.playerAcceptedDeal) {
                if (isOpponentsAccepmentField(i)) {
                    tw.oppositeInventory.setItem(i, tw.oppositeGreenGlass);
                }
                if (isPersonalTradeAccepmentField(i)) {
                    tw.playerInventory.setItem(i, tw.ownRedGlass);
                }
            } else {
                if (isOpponentsAccepmentField(i)) {
                    tw.oppositeInventory.setItem(i, tw.oppositeRedGlass);
                }
                if (isPersonalTradeAccepmentField(i)) {
                    tw.playerInventory.setItem(i, tw.ownGreenGlass);
                }
            }
        }

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
                economyManager.addCoins(o, tw.playerCoinOffer);
                economyManager.deductCoins(o, tw.opponentCoinOffer);
                economyManager.addCoins(p, tw.opponentCoinOffer);

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
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                o.playSound(o.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

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
                if (stack != null
                    && stack.hasItemMeta()
                    && stack.getItemMeta().getPersistentDataContainer()
                    .has(ItemUtil.ITEM_UUID_KEY, PersistentDataType.STRING)) {
                    ItemUtil.updateCustomItemTooltip(stack, recipient);
                }
            }
            recipient.updateInventory();
        }
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
                if (inv.getItem(i) != null)
                    result[pointer] = inv.getItem(i);
                else
                    result[pointer] = null;
                pointer++;
            }
        }
        return result;
    }

    private void projectToOpponentField(ItemStack[] playerItems, boolean toPlayersInventory) {
        int pointer = 0;
        for (int i = 0; i < ROWS * 9; i++) {
            if (toPlayersInventory) {
                if (isOpponentsField(i)) {
                    if (playerItems[pointer] != null) {
                        ItemStack itemStack = playerItems[pointer].clone();
                        ItemMeta im = itemStack.getItemMeta();
                        ArrayList<String> meta = new ArrayList<String>();
                        meta.add(messageStrings.getTranslation(Translations.DEAL_PARTNERS_LORE_1));
                        meta.add(messageStrings.getTranslation(Translations.DEAL_PARTNERS_LORE_2));
                        im.setLore(meta);
                        itemStack.setItemMeta(im);
                        this.playerInventory.setItem(i, itemStack);
                    } else {
                        this.playerInventory.setItem(i, this.separator);
                    }
                    pointer++;
                }
            } else {
                if (isOpponentsField(i)) {
                    if (playerItems[pointer] != null) {
                        ItemStack itemStack = playerItems[pointer].clone();
                        ItemMeta im = itemStack.getItemMeta();
                        ArrayList<String> meta = new ArrayList<String>();
                        meta.add(messageStrings.getTranslation(Translations.DEAL_PARTNERS_LORE_1));
                        meta.add(messageStrings.getTranslation(Translations.DEAL_PARTNERS_LORE_2));
                        im.setLore(meta);
                        itemStack.setItemMeta(im);
                        this.oppositeInventory.setItem(i, itemStack);
                    } else {
                        this.oppositeInventory.setItem(i, this.separator);
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
                    if (tw.playerAcceptedDeal || tw.oppositeAcceptedDeal) {
                        e.setCancelled(true);
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
                    if (tw.playerAcceptedDeal || tw.oppositeAcceptedDeal) {
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
            if (tw.playerAcceptedDeal || tw.oppositeAcceptedDeal) {
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
    public void setPlayerCoinOffer(int coins) {
        this.playerCoinOffer = coins;
    }

    public void setOpponentCoinOffer(int coins) {
        this.opponentCoinOffer = coins;
    }

    /**
     * Reopens the trading inventories for both players.
     * This method encapsulates the logic you currently run after the sign input.
     */
    public void reopenInventories() {
        // 1) Create fresh inventories
        this.playerInventory = Bukkit.createInventory(null, CHEST_SIZE,
            String.format(messageStrings.getTranslation(Translations.DEAL_WITH), this.opposite.getName()));
        this.oppositeInventory = Bukkit.createInventory(null, CHEST_SIZE,
            String.format(messageStrings.getTranslation(Translations.DEAL_WITH), this.player.getName()));
        prepareInventory(this.playerInventory);
        prepareInventory(this.oppositeInventory);

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
        refreshInventorySwitch();
    }

}