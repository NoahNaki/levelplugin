package me.nakilex.levelplugin.mail;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.player.level.managers.LevelManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class MailManager {
    private static final MailManager INSTANCE = new MailManager();
    public static MailManager getInstance() { return INSTANCE; }
    private MailManager() {}

    public record MailEntry(String id, UUID sender, String subject, String body, long createdAt,
                            int coins, int gems, int xp, List<ItemStack> items, boolean claimed, boolean read) {}
    public record ClaimResult(int coins, int gems, int xp, int itemCount, List<ItemStack> items) {}

    public List<MailEntry> getInbox(UUID playerId) {
        var cfg = Main.getInstance().getPlayerConfig().getConfig();
        String base = "players." + playerId + ".mail.inbox";
        List<MailEntry> out = new ArrayList<>();
        var section = cfg.getConfigurationSection(base);
        if (section == null) return out;
        for (String id : section.getKeys(false)) {
            String p = base + "." + id + ".";
            out.add(new MailEntry(
                    id,
                    parseUuid(cfg.getString(p + "sender")),
                    cfg.getString(p + "subject", "Mail"),
                    cfg.getString(p + "body", ""),
                    cfg.getLong(p + "createdAt", System.currentTimeMillis()),
                    cfg.getInt(p + "coins", 0),
                    cfg.getInt(p + "gems", 0),
                    cfg.getInt(p + "xp", 0),
                    new ArrayList<>(cfg.getList(p + "items", List.of()).stream().filter(ItemStack.class::isInstance).map(ItemStack.class::cast).toList()),
                    cfg.getBoolean(p + "claimed", false),
                    cfg.getBoolean(p + "read", false)
            ));
        }
        out.sort(Comparator.comparingLong(MailEntry::createdAt).reversed());
        return out;
    }

    public int getUnreadCount(UUID playerId) {
        return (int) getInbox(playerId).stream().filter(m -> !m.read()).count();
    }

    public void sendToPlayer(UUID target, UUID sender, String subject, String body, int coins, int gems, int xp, List<ItemStack> items) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String p = "players." + target + ".mail.inbox." + id + ".";
        var cfg = Main.getInstance().getPlayerConfig().getConfig();
        cfg.set(p + "sender", sender == null ? "system" : sender.toString());
        cfg.set(p + "subject", subject);
        cfg.set(p + "body", body);
        cfg.set(p + "createdAt", System.currentTimeMillis());
        cfg.set(p + "coins", Math.max(0, coins));
        cfg.set(p + "gems", Math.max(0, gems));
        cfg.set(p + "xp", Math.max(0, xp));
        cfg.set(p + "items", items == null ? List.of() : items);
        cfg.set(p + "claimed", false);
        cfg.set(p + "read", false);
        Main.getInstance().getPlayerConfig().saveConfigFile();
    }

    public void sendToAllOnline(UUID sender, String subject, String body, int coins, int gems, int xp, List<ItemStack> items) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendToPlayer(p.getUniqueId(), sender, subject, body, coins, gems, xp, items);
        }
    }

    public int sendToAllKnown(UUID sender, String subject, String body, int coins, int gems, int xp, List<ItemStack> items) {
        int sent = 0;
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player == null || player.getUniqueId() == null) continue;
            sendToPlayer(player.getUniqueId(), sender, subject, body, coins, gems, xp, items);
            sent++;
        }
        return sent;
    }

    public void markRead(UUID playerId, String mailId) {
        String p = "players." + playerId + ".mail.inbox." + mailId + ".read";
        Main.getInstance().getPlayerConfig().getConfig().set(p, true);
        Main.getInstance().getPlayerConfig().saveConfigFile();
    }

    public boolean claim(Player player, String mailId) {
        return claimWithResult(player, mailId) != null;
    }

    public ClaimResult claimWithResult(Player player, String mailId) {
        UUID playerId = player.getUniqueId();
        MailEntry mail = getInbox(playerId).stream().filter(m -> m.id().equals(mailId)).findFirst().orElse(null);
        if (mail == null || mail.claimed()) return null;
        if (!mail.items().isEmpty() && player.getInventory().firstEmpty() == -1) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Not enough inventory space to claim this mail.");
            return null;
        }
        if (mail.coins() > 0 && Main.getInstance().getEconomyManager() != null) {
            Main.getInstance().getEconomyManager().addCoins(player, mail.coins());
        }
        if (mail.gems() > 0 && Main.getInstance().getGemsManager() != null) {
            Main.getInstance().getGemsManager().addUnits(player, mail.gems());
        }
        if (mail.xp() > 0) {
            LevelManager.getInstance().addXP(player, mail.xp());
        }
        for (ItemStack item : mail.items()) {
            if (item != null) player.getInventory().addItem(item.clone());
        }
        String base = "players." + playerId + ".mail.inbox." + mailId + ".";
        var cfg = Main.getInstance().getPlayerConfig().getConfig();
        cfg.set(base + "claimed", true);
        cfg.set(base + "read", true);
        Main.getInstance().getPlayerConfig().saveConfigFile();
        return new ClaimResult(mail.coins(), mail.gems(), mail.xp(), mail.items().size(),
                new ArrayList<>(mail.items()));
    }

    public String senderName(UUID sender) {
        if (sender == null) return "System";
        OfflinePlayer op = Bukkit.getOfflinePlayer(sender);
        return op.getName() == null ? "Unknown" : op.getName();
    }

    private UUID parseUuid(String raw) {
        try { return raw == null || raw.equalsIgnoreCase("system") ? null : UUID.fromString(raw); }
        catch (Exception ignored) { return null; }
    }
}
