package me.nakilex.levelplugin.mail;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
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
    private static final MailCommand INSTANCE = new MailCommand();

    private MailCommand() {}

    public static MailCommand getInstance() {
        return INSTANCE;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        openInbox(player);
        return true;
    }

    private void openInbox(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        GuiUtil.fillBorder(inv, GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE));
        List<MailManager.MailEntry> inbox = MailManager.getInstance().getInbox(player.getUniqueId());
        int slot = 10;
        for (MailManager.MailEntry mail : inbox) {
            if (slot > 43) break;
            if (slot % 9 == 8) slot += 2;
            ItemStack paper = new ItemStack(mail.claimed() ? Material.MAP : Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((mail.claimed() ? ChatColor.GRAY : ChatColor.GOLD) + mail.subject());
                List<String> lore = new ArrayList<>();
                lore.addAll(TooltipUtil.bulletList("From: " + ChatColor.WHITE + MailManager.getInstance().senderName(mail.sender())));
                lore.addAll(TooltipUtil.wrapLoreLine(ChatColor.GRAY + mail.body(), 150));
                lore.add(" ");
                lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Rewards"));
                lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "" + mail.coins() + " <glyph:coins_icon>"));
                lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "" + mail.gems() + " <glyph:purple_orb_icon>"));
                lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "" + mail.xp() + " XP"));
                lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "" + mail.items().size() + " Items"));
                lore.add(" ");
                lore.addAll(TooltipUtil.clickInstructions(mail.claimed() ? null : "to claim rewards", null));
                if (mail.claimed()) lore.add(ChatColor.DARK_GRAY + "Already claimed.");
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
        if (!GuiUtil.titleMatches(event.getView().getTitle(), TITLE)) return;
        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();
        if (current == null || !current.hasItemMeta() || current.getItemMeta().getLore() == null) return;
        String token = current.getItemMeta().getLore().stream().filter(line -> line.startsWith(ChatColor.DARK_GRAY + "mail:")).findFirst().orElse(null);
        if (token == null) return;
        String id = token.substring((ChatColor.DARK_GRAY + "mail:").length());
        MailManager.getInstance().markRead(player.getUniqueId(), id);
        MailManager.ClaimResult result = MailManager.getInstance().claimWithResult(player, id);
        if (result != null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Quest-style reward claim:");
            player.sendMessage(ChatColor.GREEN + "- " + ChatColor.GRAY + result.coins() + " <glyph:coins_icon>");
            player.sendMessage(ChatColor.GREEN + "- " + ChatColor.GRAY + result.gems() + " <glyph:purple_orb_icon>");
            player.sendMessage(ChatColor.GREEN + "- " + ChatColor.GRAY + result.xp() + " XP");
            player.sendMessage(ChatColor.GREEN + "- " + ChatColor.GRAY + result.itemCount() + " items");
            openInbox(player);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
