package me.nakilex.levelplugin.mail;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.GuiUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class MailAdminCommand implements CommandExecutor, TabCompleter, Listener {
    private static final String TITLE = "Mail Admin";
    private static final String ATTACHMENTS_TITLE = "Mail Attachments";
    private static final MailAdminCommand INSTANCE = new MailAdminCommand();
    private final Map<UUID, Draft> drafts = new HashMap<>();
    private record Draft(String target, int coins, int gems, int xp, String subject, List<ItemStack> items) {
        Draft with(String t, Integer c, Integer g, Integer x, String s, List<ItemStack> i) {
            return new Draft(t == null ? target : t, c == null ? coins : c, g == null ? gems : g, x == null ? xp : x,
                    s == null ? subject : s, i == null ? items : i);
        }
    }

    private MailAdminCommand() {}

    public static MailAdminCommand getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        drafts.putIfAbsent(player.getUniqueId(), new Draft("all", 0, 0, 0, "Admin Mail", new ArrayList<>()));
        open(player);
        return true;
    }

    private void open(Player player) {
        Draft d = drafts.get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        GuiUtil.fillBorder(inv, GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE));
        inv.setItem(10, GuiUtil.createGuiItem(Material.NAME_TAG, ChatColor.YELLOW + "Target: " + d.target(),
                TooltipUtil.bulletList("Current: " + ChatColor.WHITE + d.target(), "Set target player or all")));
        inv.setItem(12, GuiUtil.createGuiItem(Material.GOLD_INGOT, ChatColor.GOLD + "Coins: " + d.coins(),
                TooltipUtil.bulletList("Reward amount", "Shown as <glyph:coins_icon> on claim")));
        inv.setItem(14, GuiUtil.createGuiItem(Material.AMETHYST_SHARD, ChatColor.LIGHT_PURPLE + "Gems: " + d.gems(),
                TooltipUtil.bulletList("Reward amount", "Shown as <glyph:purple_orb_icon> on claim")));
        inv.setItem(16, GuiUtil.createGuiItem(Material.EXPERIENCE_BOTTLE, ChatColor.AQUA + "XP: " + d.xp(),
                TooltipUtil.bulletList("Reward amount")));
        inv.setItem(30, GuiUtil.createGuiItem(Material.WRITABLE_BOOK, ChatColor.GOLD + "Subject",
                TooltipUtil.bulletList("Current: " + ChatColor.WHITE + d.subject(), "Rename this mail")));
        inv.setItem(28, GuiUtil.createGuiItem(Material.CHEST, ChatColor.GREEN + "Items",
                TooltipUtil.bulletList("Current attachments: " + ChatColor.WHITE + d.items().size(), "Edit attached items")));
        inv.setItem(49, GuiUtil.getNexoItem("check", ChatColor.GREEN + "Send Mail", TooltipUtil.clickInstructions("to send", null)));
        for (int i = 0; i < Math.min(7, d.items().size()); i++) inv.setItem(36 + i, d.items().get(i));
        player.openInventory(inv);
    }

    private void openAttachments(Player player) {
        Draft d = drafts.get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, ATTACHMENTS_TITLE);
        for (int i = 0; i < Math.min(45, d.items().size()); i++) {
            inv.setItem(i, d.items().get(i));
        }
        inv.setItem(49, GuiUtil.getNexoItem("arrow_left2", ChatColor.YELLOW + "Back", TooltipUtil.clickInstructions("to return to admin mail", null)));
        player.openInventory(inv);
    }

    @EventHandler public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();
        if (GuiUtil.titleMatches(title, ATTACHMENTS_TITLE)) {
            handleAttachmentsClick(e, p);
            return;
        }
        if (!GuiUtil.titleMatches(title, TITLE)) return;
        int slot = e.getRawSlot();
        if (slot >= 36 && slot <= 42) { return; } // allow editing attachments
        e.setCancelled(true);
        Draft d = drafts.get(p.getUniqueId());
        if (d == null) return;
        switch (slot) {
            case 10 -> prompt(p, "Enter target player name or 'all':", s -> drafts.put(p.getUniqueId(), d.with(s.trim(), null, null, null, null, null)));
            case 12 -> prompt(p, "Enter coin amount:", s -> drafts.put(p.getUniqueId(), d.with(null, parse(s), null, null, null, null)));
            case 14 -> prompt(p, "Enter gem amount:", s -> drafts.put(p.getUniqueId(), d.with(null, null, parse(s), null, null, null)));
            case 16 -> prompt(p, "Enter xp amount:", s -> drafts.put(p.getUniqueId(), d.with(null, null, null, parse(s), null, null)));
            case 30 -> prompt(p, "Enter subject:", s -> drafts.put(p.getUniqueId(), d.with(null, null, null, null, sanitizeSubject(s), null)));
            case 28 -> openAttachments(p);
            case 49 -> send(p);
            default -> {}
        }
    }

    private void handleAttachmentsClick(InventoryClickEvent event, Player player) {
        int rawSlot = event.getRawSlot();
        if (rawSlot == 49) {
            event.setCancelled(true);
            Inventory inv = event.getView().getTopInventory();
            List<ItemStack> attachments = new ArrayList<>();
            for (int i = 0; i < 45; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && !item.getType().isAir()) attachments.add(item.clone());
            }
            Draft current = drafts.get(player.getUniqueId());
            if (current != null) drafts.put(player.getUniqueId(), current.with(null, null, null, null, null, attachments));
            open(player);
            return;
        }
        if (rawSlot >= 0 && rawSlot < event.getView().getTopInventory().getSize()) {
            event.setCancelled(rawSlot >= 45);
        }
    }

    private void send(Player admin) {
        Inventory top = admin.getOpenInventory().getTopInventory();
        List<ItemStack> attachments = new ArrayList<>();
        for (int i = 36; i <= 42; i++) {
            ItemStack it = top.getItem(i);
            if (it != null && !it.getType().isAir()) attachments.add(it.clone());
        }
        Draft d = drafts.get(admin.getUniqueId()).with(null, null, null, null, null, attachments);
        if ("all".equalsIgnoreCase(d.target())) {
            int count = MailManager.getInstance().sendToAllKnown(admin.getUniqueId(), d.subject(), "Administrative mail.", d.coins(), d.gems(), d.xp(), attachments);
            ChatMessageUtil.send(admin, ChatMessageUtil.MessageType.SUCCESS, "Sent to " + count + " players.");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(d.target());
        if (target == null || target.getUniqueId() == null) {
            ChatMessageUtil.send(admin, ChatMessageUtil.MessageType.ERROR, "Unknown target.");
            return;
        }
        MailManager.getInstance().sendToPlayer(target.getUniqueId(), admin.getUniqueId(), d.subject(), "Administrative mail.", d.coins(), d.gems(), d.xp(), attachments);
        ChatMessageUtil.send(admin, ChatMessageUtil.MessageType.SUCCESS, "Mail sent to " + d.target() + ".");
    }

    private void prompt(Player p, String text, java.util.function.Consumer<String> done) {
        p.closeInventory();
        new ConversationFactory(Main.getInstance()).withFirstPrompt(new StringPrompt() {
            @Override public String getPromptText(org.bukkit.conversations.ConversationContext context) { return ChatColor.YELLOW + text; }
            @Override public org.bukkit.conversations.Prompt acceptInput(org.bukkit.conversations.ConversationContext context, String input) { done.accept(input == null ? "" : input); Bukkit.getScheduler().runTask(Main.getInstance(), () -> open(p)); return END_OF_CONVERSATION; }
        }).withLocalEcho(false).buildConversation(p).begin();
    }
    private int parse(String s) { try { return Math.max(0, Integer.parseInt(s.trim())); } catch (Exception ignored) { return 0; } }
    private String sanitizeSubject(String subject) {
        if (subject == null) return "Admin Mail";
        String trimmed = subject.trim();
        if (trimmed.isEmpty()) return "Admin Mail";
        return trimmed.length() > 48 ? trimmed.substring(0, 48) : trimmed;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) { return List.of(); }
}
