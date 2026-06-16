package me.nakilex.levelplugin.player.attributes.managers;

import me.nakilex.levelplugin.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class ActionBarTask extends BukkitRunnable {
    private final Main plugin;
    private final Set<UUID> playersWithActionBarStatus = new HashSet<>();

    public ActionBarTask(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        clearOfflinePlayers();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getCutsceneManager().isInCutscene(player)) {
                playersWithActionBarStatus.remove(player.getUniqueId());
                continue;
            }
            if (plugin.getQuestDialogueManager() != null && plugin.getQuestDialogueManager().hasSession(player)) {
                playersWithActionBarStatus.remove(player.getUniqueId());
                continue;
            }

            StatsManager statsManager = StatsManager.getInstance();
            CooldownIndicatorManager.Info info = CooldownIndicatorManager.getInstance().get(player);
            String cooldownMessage = cooldownMessage(info, now);
            String statusMessage = statusMessage(player, statsManager);
            String resourceMessage = resourceMessage(player, statsManager);
            String cookingMessage = cookingMessage(player);

            String message = joinSegments(cooldownMessage, statusMessage, resourceMessage, cookingMessage);
            if (message.isEmpty()) {
                clearActionBarStatus(player);
            } else {
                player.sendActionBar(Component.text(message));
                playersWithActionBarStatus.add(player.getUniqueId());
            }
        }
    }

    private String cookingMessage(Player player) {
        if (plugin.getCookingModule() == null || player == null) {
            return "";
        }
        return plugin.getCookingModule().activeSessions().getByPlayer(player.getUniqueId()).isPresent()
                ? ChatColor.YELLOW + "Sneak" + ChatColor.GRAY + " to cancel cooking"
                : "";
    }

    private String cooldownMessage(CooldownIndicatorManager.Info info, long now) {
        if (info == null) {
            return "";
        }
        boolean showCd = now < info.expireAt && now < info.costExpireAt;
        boolean showCost = info.cost > 0 && now < info.costExpireAt;
        if (!showCd && !showCost) {
            return "";
        }

        StringBuilder msg = new StringBuilder();
        if (showCd) {
            long remaining = info.expireAt - now;
            int seconds = (int) Math.ceil(remaining / 1000.0);
            msg.append(ChatColor.YELLOW).append(info.name)
                    .append(ChatColor.GRAY).append(" cooldown ")
                    .append(ChatColor.YELLOW).append(seconds).append("s");
        }
        if (showCost) {
            if (!showCd) {
                msg.append(ChatColor.YELLOW).append(info.name);
            }
            msg.append(" ")
                    .append(ChatColor.DARK_GRAY).append("[")
                    .append(ChatColor.GRAY).append("-")
                    .append(ChatColor.GRAY).append(info.cost)
                    .append(ChatColor.DARK_GRAY).append("]");
        }
        return msg.toString();
    }

    private String statusMessage(Player player, StatsManager statsManager) {
        if (statsManager.isInCombat(player.getUniqueId())) {
            return ChatColor.RED + "In Combat";
        }
        if (!statsManager.hasMissingHealthOrMana(player)) {
            return "";
        }
        String consistency = me.nakilex.levelplugin.player.farming.managers.FarmingManager.getInstance()
                .getConsistencyIndicator(player);
        return consistency == null ? "" : consistency;
    }

    private String resourceMessage(Player player, StatsManager statsManager) {
        String health = healthSegment(player, statsManager);
        String mana = manaSegment(player, statsManager);
        return joinSegments(health, mana);
    }

    private String healthSegment(Player player, StatsManager statsManager) {
        if (!statsManager.isHealthBelowMax(player)) {
            return "";
        }
        double maxHealth = statsManager.getMaxHealth(player);
        return ChatColor.RED + "HP " + ChatColor.WHITE + (int) Math.ceil(player.getHealth())
                + ChatColor.GRAY + "/" + ChatColor.WHITE + (int) Math.ceil(maxHealth);
    }

    private String manaSegment(Player player, StatsManager statsManager) {
        if (!statsManager.isManaBelowMax(player)) {
            return "";
        }
        StatsManager.PlayerStats stats = statsManager.getPlayerStats(player.getUniqueId());
        return ChatColor.AQUA + "Mana " + ChatColor.WHITE + stats.currentMana
                + ChatColor.GRAY + "/" + ChatColor.WHITE + stats.maxMana;
    }

    private String joinSegments(String... segments) {
        StringBuilder joined = new StringBuilder();
        for (String segment : segments) {
            if (segment == null || segment.isEmpty()) {
                continue;
            }
            if (joined.length() > 0) {
                joined.append(ChatColor.DARK_GRAY).append(" | ");
            }
            joined.append(segment);
        }
        return joined.toString();
    }

    private void clearActionBarStatus(Player player) {
        if (playersWithActionBarStatus.remove(player.getUniqueId())) {
            player.sendActionBar(Component.empty());
        }
    }

    private void clearOfflinePlayers() {
        Iterator<UUID> iterator = playersWithActionBarStatus.iterator();
        while (iterator.hasNext()) {
            UUID uuid = iterator.next();
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                iterator.remove();
            }
        }
    }
}
