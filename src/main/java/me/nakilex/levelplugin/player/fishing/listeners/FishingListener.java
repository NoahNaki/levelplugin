package me.nakilex.levelplugin.player.fishing.listeners;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.items.tools.CustomTool;
import me.nakilex.levelplugin.items.tools.ToolManager;
import me.nakilex.levelplugin.items.tools.ToolTier;
import me.nakilex.levelplugin.player.fishing.config.FishingRewardsConfig;
import me.nakilex.levelplugin.player.fishing.data.FishDefinition;
import me.nakilex.levelplugin.player.fishing.managers.FishingManager;
import me.nakilex.levelplugin.player.fishing.minigame.FishingMiniGame;
import me.nakilex.levelplugin.player.fishing.minigame.FishingMiniGameManager;
import me.nakilex.levelplugin.player.fishing.utils.FishingItemUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
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
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class FishingListener implements Listener {

    private static final int LAVA_BITE_MIN_TICKS = 20;
    private static final int LAVA_BITE_MAX_TICKS = 50;

    private final Main plugin;
    private final FishingRewardsConfig rewardsConfig;
    private final FishingManager fishingManager;
    private final FishingMiniGameManager miniGameManager;
    private final Map<UUID, org.bukkit.scheduler.BukkitTask> lavaBiteTasks = new HashMap<>();
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

        if (miniGameManager.isPlaying(uuid)) {
            switch (event.getState()) {
                case REEL_IN, FISHING, CAUGHT_FISH -> {
                    event.setCancelled(true);
                    return;
                }
                default -> { }
            }
        }

        switch (event.getState()) {
            case FISHING -> handleCast(player, uuid, event.getHook());
            case BITE -> {
                event.setCancelled(true);
                startMiniGame(player, uuid, event.getHook(),
                        isLavaFishingArea(player, event.getHook() != null ? event.getHook().getLocation() : null));
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
        miniGameManager.cancel(uuid);
        org.bukkit.Location hookLocation = hook != null ? hook.getLocation() : null;
        if (!isLavaFishingArea(player, hookLocation)) return;
        int delay = ThreadLocalRandom.current().nextInt(LAVA_BITE_MIN_TICKS, LAVA_BITE_MAX_TICKS + 1);
        lavaBiteTasks.put(uuid, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) startMiniGame(player, uuid, hook, true);
        }, delay));
    }

    private void startMiniGame(Player player, UUID uuid, FishHook hook, boolean inLava) {
        clearLavaTask(uuid);
        miniGameManager.startRandom(player, computeDurationMultiplier(player), success -> {
            if (hook != null && hook.isValid()) hook.remove();
            if (!success || !player.isOnline()) return;
            giveFishItem(player, awardCatch(player, inLava));
        });
    }

    private void handleCatch(PlayerFishEvent event, Player player, UUID uuid) {
        clearLavaTask(uuid);
        event.setCancelled(true);
        if (!miniGameManager.isPlaying(uuid)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.WARNING,
                    "Wait for a bite and complete its fishing mini-game first.");
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

    private double computeDurationMultiplier(Player player) {
        ItemStack rod = resolveRod(player);
        ToolTier tier = resolveTier(rod);
        return tier != null ? Math.max(0.5, tier.getFishingSpeed()) : 1.0;
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onMiniGameClick(PlayerInteractEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!miniGameManager.isPlaying(uuid)) return;
        switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> miniGameManager.click(uuid);
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                miniGameManager.rightClick(uuid);
                if (event.getItem() != null && event.getItem().getType() == Material.FISHING_ROD) {
                    event.setCancelled(true);
                    return;
                }
            }
            default -> { }
        }
    }

    @EventHandler
    public void onMiniGameSneak(PlayerToggleSneakEvent event) {
        miniGameManager.sneak(event.getPlayer().getUniqueId(), event.isSneaking());
    }

    @EventHandler
    public void onMiniGameMove(PlayerMoveEvent event) {
        if (event.getTo() == null || !miniGameManager.isPlaying(event.getPlayer().getUniqueId())) return;
        double dy = event.getTo().getY() - event.getFrom().getY();
        if (dy > 0.18) {
            miniGameManager.move(event.getPlayer().getUniqueId(), FishingMiniGame.Movement.JUMP);
            return;
        }
        org.bukkit.util.Vector delta = event.getTo().toVector().subtract(event.getFrom().toVector());
        if (delta.lengthSquared() < 0.0025) return;
        double side = delta.dot(event.getPlayer().getLocation().getDirection().crossProduct(new org.bukkit.util.Vector(0, 1, 0)));
        miniGameManager.move(event.getPlayer().getUniqueId(), side >= 0 ? FishingMiniGame.Movement.RIGHT : FishingMiniGame.Movement.LEFT);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        clearLavaTask(uuid);
        miniGameManager.cancel(uuid);
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
