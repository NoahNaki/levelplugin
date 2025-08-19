package me.nakilex.levelplugin.guild;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.storage.events.StorageEvents;
import me.nakilex.levelplugin.storage.gui.StorageGUI;
import me.nakilex.levelplugin.utils.CoinInputPrompt;
import me.nakilex.levelplugin.utils.GuiUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Extension of {@link StorageGUI} for guild vaults that adds a coin
 * placeholder item allowing deposit and withdrawal of abstract coins.
 */
public class GuildVaultGUI extends StorageGUI {
    private static final int COIN_SLOT = 4;
    private static final int BACK_SLOT = 0;
    private final String guildName;
    private final GuildMemberGUI memberGUI;

    public GuildVaultGUI(String guildName, StorageEvents events, GuildMemberGUI memberGUI) {
        super(guildName.toLowerCase(), "guildvault", "guild_", "Guild Vault", events, false, 1);
        this.guildName = guildName;
        this.memberGUI = memberGUI;
    }

    @Override
    public void open(Player player) {
        Guild g = GuildManager.getInstance().getGuild(guildName);
        if (g != null) setMaxPages(g.getMaxPages());
        super.open(player);
        Inventory inv = player.getOpenInventory().getTopInventory();
        inv.setItem(BACK_SLOT, GuiUtil.getNexoItem("arrow_left2", ChatColor.GRAY + "Back"));
        inv.setItem(COIN_SLOT, createCoinItem());
    }

    private ItemStack createCoinItem() {
        Guild g = GuildManager.getInstance().getGuild(guildName);
        int coins = g != null ? g.getCoins() : 0;
        int capacity = g != null ? g.getCoinCapacity() : 0;
        ItemStack stack = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Guild Coins");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Guild Coins: " + ChatColor.GOLD + coins + ChatColor.GRAY + "/" + ChatColor.GOLD + capacity + " <glyph:coins_icon>");
            lore.add(ChatColor.WHITE + "Left-click " + ChatColor.GRAY + "to deposit");
            lore.add(ChatColor.WHITE + "Right-click " + ChatColor.GRAY + "to withdraw");
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot == BACK_SLOT) {
            event.setCancelled(true);
            if (memberGUI != null) {
                memberGUI.open((Player) event.getWhoClicked());
            }
            return;
        }
        if (slot == COIN_SLOT) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            Guild g = GuildManager.getInstance().getGuild(guildName);
            if (g == null) return;
            EconomyManager econ = Main.getInstance().getEconomyManager();

            if (event.isLeftClick()) {
                player.closeInventory();
                ConversationFactory factory = new ConversationFactory(Main.getInstance())
                        .withFirstPrompt(new CoinInputPrompt(
                                Main.getInstance(),
                                player,
                                ChatColor.GOLD + "Enter amount to deposit:",
                                amt -> amt > 0 && econ.getBalance(player) >= amt,
                                amt -> {
                                    econ.deductCoins(player, amt);
                                    int before = g.getCoins();
                                    g.addCoins(amt);
                                    int added = g.getCoins() - before;
                                    if (added < amt) {
                                        int refund = amt - added;
                                        econ.addCoins(player, refund);
                                        player.sendMessage(ChatColor.RED + "Vault full. Deposited " + ChatColor.GOLD + added + " <glyph:coins_icon>" + ChatColor.RED + " and refunded " + refund + ".");
                                    } else {
                                        player.sendMessage(ChatColor.GRAY + "Deposited " + ChatColor.GOLD + amt + " <glyph:coins_icon>");
                                    }
                                    GuildManager.getInstance().save();
                                }
                        ))
                        .withLocalEcho(false)
                        .addConversationAbandonedListener(c -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player)));
                factory.buildConversation(player).begin();
            } else if (event.isRightClick()) {
                player.closeInventory();
                ConversationFactory factory = new ConversationFactory(Main.getInstance())
                        .withFirstPrompt(new CoinInputPrompt(
                                Main.getInstance(),
                                player,
                                ChatColor.GOLD + "Enter amount to withdraw:",
                                amt -> amt > 0 && g.getCoins() >= amt,
                                amt -> {
                                    g.removeCoins(amt);
                                    econ.addCoins(player, amt);
                                    player.sendMessage(ChatColor.GRAY + "Withdrew " + ChatColor.GOLD + amt + " <glyph:coins_icon>");
                                    GuildManager.getInstance().save();
                                }
                        ))
                        .withLocalEcho(false)
                        .addConversationAbandonedListener(c -> Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(player)));
                factory.buildConversation(player).begin();
            }
            return;
        }
        super.handleClick(event);
    }

    @Override
    public void saveToDisk() {
        for (Inventory page : getPages()) {
            page.setItem(COIN_SLOT, null);
            page.setItem(BACK_SLOT, null);
        }
        super.saveToDisk();
    }
}
