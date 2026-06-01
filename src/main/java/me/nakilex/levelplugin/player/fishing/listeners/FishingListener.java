package me.nakilex.levelplugin.player.fishing.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.player.fishing.config.FishingRewardsConfig;
import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.player.fishing.minigame.AccurateClickFishingMinigame;
import me.nakilex.levelplugin.player.fishing.minigame.FishingMinigame;
import me.nakilex.levelplugin.player.fishing.minigame.FishingMinigameContext;
import me.nakilex.levelplugin.player.fishing.minigame.FishingMinigameRegistry;
import me.nakilex.levelplugin.player.fishing.minigame.ReelWindowFishingMinigame;
import me.nakilex.levelplugin.player.fishing.utils.FishingItemUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FishingListener implements Listener {

    private static final long BITE_WINDOW_MS = 2000L;
    private static final int LAVA_BITE_MIN_TICKS = 20;
    private static final int LAVA_BITE_MAX_TICKS = 50;

    private final Main plugin;
    private final FishingRewardsConfig rewardsConfig;
    private final FishingManager fishingManager;
    private final Map<UUID, FishingSession> sessions = new HashMap<>();
    private final Map<UUID, Boolean> lastResult = new HashMap<>();
    private final Map<UUID, Boolean> lastRewarded = new HashMap<>();
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> lavaBiteTasks = new HashMap<>();
    private final Random random = new Random();
    private final FishingMinigameRegistry minigames = new FishingMinigameRegistry();

    public FishingListener(Main plugin, FishingRewardsConfig rewardsConfig, FishingManager fishingManager) {
        this.plugin = plugin;
        this.rewardsConfig = rewardsConfig;
        this.fishingManager = fishingManager;
        registerMinigames();
    }

    private void registerMinigames() {
        minigames.register(ReelWindowFishingMinigame::new,
                rewardsConfig.getConfig().getDouble("minigames.simple_reel.weight", 1.0));
        minigames.register(context -> new AccurateClickFishingMinigame(new FishingMinigameContext(
                        context.player(), Math.max(4_000L, context.durationMs() * 3L))),
                rewardsConfig.getConfig().getDouble("minigames.accurate_click.weight", 1.0));
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        switch (event.getState()) {
            case FISHING -> handleCast(player, uuid, event.getHook() != null ? event.getHook().getLocation() : null);
            case BITE -> startSession(player, uuid,
                    event.getHook() != null ? event.getHook().getLocation() : null,
                    isLavaFishingArea(player, event.getHook() != null ? event.getHook().getLocation() : null),
                    false);
            case REEL_IN -> handleReel(player, uuid);
            case CAUGHT_FISH -> handleCatch(event, player, uuid);
            default -> {
            }
        }
    }

    private void handleCast(Player player, UUID uuid, org.bukkit.Location hookLocation) {
        clearLavaTask(uuid);
        lastResult.remove(uuid);
        lastRewarded.remove(uuid);
        boolean inLava = isLavaFishingArea(player, hookLocation);
        if (!inLava) {
            return;
        }
        int delay = ThreadLocalRandom.current().nextInt(LAVA_BITE_MIN_TICKS, LAVA_BITE_MAX_TICKS + 1);
        lavaBiteTasks.put(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            startSession(player, uuid, hookLocation, true, true);
        }, delay));
    }

    private void startSession(Player player, UUID uuid, org.bukkit.Location hookLocation,
                              boolean inLava, boolean rewardOnReel) {
        clearSession(uuid);
        long windowMs = computeWindowMs(player);
        FishingMinigame minigame = minigames.create(random, new FishingMinigameContext(player, windowMs));
        FishingSession session = new FishingSession(minigame, inLava, rewardOnReel);
        sessions.put(uuid, session);
        minigame.start();
        session.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            minigame.tick();
            if (minigame.isComplete()) finishSession(uuid, minigame.isSuccessful());
        }, 0L, 1L);
    }

    private void handleReel(Player player, UUID uuid) {
        clearLavaTask(uuid);
        FishingSession session = sessions.get(uuid);
        if (session == null) return;
        session.minigame.reel();
        if (!session.minigame.isComplete()) return;
        boolean success = session.minigame.isSuccessful();
        finishSession(uuid, success);
        if (success) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.2f);
            if (session.inLava && session.rewardOnReel) {
                ItemStack fishItem = awardCatch(player, session.inLava);
                giveFishItem(player, fishItem);
                lastRewarded.put(uuid, true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> lastRewarded.remove(uuid), 40L);
            }
        } else {
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.9f, 0.8f);
        }
    }

    private void handleCatch(PlayerFishEvent event, Player player, UUID uuid) {
        clearLavaTask(uuid);
        if (lastRewarded.remove(uuid) != null) {
            event.setCancelled(true);
            return;
        }
        boolean success = resolveSuccess(uuid);
        if (!success) {
            event.setCancelled(true);
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.9f, 0.7f);
            return;
        }

        ItemStack fishItem = awardCatch(player, isLavaFishingArea(player, event.getHook() != null ? event.getHook().getLocation() : null));

        if (event.getCaught() instanceof Item item) {
            item.setItemStack(fishItem);
        } else {
            giveFishItem(player, fishItem);
        }
    }

    private ItemStack awardCatch(Player player, boolean inLava) {
        ItemStack rod = resolveRod(player);
        ToolTier tier = resolveTier(rod);
        boolean highestTier = tier != null && tier.isHighestTier();

        double rarityBonus = tier == null ? 0.0 : Math.max(0.0, tier.getFishRarityBonus() - 1.0);
        FishDefinition definition = rewardsConfig.rollFish(
                fishingManager.getLevel(player),
                inLava,
                highestTier,
                rarityBonus,
                random);
        double size = rollSize(definition);
        ItemStack fishItem = FishingItemUtil.createFishItem(definition, size);

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
        return fishItem;
    }

    private void giveFishItem(Player player, ItemStack fishItem) {
        if (player == null || fishItem == null) {
            return;
        }
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(fishItem);
        if (!overflow.isEmpty()) {
            overflow.values().forEach(item ->
                    player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
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
        long window = Math.round(BITE_WINDOW_MS * speedMultiplier);
        return Math.max(500L, window);
    }

    private boolean resolveSuccess(UUID uuid) {
        Boolean stored = lastResult.remove(uuid);
        if (stored != null) {
            return stored;
        }
        FishingSession session = sessions.get(uuid);
        if (session == null) return false;
        session.minigame.tick();
        if (!session.minigame.isComplete()) session.minigame.reel();
        boolean success = session.minigame.isComplete() && session.minigame.isSuccessful();
        finishSession(uuid, success);
        return success;
    }

    private double rollSize(FishDefinition definition) {
        int min = definition.minSize();
        int max = Math.max(min, definition.maxSize());
        double size = min + (random.nextDouble() * (max - min));
        return Math.round(size * 10.0) / 10.0;
    }

    private boolean isLavaHook(org.bukkit.Location hookLocation) {
        if (hookLocation == null) return false;
        Block block = hookLocation.getBlock();
        if (isLavaBlock(block)) {
            return true;
        }
        return isLavaBlock(block.getRelative(BlockFace.DOWN));
    }

    private boolean isLavaFishingArea(Player player, org.bukkit.Location hookLocation) {
        if (player == null) return false;
        if (isLavaHook(hookLocation)) {
            return true;
        }
        return isNearLava(player.getLocation(), 4, 2)
                || (hookLocation != null && isNearLava(hookLocation, 2, 2));
    }

    private boolean isNearLava(org.bukkit.Location origin, int radius, int vertical) {
        if (origin == null || origin.getWorld() == null) return false;
        Block base = origin.getBlock();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -vertical; dy <= vertical; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (isLavaBlock(base.getRelative(dx, dy, dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isLavaBlock(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        return type == Material.LAVA || type == Material.LAVA_CAULDRON;
    }

    private void clearSession(UUID uuid) {
        FishingSession session = sessions.remove(uuid);
        if (session == null) return;
        if (session.task != null) {
            session.task.cancel();
        }
        session.minigame.dispose();
    }

    private void finishSession(UUID uuid, boolean success) {
        lastResult.put(uuid, success);
        clearSession(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRodReel(PlayerInteractEvent event) {
        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
            }
            default -> {
                return;
            }
        }
        if (event.getItem() == null || event.getItem().getType() != Material.FISHING_ROD) {
            return;
        }
        UUID uuid = event.getPlayer().getUniqueId();
        if (!sessions.containsKey(uuid)) {
            return;
        }
        handleReel(event.getPlayer(), uuid);
    }

    private void clearLavaTask(UUID uuid) {
        org.bukkit.scheduler.BukkitTask task = lavaBiteTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        clearLavaTask(uuid);
        clearSession(uuid);
        lastResult.remove(uuid);
        lastRewarded.remove(uuid);
    }

    private static class FishingSession {
        private final FishingMinigame minigame;
        private final boolean inLava;
        private final boolean rewardOnReel;
        private org.bukkit.scheduler.BukkitTask task;

        private FishingSession(FishingMinigame minigame, boolean inLava, boolean rewardOnReel) {
            this.minigame = minigame;
            this.inLava = inLava;
            this.rewardOnReel = rewardOnReel;
        }
    }
}
