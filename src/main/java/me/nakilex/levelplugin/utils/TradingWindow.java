package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.trade.utils.Translations;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class TradingWindow implements Listener {
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

    private void prepareInventory(Inventory inv) {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta im = filler.getItemMeta();
        im.setDisplayName(messageStrings.getTranslation(Translations.FILLER_ITEM));
        filler.setItemMeta(im);

        separator = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta imSep = separator.getItemMeta();
        imSep.setDisplayName(OPPOSITE_FIELD_GLASS_NAME);
        separator.setItemMeta(imSep);

        ItemStack personalTradeAccepment = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta imPTA = personalTradeAccepment.getItemMeta();
        imPTA.setDisplayName(messageStrings.getTranslation(Translations.ACCEPT_TRADE_ITEM));
        personalTradeAccepment.setItemMeta(imPTA);

        this.initGlassConfig();



        for(int i = 0; i < ROWS * 9; i++) {
            if(isPersonalTradeAccepmentField(i)) {
                inv.setItem(i, ownGreenGlass);
            } else if(isOpponentsField(i)) {
                inv.setItem(i, separator);
            } else if(isOpponentsAccepmentField(i)) {
                inv.setItem(i, oppositeRedGlass);
            } else if(isFillerIndex(i)) {
                inv.setItem(i, filler);
            }
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
        return index > 13 && index < 9 * ROWS - 9 && (index + 4) % 9 < 3;
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
        if(!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player) e.getWhoClicked();
        DealMaker dm = Main.getPlugin().getDealMaker();


        if(Main.getPlugin().getDealMaker().isInventoryInList(e.getClickedInventory())) {
            TradingWindow tw = Main.getPlugin().getDealMaker().getTradingWindow(e.getClickedInventory());

            // Save click slot for drop event

            if(e.getClick().isKeyboardClick()) {
                if(tw.player.equals(p) && e.getClickedInventory().equals(tw.playerInventory)) {
                    tw.cursorPlayer = e.getSlot();
                    e.setCancelled(true);
                }
                else if(tw.opposite.equals(p) && e.getClickedInventory().equals(tw.oppositeInventory)) {
                    tw.cursorOpponent = e.getSlot();
                    e.setCancelled(true);
                }
            }

            if(e.getClickedInventory().equals(tw.playerInventory)) {
                if(isPersonalTradeAccepmentField(e.getSlot())) {
                    // Player toggles deal status
                    e.setCancelled(true);
                    this.toggleOwnStatus(tw, e.getClickedInventory());
                } else if(isOwnField(e.getSlot())) {
                    if(tw.playerAcceptedDeal || tw.oppositeAcceptedDeal)
                        e.setCancelled(true);
                    tw.refreshInventorySwitch();
                } else {
                    e.setCancelled(true);
                }
            } else if(e.getClickedInventory().equals(tw.oppositeInventory)) {
                if(isPersonalTradeAccepmentField(e.getSlot())) {
                    // Opposite toggles own deal status
                    e.setCancelled(true);
                    this.toggleOpponentsStatus(tw);
                } else if(isOwnField(e.getSlot())) {
                    if(tw.playerAcceptedDeal || tw.oppositeAcceptedDeal)
                        e.setCancelled(true);
                    else
                        e.setCancelled(false);
                    tw.refreshInventorySwitch();
                } else {
                    e.setCancelled(true);
                }
            }
        } else if(dm.isPlayerCurrentlyDealing(p)) {
            TradingWindow tw = dm.getTradingWindowByPlayer(p);
            if(tw.playerAcceptedDeal || tw.oppositeAcceptedDeal) {
                if(e.isShiftClick())
                    e.setCancelled(true);
                else if(e.getClick().equals(ClickType.DOUBLE_CLICK)) // player double-clicks in own inventory
                    e.setCancelled(true); // prevent stealing items from own slot field after trade accepted
            } else if(e.isShiftClick()) {
                tw.refreshInventorySwitch();
            }
        }
    }


    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        DealMaker dm = Main.getPlugin().getDealMaker();
        if(dm.isInventoryInList(e.getInventory())) {
            TradingWindow tw = dm.getTradingWindowByPlayer((Player) e.getPlayer());

            if(e.getPlayer() instanceof Player) {
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