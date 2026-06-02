package me.nakilex.levelplugin.player.fishing.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.items.tools.FishingToolEnchant;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.player.fishing.config.FishingRewardsConfig;
import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import me.nakilex.levelplugin.player.fishing.data.FishingQuality;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.player.fishing.minigame.FishingDifficultyProfile;
import me.nakilex.levelplugin.player.fishing.minigame.FishingDifficultyResolver;
import me.nakilex.levelplugin.player.fishing.minigame.FishingMiniGame;
import me.nakilex.levelplugin.player.fishing.minigame.FishingMiniGameManager;
import me.nakilex.levelplugin.player.fishing.utils.FishingItemUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.advancement.AdvancementToastUtil;
import me.nakilex.levelplugin.advancement.model.AdvancementDisplay;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.SoundMelodyUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FishingListener implements Listener {
    private static final SoundMelodyUtil.Note[] TROPHY_QUALITY_MELODY = {
            new SoundMelodyUtil.Note(0L, 1.0f),
            new SoundMelodyUtil.Note(4L, 1.2f),
            new SoundMelodyUtil.Note(6L, 1.4f),
            new SoundMelodyUtil.Note(8L, 1.6f),
            new SoundMelodyUtil.Note(12L, 1.2f),
            new SoundMelodyUtil.Note(16L, 1.8f)
    };

    private static final int LAVA_BITE_MIN_TICKS = 20;
    private static final int LAVA_BITE_MAX_TICKS = 50;
    private static final long RECENTLY_COMPLETED_MINI_GAME_MS = 2_000L;

    private final Main plugin;
    private final FishingRewardsConfig rewardsConfig;
    private final FishingManager fishingManager;
    private final FishingMiniGameManager miniGameManager;
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> lavaBiteTasks = new HashMap<>();
    private final Map<UUID, Long> recentlyCompletedMiniGame = new HashMap<>();
    private final Random random = new Random();

    public FishingListener(Main plugin, FishingRewardsConfig rewardsConfig, FishingManager fishingManager) {
        this.plugin = plugin;
        this.rewardsConfig = rewardsConfig;
        this.fishingManager = fishingManager;
        this.miniGameManager = new FishingMiniGameManager(plugin);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        boolean miniGamesEnabled = miniGameManager.isEnabled();
        if (!miniGamesEnabled) miniGameManager.cancelSilently(uuid);
        if (event.getState() == PlayerFishEvent.State.FISHING && !canUseRod(player, event.getHook())) {
            event.setCancelled(true);
            return;
        }

        if (miniGamesEnabled && miniGameManager.isPlaying(uuid)) {
            event.setCancelled(true);
            miniGameManager.logVanillaState(player,
                    "Cancelled vanilla fishing state because mini-game is active: " + event.getState());
            return;
        }

        switch (event.getState()) {
            case FISHING -> handleCast(player, uuid, event.getHook());
            case BITE -> {
                if (miniGamesEnabled) {
                    event.setCancelled(true);
                    miniGameManager.logVanillaState(player, "vanillaState=BITE, starting mini-game");
                    startMiniGame(player, uuid, event.getHook(),
                            isLavaFishingArea(player, event.getHook() != null ? event.getHook().getLocation() : null));
                }
            }
            case REEL_IN -> {
                if (miniGameManager.isPlaying(uuid)) event.setCancelled(true);
            }
            case CAUGHT_FISH -> handleCatch(event, player, uuid);
            default -> {
            }
        }
    }

    private void handleCast(Player player, UUID uuid, FishHook hook) {
        if (miniGameManager.isPlaying(uuid)) return;
        clearLavaTask(uuid);
        miniGameManager.cancelSilently(uuid);
        ItemStack rod = resolveRod(player);
        double biteSpeed = resolveBiteSpeed(player, rod);
        if (hook != null) {
            hook.setMinWaitTime(scaleWaitTicks(100, biteSpeed));
            hook.setMaxWaitTime(scaleWaitTicks(600, biteSpeed));
        }
        org.bukkit.Location hookLocation = hook != null ? hook.getLocation() : null;
        if (!miniGameManager.isEnabled() || !isLavaFishingArea(player, hookLocation)) return;
        int delay = scaleWaitTicks(ThreadLocalRandom.current().nextInt(LAVA_BITE_MIN_TICKS, LAVA_BITE_MAX_TICKS + 1), biteSpeed);
        lavaBiteTasks.put(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            if (!miniGameManager.isEnabled() || miniGameManager.isPlaying(uuid)) return;
            if (hook == null || !hook.isValid()) return;
            startMiniGame(player, uuid, hook, true);
        }, delay));
    }

    private void startMiniGame(Player player, UUID uuid, FishHook hook, boolean inLava) {
        clearLavaTask(uuid);
        ItemStack rod = resolveRod(player);
        ToolTier rodTier = resolveTier(rod);
        FishDefinition hookedFish = rollHookedFish(player, inLava, rodTier);
        FishingDifficultyProfile profile = FishingDifficultyResolver.resolve(
                plugin.getConfig(), fishingManager.getLevel(player), hookedFish, rodTier);
        if (getFishingEnchant(rod) == FishingToolEnchant.STEADY_HAND) {
            profile = profile.withRodAssistance(0.18, 0.18, 0.12);
        }
        FishingMiniGameManager.DebugContext debugContext = new FishingMiniGameManager.DebugContext(
                hookedFish != null ? hookedFish.id() : "unknown",
                hookedFish != null && hookedFish.rarity() != null ? hookedFish.rarity().name() : "COMMON",
                fishingManager.getLevel(player));
        miniGameManager.startRandom(player, hook, inLava, hookedFish, profile, debugContext, success -> {
            markRecentlyCompletedMiniGame(uuid);
            if (!player.isOnline()) return;
            if (!success) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING, "The fish escaped!");
                return;
            }
            giveFishItem(player, awardSpecificCatch(player, hookedFish, inLava));
            if (getFishingEnchant(rod) == FishingToolEnchant.DOUBLE_HOOK && random.nextDouble() < 0.15) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.REWARD, "Double Hook reeled in an extra fish!");
                giveFishItem(player, awardCatch(player, inLava));
            }
        });
    }

    private void handleCatch(PlayerFishEvent event, Player player, UUID uuid) {
        clearLavaTask(uuid);
        if (miniGameManager.isEnabled()) {
            event.setCancelled(true);
            if (hasRecentlyCompletedMiniGame(uuid)) return;
            if (!miniGameManager.isPlaying(uuid)) {
                ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                        "Wait for a bite and complete its fishing mini-game first.");
            }
            return;
        }

        ItemStack fishItem = awardCatch(player,
                isLavaFishingArea(player, event.getHook() != null ? event.getHook().getLocation() : null));
        if (fishItem == null) return;
        if (event.getCaught() instanceof Item item) {
            item.setItemStack(fishItem);
        } else {
            giveFishItem(player, fishItem);
        }
    }

    private void markRecentlyCompletedMiniGame(UUID uuid) {
        long expiresAtMs = System.currentTimeMillis() + RECENTLY_COMPLETED_MINI_GAME_MS;
        recentlyCompletedMiniGame.put(uuid, expiresAtMs);
        Bukkit.getScheduler().runTaskLater(plugin, () -> recentlyCompletedMiniGame.remove(uuid, expiresAtMs), 40L);
    }

    private boolean hasRecentlyCompletedMiniGame(UUID uuid) {
        Long expiresAtMs = recentlyCompletedMiniGame.get(uuid);
        if (expiresAtMs == null) return false;
        if (System.currentTimeMillis() <= expiresAtMs) return true;
        recentlyCompletedMiniGame.remove(uuid);
        return false;
    }

    private ItemStack awardCatch(Player player, boolean inLava) {
        ItemStack rod = resolveRod(player);
        return awardSpecificCatch(player, rollHookedFish(player, inLava, resolveTier(rod)), inLava);
    }

    private FishDefinition rollHookedFish(Player player, boolean inLava, ToolTier tier) {
        boolean highestTier = tier != null && tier.isHighestTier();
        double rarityBonus = tier == null ? 0.0 : Math.max(0.0, tier.getFishRarityBonus() - 1.0);
        if (getFishingEnchant(resolveRod(player)) == FishingToolEnchant.LUCKY_CAST) rarityBonus += 0.25;
        return rewardsConfig.rollFish(fishingManager.getLevel(player), inLava, highestTier, rarityBonus, random);
    }

    private ItemStack awardSpecificCatch(Player player, FishDefinition definition, boolean inLava) {
        if (definition == null) return null;
        double size = rollSize(player, definition);
        FishingQuality quality = FishingQuality.fromSize(definition, size);
        ItemStack fishItem = FishingItemUtil.createFishItem(definition, size);

        fishingManager.addXP(player, definition.xpReward());
        boolean newlyDiscovered = fishingManager.discoverFish(player.getUniqueId(), definition.id());
        FishingManager.CatchResult catchResult = fishingManager.recordCatch(player.getUniqueId(), definition.id(), size, quality);
        if (plugin.getPlayerConfig() != null) plugin.getPlayerConfig().savePlayerData(player.getUniqueId());
        if (plugin.getQuestManager() != null) {
            plugin.getQuestManager().handleCaptureFish(player, definition.id());
        }
        String sizeLabel = String.format("%.1f cm", size);
        String expColor = ChatFormatter.experienceColor();
        String message = ChatColor.GRAY + "You caught a " + ChatColor.WHITE + sizeLabel + " "
                + definition.displayName() + ChatColor.GRAY + " and earned "
                + ChatColor.WHITE + "+" + definition.xpReward() + " " + expColor
                + "<glyph:experience_orb_icon> Fishing EXP" + ChatColor.GRAY + ".";
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO, message);
        if (newlyDiscovered) showCatalogMilestoneToast(player);
        if (catchResult.personalBest()) {
            AdvancementToastUtil.showToast(player, Material.COD, "New Personal Record!",
                    sizeLabel + " " + definition.displayName(), AdvancementDisplay.FrameType.GOAL);
        }
        if (catchResult.qualityUpgrade() && quality != FishingQuality.NORMAL) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                AdvancementToastUtil.showToast(player, Material.GOLD_NUGGET,
                        "New Trophy Quality!", quality.getDisplayName() + " " + definition.displayName(),
                        AdvancementDisplay.FrameType.CHALLENGE);
                SoundMelodyUtil.play(plugin, player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, TROPHY_QUALITY_MELODY);
            }, 24L);
        }
        return fishItem;
    }

    private void showCatalogMilestoneToast(Player player) {
        int discovered = fishingManager.getDiscoveredFish(player.getUniqueId()).size();
        int total = rewardsConfig.getFish().size();
        if (discovered != Math.min(5, total) && discovered != Math.min(10, total) && discovered != total) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> AdvancementToastUtil.showToast(player, Material.BOOK,
                "Fishing Catalog Milestone!", discovered + "/" + total + " species discovered",
                AdvancementDisplay.FrameType.GOAL), 48L);
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

    private double rollSize(Player player, FishDefinition definition) {
        int min = definition.minSize();
        int max = Math.max(min, definition.maxSize());
        double roll = random.nextDouble();
        if (getFishingEnchant(resolveRod(player)) == FishingToolEnchant.TROPHY_HUNTER) {
            roll = Math.min(1.0, roll + 0.18);
        }
        double size = min + (roll * (max - min));
        return Math.round(size * 10.0) / 10.0;
    }

    private boolean canUseRod(Player player, FishHook hook) {
        ItemStack rod = resolveRod(player);
        CustomTool tool = ToolManager.getInstance().getTool(rod);
        if (tool == null || tool.getDiscipline() != me.nakilex.levelplugin.items.tools.ToolDiscipline.FISHING
                || ToolManager.getInstance().meetsLevelRequirement(player, tool)) return true;
        if (hook != null && hook.isValid()) hook.remove();
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                "You need Fishing level " + tool.getTier().getLevelRequirement() + " to use this fishing rod.");
        return false;
    }

    private FishingToolEnchant getFishingEnchant(ItemStack rod) {
        return ToolManager.getInstance().getFishingEnchant(rod);
    }

    private double resolveBiteSpeed(Player player, ItemStack rod) {
        ToolTier tier = resolveTier(rod);
        double speed = tier == null ? 1.0 : Math.max(1.0, tier.getFishingSpeed());
        if (getFishingEnchant(rod) == FishingToolEnchant.LURE) speed *= 1.20;
        speed *= fishingManager.getDebugBiteSpeedMultiplier(player.getUniqueId());
        return speed;
    }

    private int scaleWaitTicks(int ticks, double speed) {
        return Math.max(1, (int) Math.round(ticks / Math.max(1.0, speed)));
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onMiniGameClick(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        UUID uuid = event.getPlayer().getUniqueId();
        if (!miniGameManager.isPlaying(uuid)) return;
        switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> miniGameManager.click(uuid);
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> miniGameManager.rightClick(uuid);
            default -> { }
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onMiniGameSneak(PlayerToggleSneakEvent event) {
        miniGameManager.sneak(event.getPlayer().getUniqueId(), event.isSneaking());
    }

    @EventHandler
    public void onMiniGameMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (event.getTo() == null || !miniGameManager.usesMovementInput(uuid)) return;
        double dy = event.getTo().getY() - event.getFrom().getY();
        if (dy > 0.18) {
            miniGameManager.move(uuid, FishingMiniGame.Movement.JUMP);
            return;
        }
        org.bukkit.util.Vector delta = event.getTo().toVector().subtract(event.getFrom().toVector());
        if (delta.lengthSquared() < 0.0025) return;
        double side = delta.dot(event.getPlayer().getLocation().getDirection().crossProduct(new org.bukkit.util.Vector(0, 1, 0)));
        miniGameManager.move(uuid, side >= 0 ? FishingMiniGame.Movement.RIGHT : FishingMiniGame.Movement.LEFT);
    }

    @EventHandler
    public void onMiniGameHotbarChange(PlayerItemHeldEvent event) {
        if (miniGameManager.isPlaying(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onMiniGameSwapHands(PlayerSwapHandItemsEvent event) {
        if (miniGameManager.isPlaying(event.getPlayer().getUniqueId())) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelPlayerSession(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        cancelPlayerSession(event.getEntity());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        cancelPlayerSession(event.getPlayer());
    }

    private void cancelPlayerSession(Player player) {
        UUID uuid = player.getUniqueId();
        clearLavaTask(uuid);
        miniGameManager.cancelSilently(uuid);
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin) miniGameManager.shutdown();
    }

    private void clearLavaTask(UUID uuid) {
        org.bukkit.scheduler.BukkitTask task = lavaBiteTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

}
