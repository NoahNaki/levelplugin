package me.nakilex.levelplugin.player.fishing.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.player.fishing.config.FishingRewardsConfig;
import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.player.fishing.utils.FishingItemUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class FishingListener implements Listener {

    private static final long BITE_WINDOW_MS = 2000L;

    private final Main plugin;
    private final FishingRewardsConfig rewardsConfig;
    private final FishingManager fishingManager;
    private final Map<UUID, FishingSession> sessions = new HashMap<>();
    private final Map<UUID, Boolean> lastResult = new HashMap<>();
    private final Random random = new Random();

    public FishingListener(Main plugin, FishingRewardsConfig rewardsConfig, FishingManager fishingManager) {
        this.plugin = plugin;
        this.rewardsConfig = rewardsConfig;
        this.fishingManager = fishingManager;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        switch (event.getState()) {
            case BITE -> startSession(player, uuid);
            case REEL_IN -> handleReel(player, uuid);
            case CAUGHT_FISH -> handleCatch(event, player, uuid);
            default -> {
            }
        }
    }

    private void startSession(Player player, UUID uuid) {
        clearSession(uuid);
        BossBar bar = Bukkit.createBossBar("Reel in!", BarColor.BLUE, BarStyle.SOLID);
        bar.addPlayer(player);
        bar.setVisible(true);
        long windowMs = computeWindowMs(player);
        FishingSession session = new FishingSession(bar, System.currentTimeMillis() + windowMs, windowMs);
        sessions.put(uuid, session);
        session.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long remaining = session.expiresAtMs - System.currentTimeMillis();
            double progress = Math.max(0.0, Math.min(1.0, remaining / (double) session.windowMs));
            bar.setProgress(progress);
            if (remaining <= 0) {
                clearSession(uuid);
            }
        }, 0L, 1L);
    }

    private void handleReel(Player player, UUID uuid) {
        FishingSession session = sessions.get(uuid);
        if (session == null) {
            lastResult.put(uuid, false);
            return;
        }
        boolean success = System.currentTimeMillis() <= session.expiresAtMs;
        lastResult.put(uuid, success);
        clearSession(uuid);
        if (success) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.2f);
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.9f, 0.8f);
        }
    }

    private void handleCatch(PlayerFishEvent event, Player player, UUID uuid) {
        boolean success = resolveSuccess(uuid);
        if (!success) {
            event.setCancelled(true);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.9f, 0.7f);
            return;
        }

        ItemStack rod = resolveRod(player);
        ToolTier tier = resolveTier(rod);
        boolean highestTier = tier != null && tier.isHighestTier();
        boolean inLava = event.getHook() != null
                && event.getHook().getLocation().getBlock().getType() == Material.LAVA;

        FishDefinition definition = rewardsConfig.rollFish(
                fishingManager.getLevel(player),
                inLava,
                highestTier,
                random);
        double size = rollSize(definition);
        ItemStack fishItem = FishingItemUtil.createFishItem(definition, size);

        if (event.getCaught() instanceof Item item) {
            item.setItemStack(fishItem);
        } else {
            player.getInventory().addItem(fishItem);
        }
        fishingManager.addXP(player, definition.xpReward());
        fishingManager.discoverFish(player.getUniqueId(), definition.id());
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().handleCaptureFish(player, definition.id());
        }
        String sizeLabel = String.format("%.1f cm", size);
        String expColor = ChatFormatter.experienceColor();
        String message = ChatColor.GRAY + "You caught a " + ChatColor.WHITE + sizeLabel + " "
                + definition.displayName() + ChatColor.GRAY + " and earned "
                + expColor + "+" + definition.xpReward() + ChatColor.GRAY
                + " <glyph:experience_orb_icon> Fishing EXP" + ChatColor.GRAY + ".";
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, message);
    }

    private ItemStack resolveRod(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && main.getType() == Material.FISHING_ROD) {
            return main;
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off != null && off.getType() == Material.FISHING_ROD) {
            return off;
        }
        return main;
    }

    private ToolTier resolveTier(ItemStack rod) {
        if (rod == null) return null;
        CustomTool tool = ToolManager.getInstance().getTool(rod);
        return tool != null ? tool.getTier() : ToolTier.fromMaterial(rod.getType());
    }

    private long computeWindowMs(Player player) {
        ItemStack rod = resolveRod(player);
        ToolTier tier = resolveTier(rod);
        double speedMultiplier = tier != null ? tier.getFishingSpeed() : 1.0;
        if (player.getWorld().hasStorm()) {
            speedMultiplier *= 2.0;
        }
        long window = Math.round(BITE_WINDOW_MS * speedMultiplier);
        return Math.max(500L, window);
    }

    private boolean resolveSuccess(UUID uuid) {
        Boolean stored = lastResult.remove(uuid);
        if (stored != null) {
            return stored;
        }
        FishingSession session = sessions.get(uuid);
        if (session == null) {
            return false;
        }
        boolean success = System.currentTimeMillis() <= session.expiresAtMs;
        clearSession(uuid);
        return success;
    }

    private double rollSize(FishDefinition definition) {
        int min = definition.minSize();
        int max = Math.max(min, definition.maxSize());
        double size = min + (random.nextDouble() * (max - min));
        return Math.round(size * 10.0) / 10.0;
    }

    private void clearSession(UUID uuid) {
        FishingSession session = sessions.remove(uuid);
        if (session == null) return;
        if (session.task != null) {
            session.task.cancel();
        }
        session.bar.removeAll();
    }

    private static class FishingSession {
        private final BossBar bar;
        private final long expiresAtMs;
        private final long windowMs;
        private org.bukkit.scheduler.BukkitTask task;

        private FishingSession(BossBar bar, long expiresAtMs, long windowMs) {
            this.bar = bar;
            this.expiresAtMs = expiresAtMs;
            this.windowMs = windowMs;
        }
    }
}
