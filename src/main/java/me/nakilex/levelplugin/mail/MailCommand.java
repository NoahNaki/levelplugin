package me.nakilex.levelplugin.mail;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class MailCommand implements CommandExecutor, TabCompleter, Listener {
    private static final String TITLE = "Mailbox";
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        openInbox(player);
        return true;
    }

    private void openInbox(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        List<MailManager.MailEntry> inbox = MailManager.getInstance().getInbox(player.getUniqueId());
        int slot = 0;
        for (MailManager.MailEntry mail : inbox) {
            if (slot >= 45) break;
            ItemStack paper = new ItemStack(mail.claimed() ? Material.MAP : Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((mail.claimed() ? ChatColor.GRAY : ChatColor.GOLD) + mail.subject());
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.DARK_GRAY + "From: " + ChatColor.GRAY + MailManager.getInstance().senderName(mail.sender()));
                lore.add(ChatColor.GRAY + mail.body());
                lore.add(ChatColor.YELLOW + "Coins: " + ChatColor.GOLD + mail.coins());
                lore.add(ChatColor.LIGHT_PURPLE + "Gems: " + ChatColor.WHITE + mail.gems());
                lore.add(ChatColor.AQUA + "XP: " + ChatColor.WHITE + mail.xp());
                lore.add(ChatColor.GRAY + "Items: " + ChatColor.WHITE + mail.items().size());
                lore.add("");
                lore.add(mail.claimed() ? ChatColor.DARK_GRAY + "Already claimed." : ChatColor.GREEN + "Click to claim.");
                lore.add(ChatColor.DARK_GRAY + "mail:" + mail.id());
                meta.setLore(lore);
                paper.setItemMeta(meta);
            }
            inv.setItem(slot++, paper);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();
        if (current == null || !current.hasItemMeta() || current.getItemMeta().getLore() == null) return;
        String token = current.getItemMeta().getLore().stream().filter(line -> line.startsWith(ChatColor.DARK_GRAY + "mail:")).findFirst().orElse(null);
        if (token == null) return;
        String id = token.substring((ChatColor.DARK_GRAY + "mail:").length());
        MailManager.getInstance().markRead(player.getUniqueId(), id);
        if (MailManager.getInstance().claim(player, id)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Mail claimed.");
            openInbox(player);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
