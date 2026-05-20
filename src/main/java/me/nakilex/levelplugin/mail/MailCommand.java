package me.nakilex.levelplugin.mail;

import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
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
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.items.utils.ItemUtil;

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
                lore.add(TooltipUtil.bulletLine(ChatColor.WHITE + "" + mail.coins() + " " + ChatColor.GOLD + "<glyph:coins_icon>"));
                lore.add(TooltipUtil.bulletLine(ChatColor.WHITE + "" + mail.gems() + " " + ChatColor.LIGHT_PURPLE + "<glyph:purple_orb_icon>"));
                lore.add(TooltipUtil.bulletLine(ChatColor.WHITE + "" + mail.xp() + ChatColor.GRAY + " XP"));
                lore.add(TooltipUtil.bulletLine(ChatColor.WHITE + "" + mail.items().size() + ChatColor.GRAY + " Items"));
                if (!mail.items().isEmpty()) {
                    lore.add(" ");
                    lore.add(TooltipUtil.bulletLine(ChatColor.GRAY + "Attachments"));
                    int maxLines = Math.min(4, mail.items().size());
                    for (int i = 0; i < maxLines; i++) {
                        ItemStack attachment = mail.items().get(i);
                        lore.add(TooltipUtil.bulletLine(formatAttachmentLine(attachment)));
                    }
                    if (mail.items().size() > maxLines) {
                        lore.add(TooltipUtil.bulletLine(ChatColor.DARK_GRAY + "+" + String.valueOf(mail.items().size() - maxLines) + " more"));
                    }
                }
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
            sendClaimRewardMessage(player, current.getItemMeta().getDisplayName(), result);
            openInbox(player);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }

    private String formatAttachmentLine(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return ChatColor.GRAY + "Unknown Item";
        }
        int amount = Math.max(1, stack.getAmount());
        String amountText = ChatColor.WHITE + String.valueOf(amount) + "x ";

        String displayName = resolveItemDisplayName(stack);
        ItemRarity rarity = ItemUtil.getCustomItemRarity(stack);
        if (rarity != null && ItemUtil.isWeaponOrArmor(stack)) {
            return amountText + rarity.getColor() + ChatColor.stripColor(displayName);
        }
        return amountText + ChatColor.GRAY + ChatColor.stripColor(displayName);
    }

    private String resolveItemDisplayName(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName() && meta.getDisplayName() != null && !meta.getDisplayName().isBlank()) {
            return meta.getDisplayName();
        }
        String friendly = stack.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = friendly.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private void sendClaimRewardMessage(Player player, String subject, MailManager.ClaimResult result) {
        ChatFormatter.constructDivider(player, "", 45);
        ChatFormatter.sendCenteredMessage(player, ChatColor.GOLD + "" + ChatColor.BOLD + "Mail Claimed!");
        ChatFormatter.sendCenteredMessage(player, ChatColor.YELLOW + ChatColor.stripColor(subject));
        ChatFormatter.constructDivider(player, " ", 45);
        ChatFormatter.sendIndentedMessage(player, ChatColor.GREEN + "Rewards:");
        if (result.coins() > 0) {
            ChatFormatter.sendIndentedMessage(player,
                    ChatColor.GREEN + "- " + ChatColor.WHITE + result.coins() + " " + ChatColor.GOLD + "<glyph:coins_icon>");
        }
        if (result.gems() > 0) {
            ChatFormatter.sendIndentedMessage(player,
                    ChatColor.GREEN + "- " + ChatColor.WHITE + result.gems() + " " + ChatColor.LIGHT_PURPLE + "<glyph:purple_orb_icon>");
        }
        if (result.xp() > 0) {
            String expLabel = ChatFormatter.experienceLabel();
            String expColor = ChatFormatter.experienceColor();
            ChatFormatter.sendIndentedMessage(player,
                    ChatColor.GREEN + "- " + expColor + result.xp() + ChatColor.RESET + " <glyph:experience_orb_icon> " + expLabel);
        }
        for (ItemStack item : result.items()) {
            ChatFormatter.sendIndentedMessage(player, ChatColor.GREEN + "- " + formatAttachmentLine(item));
        }
        ChatFormatter.constructDivider(player, " ", 45);
    }
}
