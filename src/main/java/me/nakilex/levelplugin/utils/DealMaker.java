package me.nakilex.levelplugin.utils;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.trade.data.ConfigValues;
import me.nakilex.levelplugin.trade.utils.MessageStrings;
import me.nakilex.levelplugin.trade.utils.Translations;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

public class DealMaker {
    private HashMap<UUID, Player> pairs = new HashMap<UUID, Player>(); // Owner saved as UUID in key
    private ArrayList<TradingWindow> currentDealInvs = new ArrayList<TradingWindow>();
    private ArrayList<Player> cooldownRightClick = new ArrayList<Player>();


    private HashMap<UUID, ArrayList<UUID>> blocked = new HashMap<>();
    private HashMap<UUID, ArrayList<UUID>> unblocked = new HashMap<>();

    private ArrayList<UUID> generalBlocks = new ArrayList<UUID>();


    public boolean makeTradeOffer(Player owner, Player target) {
        MessageStrings messageStrings = Main.getPlugin().getMessageStrings();
        ConfigValues configValues = Main.getPlugin().getConfigValues();
        if(owner.getUniqueId().equals(target.getUniqueId())) {
            owner.sendMessage(Main.PREFIX + messageStrings.getTranslation(Translations.CAN_NOT_TRADE_WITH_YOURSELF));
            return false;
        } else if(pairs.containsKey(owner.getUniqueId())) {
            owner.sendMessage(String.format(Main.PREFIX + messageStrings.getTranslation(Translations.ALREADY_SENT_TRADE_REQUEST), pairs.get(owner.getUniqueId()).getName()));
            return false;
        } else if(!isDistanceNearEnough(owner, target)) {
            owner.sendMessage(String.format(Main.PREFIX + messageStrings.getTranslation(Translations.PLAYER_TO_FAR_AWAY), configValues.MAX_DISTANCE_FOR_USING_TRADE_COMMAND));
            return false;
        } else if(isPlayerBlocked(owner, target)) {
            owner.sendMessage(String.format(Main.PREFIX + messageStrings.getTranslation(Translations.PLAYER_DOES_NOT_ACCEPT_TRADE_REQUESTS)));
            return false;
        } else {
            pairs.put(owner.getUniqueId(), target);

            final Component CHAT_BUTTON_COMPONENT = Component.empty()
                .append(LegacyComponentSerializer.legacySection()
                    .deserialize(Main.PREFIX + String.format(messageStrings.getTranslation(Translations.YOU_GOT_A_NEW_TRADE_OFFER) + " ", owner.getName())))
                .append(
                    Component.text("[").color(NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD)
                ).append(
                    Component.text(messageStrings.getTranslation(Translations.CHAT_BUTTON_ACCEPT)).color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/trade accept " + owner.getName()))
                        .hoverEvent(Component.text(messageStrings.getTranslation(Translations.CHAT_BUTTON_ACCEPT)).color(NamedTextColor.DARK_GRAY))
                ).append(
                    Component.text(" | ").color(NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD)
                ).append(
                    Component.text(messageStrings.getTranslation(Translations.CHAT_BUTTON_DENY)).color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/trade deny " + owner.getName()))
                        .hoverEvent(Component.text(messageStrings.getTranslation(Translations.CHAT_BUTTON_DENY)).color(NamedTextColor.DARK_GRAY))
                ).append(
                    Component.text("]").color(NamedTextColor.DARK_GRAY).decorate(TextDecoration.BOLD)
                ).toBuilder().build();

            target.sendMessage(CHAT_BUTTON_COMPONENT);
            target.playSound(target.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_3, 1.0f, 1.0f);
            owner.playSound(owner.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
            Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    Player opponent = Main.getPlugin().getDealMaker().cancelTrade(owner);
                    if(opponent != null) {
                        opponent.sendMessage(String.format(Main.PREFIX + messageStrings.getTranslation(Translations.TRADE_REQUEST_BY_EXPIRED), owner.getName()));
                        opponent.playSound(opponent.getLocation(), Sound.ENTITY_RAVAGER_CELEBRATE, 1.0f, 1.0f);
                        owner.sendMessage(String.format(Main.PREFIX + messageStrings.getTranslation(Translations.OWN_TRADE_REQUEST_EXPIRED), opponent.getName()));
                        owner.playSound(owner.getLocation(), Sound.ENTITY_WARDEN_HURT, 1.0f, 1.0f);
                    }
                }
            }, 20L * 60 * Main.getPlugin().getConfigValues().TIME_TRADING_REQUEST_SURVIVES);
            return true;
        }
    }

    public void acceptTrade(Player targeted, Player acceptedPlayer) {
        MessageStrings messageStrings = Main.getPlugin().getMessageStrings();
        if(pairs.containsKey(acceptedPlayer.getUniqueId()) && pairs.get(acceptedPlayer.getUniqueId()).equals(targeted)) {
            TradingWindow trade = new TradingWindow(acceptedPlayer, targeted);
            pairs.remove(acceptedPlayer.getUniqueId());
        } else {
            targeted.sendMessage(Main.PREFIX + messageStrings.getTranslation(Translations.PLAYER_DID_NOT_SENT_YOU_A_TRADE_REQUEST));
        }
    }

    public void acceptTrade(Player targetted) {
        MessageStrings messageStrings = Main.getPlugin().getMessageStrings();
        if(pairs.containsValue(targetted)) {
            boolean found = false;
            for(Player v : pairs.values()) {
                if(v.equals(targetted)) {
                    if(!found)
                        found = true;
                    else {
                        targetted.sendMessage(Main.PREFIX + messageStrings.getTranslation(Translations.YOU_GOT_MORE_THAN_ONE_OFFER));
                        return;
                    }
                }
            }
            for(UUID t : pairs.keySet()) {
                if(pairs.get(t).equals(targetted)) {
                    // Found trade offer pair
                    if(Bukkit.getPlayer(t) != null) { // Checking if trading partner is online
                        TradingWindow trade = new TradingWindow(Objects.requireNonNull(Bukkit.getPlayer(t)), targetted);
                    } else {
                        targetted.sendMessage(Main.PREFIX + messageStrings.getTranslation(Translations.PLAYER_WENT_OFFLINE));
                    }
                    pairs.remove(t);
                    return;
                }
            }
        } else {
            targetted.sendMessage(Main.PREFIX + messageStrings.getTranslation(Translations.YOU_GOT_NO_TRADING_OFFER));
        }
    }

    public Player cancelTrade(Player owner) {
        // returns opponent player
        if(pairs.containsKey(owner.getUniqueId())) {
            Player opposite = pairs.get(owner.getUniqueId());
            pairs.remove(owner.getUniqueId());
            return opposite;
        } else {
            return null;
        }
    }

    public void cancelOwnTrade(Player owner) {
        MessageStrings messageStrings = Main.getPlugin().getMessageStrings();
        Player opposite = cancelTrade(owner);
        if(opposite != null) {
            owner.sendMessage(String.format(Main.PREFIX + messageStrings.getTranslation(Translations.YOU_CANCELED_YOUR_TRADE_REQUEST), opposite.getName()));
            opposite.sendMessage(String.format(Main.PREFIX + messageStrings.getTranslation(Translations.OPPONENT_CANCELED_TRADE_OFFER), owner.getName()));
        } else {
            owner.sendMessage(Main.PREFIX + messageStrings.getTranslation(Translations.NO_TRADES_TO_CANCEL));
        }
    }

    public void denyTrade(Player target) {
        MessageStrings messageStrings = Main.getPlugin().getMessageStrings();
        boolean found = false;
        for(UUID key : pairs.keySet()) {
            if(pairs.get(key).equals(target)) {
                if(Bukkit.getPlayer(key) != null) {
                    Bukkit.getPlayer(key).sendMessage(String.format(Main.PREFIX +
                        messageStrings.getTranslation(Translations.OPPONENT_DENIED_TRADE_REQUEST), target.getName()));
                    target.sendMessage(String.format(Main.PREFIX + messageStrings.getTranslation(Translations.YOU_DECLINED_TRADE_REQUEST),
                        Bukkit.getPlayer(key).getName()));
                    found = true;
                }
                pairs.remove(key);
            }
        }
        if(!found)
            target.sendMessage(Main.PREFIX + messageStrings.getTranslation(Translations.GOT_NO_REQUESTS_TO_DENY));
    }

    public void denyTrade(Player target, Player requester) {
        if(pairs.containsKey(requester.getUniqueId())) {
            requester.sendMessage(Main.PREFIX + target.getName() + " denied your trade request!");
            target.sendMessage(Main.PREFIX + "Declined trade request by " +
                target.getName());
            pairs.remove(requester.getUniqueId());
            return;
        } else {
            target.sendMessage(Main.PREFIX + "You got no trade requests to deny!");
        }
    }

    public void addTradingWindow(TradingWindow tw) {
        this.currentDealInvs.add(tw);
    }

    public void removeTradingWindow(TradingWindow tw) {
        currentDealInvs.remove(tw);
    }

    public boolean isInventoryInList(Inventory inv) {
        if(inv != null) {
            for (TradingWindow c : currentDealInvs) {
                if (inv.equals(c.playerInventory) || inv.equals(c.oppositeInventory)) {
                    return true;
                }
            }
        }
        return false;
    }

    public TradingWindow getTradingWindow(Inventory inv) {
        if(inv != null) {
            for (TradingWindow c : currentDealInvs) {
                if (inv.equals(c.playerInventory) || inv.equals(c.oppositeInventory))
                    return c;
            }
        }
        return null;
    }

    public boolean isPlayerCurrentlyDealing(Player p) {
        for(TradingWindow c : currentDealInvs) {
            if(c.player.equals(p) || c.opposite.equals(p)) {
                return true;
            }
        }
        return false;
    }

    public TradingWindow getTradingWindowByPlayer(Player p) {
        for(TradingWindow c : currentDealInvs) {
            if(p.equals(c.player) || p.equals(c.opposite))
                return c;
        }
        return null;
    }

    public ArrayList<TradingWindow> getCurrentDealInvs() {
        return this.currentDealInvs;
    }

    public void closeAllTrades() {
        for(Player p : Bukkit.getOnlinePlayers()) {
            if(this.isPlayerCurrentlyDealing(p)) {
                TradingWindow tw = this.getTradingWindowByPlayer(p);
                tw.closeTrade(p);
            }
        }
    }

    public boolean addPlayerToCooldown(Player p) {
        if(!this.cooldownRightClick.contains(p)) {
            // You can init a new request
            this.cooldownRightClick.add(p);
            Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    cooldownRightClick.remove(p);
                }
            }, 20L);
            return true;
        } else {
            // Player already sent request and needs to cool down
            return false;
        }
    }

    public boolean madePlayerARequest(Player p, Player acceptor) {
        if(this.pairs.containsKey(p.getUniqueId())) {
            if(this.pairs.get(p.getUniqueId()).equals(acceptor))
                return true;
        }
        return false;
    }

    public boolean isDistanceNearEnough(Player p1, Player p2) {
        ConfigValues cv = Main.getPlugin().getConfigValues();
        int maxDistance = cv.MAX_DISTANCE_FOR_USING_TRADE_COMMAND;
        return p1.getLocation().distance(p2.getLocation()) <= maxDistance || maxDistance < 1;
        // Allows disabling max distance feature by putting numbers less 1 in config, e.g. -1 (default)
    }

    public boolean isPlayerBlocked(Player requester, Player requested) {
        UUID requesterID = requester.getUniqueId();
        UUID requestedID = requested.getUniqueId();

        if(blocked.containsKey(requestedID) && blocked.get(requestedID).contains(requesterID)) return true; // check if specific player is blocked

        if(generalBlocks.contains(requestedID)) {
            if(unblocked.containsKey(requestedID)) {
                if (unblocked.get(requestedID).contains(requesterID)) return false; // check if player is whitelisted
                else return true; // player blocks all and does not accept specificly requester by whitelist
            }
            return true;
        }
        return false;
    }

    public void blockAll(Player owner) {
        UUID ownerID = owner.getUniqueId();
        if(blocked.containsKey(ownerID)) {
            blocked.remove(ownerID);
        }
        generalBlocks.add(ownerID);
        owner.sendMessage(Main.PREFIX + Main.getPlugin().getMessageStrings().getTranslation(Translations.BLOCKED_ALL));
    }

    public void unblockAll(Player owner) {
        UUID ownerID = owner.getUniqueId();
        if(generalBlocks.contains(ownerID)) generalBlocks.remove(ownerID);
        if(blocked.containsKey(ownerID)) blocked.remove(ownerID);
        owner.sendMessage(Main.PREFIX + Main.getPlugin().getMessageStrings().getTranslation(Translations.UNBLOCKED_ALL));
    }

    public void addBlock(Player owner, String[] playerNamesToBlock) {
        UUID ownerID = owner.getUniqueId();
        this.prepareBlockSystemListsForPlayerKey(ownerID);

        Player[] toBlock = playerNamesToPlayers(playerNamesToBlock);

        ArrayList<UUID> localBlockPointer = blocked.get(ownerID);
        ArrayList<UUID> localUnblockPointer = unblocked.get(ownerID);

        if(toBlock.length == 0) {
            owner.sendMessage(Main.PREFIX + Main.getPlugin().getMessageStrings().getTranslation(Translations.COULD_NOT_FIND_PLAYER_WITH_THAT_NAME));
            return;
        }

        for(Player current : toBlock) {
            UUID currentID = current.getUniqueId();
            if(!localBlockPointer.contains(currentID)) localBlockPointer.add(currentID);
            if(localUnblockPointer.contains(currentID)) {
                localUnblockPointer.remove(currentID);
            }
        }
        owner.sendMessage(Main.PREFIX + String.format(Main.getPlugin().getMessageStrings().getTranslation(Translations.BLOCKED_PLAYER), playerListToString(toBlock)));
    }

    public void addUnblock(Player owner, String[] playerNamesToUnblock) {
        UUID ownerID = owner.getUniqueId();
        this.prepareBlockSystemListsForPlayerKey(ownerID);

        Player[] toUnblock = playerNamesToPlayers(playerNamesToUnblock);

        ArrayList<UUID> localBlockPointer = blocked.get(ownerID);
        ArrayList<UUID> localUnblockPointer = unblocked.get(ownerID);

        if(toUnblock.length == 0) {
            owner.sendMessage(Main.PREFIX + Main.getPlugin().getMessageStrings().getTranslation(Translations.COULD_NOT_FIND_PLAYER_WITH_THAT_NAME));
            return;
        }

        for(Player current : toUnblock) {
            UUID currentID = current.getUniqueId();
            if(!localUnblockPointer.contains(currentID)) localUnblockPointer.add(currentID);
            if(localBlockPointer.contains(currentID)) localBlockPointer.remove(currentID);
        }
        owner.sendMessage(Main.PREFIX + String.format(Main.getPlugin().getMessageStrings().getTranslation(Translations.UNBLOCKED_PLAYER), playerListToString(toUnblock)));
    }

    private Player[] playerNamesToPlayers(String[] playerNames) {
        LinkedList<Player> result = new LinkedList<Player>();
        for(String current : playerNames) {
            if(Bukkit.getPlayer(current) != null)
                result.add(Bukkit.getPlayer(current));
        }
        return linkedListToPlayerArray(result);
    }

    private String playerListToString(Player[] playerList) {
        StringBuilder result = new StringBuilder();
        for(int i = 0; i < playerList.length; i++) {
            result.append(playerList[i].getName());
            if(i < playerList.length - 1) result.append(", ");
        }
        return result.toString();
    }

    private Player[] linkedListToPlayerArray(LinkedList<Player> list) {
        Player[] result = new Player[list.size()];
        for(int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }

    private void prepareBlockSystemListsForPlayerKey(UUID ownerID) {
        if(!blocked.containsKey(ownerID)) {
            blocked.put(ownerID, new ArrayList<UUID>());
        }

        if(!unblocked.containsKey(ownerID)) {
            unblocked.put(ownerID, new ArrayList<UUID>());
        }
    }
}