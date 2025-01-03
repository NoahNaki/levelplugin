package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.trade.utils.Translations;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradingWindow implements Listener {

    private static final int PLAYER_COIN_SLOT   = 0;  // now slot 0
    private static final int OPPONENT_COIN_SLOT = 8;  // now slot 8

    // Tracks players waiting for sign input and their respective TradingWindow
    private static final java.util.Map<UUID, TradingWindow> awaitingSignInput = new java.util.HashMap<>();
    private static final java.util.Set<UUID> activeSignInputs = new java.util.HashSet<>();

    // Stores the coin offers for both players
    private int playerCoinOffer = 0;     // Coins offered by the main player
    private int opponentCoinOffer = 0;  // Coins offered by the opponent


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

    public TradingWindow() {};

    public TradingWindow(Player player, Player oppositeDealPartner) {
        this.player = player;
        this.opposite = oppositeDealPartner;
        playerAcceptedDeal = false;
        oppositeAcceptedDeal = false;
        paidAfterClose = false;

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
        if(!this.paidAfterClose)
            oppositeDealPartner.openInventory(oppositeInventory);
        player.playNote(player.getLocation(), Instrument.SNARE_DRUM, Note.natural(1, Note.Tone.D));
        opposite.playNote(opposite.getLocation(), Instrument.SNARE_DRUM, Note.natural(1, Note.Tone.D));
    }

    private void openCoinSignGUI(Player p, TradingWindow tw) {
        // Mark the player as having an active sign input
        awaitingSignInput.put(p.getUniqueId(), tw);
        activeSignInputs.add(p.getUniqueId()); // Add to active sign input set

        Location loc = p.getLocation().clone().add(0, -1, 0);
        Block block = loc.getBlock();
        block.setType(Material.OAK_SIGN);

        Sign sign = (Sign) block.getState();
        sign.setLine(0, "Enter coins");
        sign.update(true, false);

        p.openSign(sign);

        // Cleanup after 10 seconds, just in case
        Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
            if (block.getType().toString().contains("SIGN")) {
                block.setType(Material.AIR);
            }
            activeSignInputs.remove(p.getUniqueId()); // Remove the active sign input
        }, 20L * 10);
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

        // Validate input (must be a positive number)
        if (!line0.matches("\\d+")) {
            p.sendMessage(ChatColor.RED + "Please enter a valid number!");
            return;
        }

        int coins = Integer.parseInt(line0); // Parse coin amount

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

        // Reopen the trade window properly by resetting the inventories
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

    private void updateCoinOfferItems() {
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
        for(int i = 0; i < ROWS * 9; i++) {
            if(isPersonalTradeAccepmentField(i)) {
                inv.setItem(i, ownGreenGlass);
            }
            else if(isOpponentsField(i)) {
                inv.setItem(i, separator);
            }
            else if(isOpponentsAccepmentField(i)) {
                inv.setItem(i, oppositeRedGlass);
            }
            else if(isFillerIndex(i)) {
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

        tw.oppositeAcceptedDeal = !tw.oppositeAcceptedDeal;
        for(int i = 0; i < ROWS * 9; i++) {
            if(tw.oppositeAcceptedDeal) {
                if(isOpponentsAccepmentField(i)) {
                    tw.playerInventory.setItem(i, tw.oppositeGreenGlass);
                }
                if(isPersonalTradeAccepmentField(i)) {
                    tw.oppositeInventory.setItem(i, tw.ownRedGlass);
                }
            } else {
                if(isOpponentsAccepmentField(i)) {
                    tw.playerInventory.setItem(i, tw.oppositeRedGlass);
                }
                if(isPersonalTradeAccepmentField(i)) {
                    tw.oppositeInventory.setItem(i, tw.ownGreenGlass);
                }
            }
        }

        if(playerAcceptedDeal && oppositeAcceptedDeal)
            tw.playerInventory.close();
    }

    public void toggleOwnStatus(TradingWindow tw, Inventory inv) {
        tw.playerAcceptedDeal = !tw.playerAcceptedDeal;
        for(int i = 0; i < ROWS * 9; i++) {
            if(tw.playerAcceptedDeal) {
                if(isOpponentsAccepmentField(i)) {
                    tw.oppositeInventory.setItem(i, tw.oppositeGreenGlass);
                }
                if(isPersonalTradeAccepmentField(i)) {
                    tw.playerInventory.setItem(i, tw.ownRedGlass);
                }
            } else {
                if(isOpponentsAccepmentField(i)) {
                    tw.oppositeInventory.setItem(i, tw.oppositeRedGlass);
                }
                if(isPersonalTradeAccepmentField(i)) {
                    tw.playerInventory.setItem(i, tw.ownGreenGlass);
                }
            }
        }
        if(playerAcceptedDeal && oppositeAcceptedDeal)
            tw.playerInventory.close();
    }

    public void closeTrade(Player player) {
        DealMaker dm = Main.getPlugin().getDealMaker();
        TradingWindow tw = this;

        Player p = tw.player;
        Player o = tw.opposite;

        if(!tw.paidAfterClose) {
            tw.paidAfterClose = true;
            if(tw.playerInventory.getViewers().contains(tw.player))
                tw.playerInventory.close();
            if(tw.oppositeInventory.getViewers().contains(tw.opposite))
                tw.oppositeInventory.close();
            if(tw.oppositeAcceptedDeal && tw.playerAcceptedDeal) {
                // Both accepted the deal and the items to deal get flipped

                // Check, if the items already got moved back to the inventory
                for(int i = 0; i < ROWS * 9; i++) {
                    if(isOwnField(i)) {
                        if(tw.playerInventory.getItem(i) != null) {
                            if(tw.opposite.getInventory().firstEmpty() > -1)
                                tw.opposite.getInventory().addItem(tw.playerInventory.getItem(i));
                            else {
                                tw.opposite.getWorld().dropItem(tw.opposite.getLocation(), tw.playerInventory.getItem(i));
                            }
                        }
                        if(tw.oppositeInventory.getItem(i) != null) {
                            if(tw.player.getInventory().firstEmpty() > -1)
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
                for(int i = 0; i < ROWS * 9; i++) {
                    if(isOwnField(i)) {
                        if(tw.playerInventory.getItem(i) != null) {
                            if(tw.player.getInventory().firstEmpty() > -1)
                                tw.player.getInventory().addItem(tw.playerInventory.getItem(i));
                            else {
                                tw.player.getWorld().dropItem(tw.player.getLocation(), tw.playerInventory.getItem(i));
                            }
                        }
                        if(tw.oppositeInventory.getItem(i) != null) {
                            if(tw.opposite.getInventory().firstEmpty() > -1)
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
        for(int i = 0; i < ROWS * 9; i++) {
            if(isOwnField(i)) count++;
        }
        return count;
    }

    private ItemStack[] projectToItemField(Inventory inv) {
        int pointer = 0; // keeps track of how many slots already inserted to result array
        ItemStack[] result = new ItemStack[this.slots];
        for(int i = 0; i < ROWS * 9; i++) {
            if(isOwnField(i)) {
                if(inv.getItem(i) != null)
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
        for(int i = 0; i < ROWS * 9; i++) {
            if(toPlayersInventory) {
                if(isOpponentsField(i)) {
                    if(playerItems[pointer] != null) {
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
                if(isOpponentsField(i)) {
                    if(playerItems[pointer] != null) {
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
        for(int i = 0; i < ROWS * 9; i++) {
            if((!invert && isOpponentsField(i)) || (invert && isOwnField(i)) && i < index) {
                opponentSlot++;
            }
        }
        for(int i = 0; i < ROWS * 9; i++) {
            if((!invert && isOwnField(i)) || (invert && isOpponentsField(i)) && opponentSlot > 0) {
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
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        DealMaker dm = Main.getPlugin().getDealMaker();

        // Check if the inventory is part of the trade system
        if (dm.isInventoryInList(e.getClickedInventory())) {
            TradingWindow tw = dm.getTradingWindow(e.getClickedInventory());

            // Player's Inventory Handling
            if (e.getClickedInventory().equals(tw.playerInventory)) {

                // Handle Player Coin Slot
                if (e.getSlot() == PLAYER_COIN_SLOT) {
                    e.setCancelled(true); // Prevent moving the item
                    if (tw.player.equals(p)) {
                        openCoinSignGUI(p, tw); // Open the sign GUI for the main player
                    }
                    return;
                }

                // Handle Opponent Coin Slot
                else if (e.getSlot() == OPPONENT_COIN_SLOT) {
                    e.setCancelled(true); // Prevent moving the item
                    if (tw.opposite.equals(p)) {
                        openCoinSignGUI(p, tw); // Open the sign GUI for the opponent
                    }
                    return;
                }

                // Handle Deal Acceptance Field
                if (isPersonalTradeAccepmentField(e.getSlot())) {
                    e.setCancelled(true);
                    this.toggleOwnStatus(tw, e.getClickedInventory());
                }
                // Handle Other Fields
                else if (isOwnField(e.getSlot())) {
                    if (tw.playerAcceptedDeal || tw.oppositeAcceptedDeal) e.setCancelled(true);
                    tw.refreshInventorySwitch();
                } else {
                    e.setCancelled(true);
                }

                // Opponent's Inventory Handling
            } else if (e.getClickedInventory().equals(tw.oppositeInventory)) {

                // Handle Opponent Coin Slot
                if (e.getSlot() == PLAYER_COIN_SLOT) {
                    e.setCancelled(true); // Prevent moving the item
                    if (tw.opposite.equals(p)) {
                        openCoinSignGUI(p, tw); // Open the sign GUI for the opponent
                    }
                    return;
                }

                // Handle Player Coin Slot (From Opponent's Perspective)
                else if (e.getSlot() == OPPONENT_COIN_SLOT) {
                    e.setCancelled(true); // Prevent moving the item
                    if (tw.player.equals(p)) {
                        openCoinSignGUI(p, tw); // Open the sign GUI for the main player
                    }
                    return;
                }

                // Handle Deal Acceptance Field
                if (isPersonalTradeAccepmentField(e.getSlot())) {
                    e.setCancelled(true);
                    this.toggleOpponentsStatus(tw);
                }
                // Handle Other Fields
                else if (isOwnField(e.getSlot())) {
                    if (tw.playerAcceptedDeal || tw.oppositeAcceptedDeal) e.setCancelled(true);
                    else e.setCancelled(false);
                    tw.refreshInventorySwitch();
                } else {
                    e.setCancelled(true);
                }
            }
        }
        // Handle players dealing outside trade window
        else if (dm.isPlayerCurrentlyDealing(p)) {
            TradingWindow tw = dm.getTradingWindowByPlayer(p);
            if (tw.playerAcceptedDeal || tw.oppositeAcceptedDeal) {
                if (e.isShiftClick()) e.setCancelled(true);
                else if (e.getClick().equals(ClickType.DOUBLE_CLICK)) e.setCancelled(true);
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
            TradingWindow tw = dm.getTradingWindowByPlayer((Player) e.getPlayer());

            // Ignore close events triggered by the sign GUI
            if (activeSignInputs.contains(e.getPlayer().getUniqueId())) {
                return; // Skip processing if player is still awaiting sign input
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
        if(dm.isInventoryInList(e.getInventory())) {
            TradingWindow tw = dm.getTradingWindow(e.getInventory());
            if(tw.playerAcceptedDeal || tw.oppositeAcceptedDeal) {
                e.setCancelled(true);
            } else {
                tw.refreshInventorySwitch();
            }
        }
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        DealMaker dm = Main.getPlugin().getDealMaker();
        if(dm.isPlayerCurrentlyDealing(e.getPlayer())) {
            TradingWindow tw = dm.getTradingWindowByPlayer(e.getPlayer());
            tw.closeTrade(e.getPlayer());
        }
    }

}