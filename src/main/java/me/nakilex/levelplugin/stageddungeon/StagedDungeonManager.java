package me.nakilex.levelplugin.stageddungeon;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.arena.instance.ArenaInstance;
import me.nakilex.levelplugin.arena.instance.ArenaInstanceManager;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import me.nakilex.levelplugin.utils.AttributeUtil;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.NumberUtil;
import me.nakilex.levelplugin.utils.TeleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/** Coordinates reusable stage-based currency dungeon runs and progression. */
public class StagedDungeonManager implements Listener {
    private static final String RUN_MOB_TAG = "staged_dungeon_mob";

    private final Main plugin;
    private final ArenaInstanceManager instanceManager;
    private final PlayerConfig playerConfig;
    private final ProfileManager profileManager = ProfileManager.getInstance();
    private final Map<String, StagedDungeonDefinition> definitions = new HashMap<>();
    private final Map<UUID, StagedDungeonRun> activeRuns = new HashMap<>();

    public StagedDungeonManager(Main plugin, ArenaInstanceManager instanceManager) {
        this.plugin = plugin;
        this.instanceManager = instanceManager;
        this.playerConfig = plugin.getPlayerConfig();
        registerDefaults();
    }

    private void registerDefaults() {
        registerDefinition(new StagedDungeonDefinition(
                "gem",
                "Gem Dungeon",
                ChatColor.LIGHT_PURPLE,
                org.bukkit.Material.AMETHYST_CLUSTER,
                "gem_dungeon",
                org.bukkit.entity.EntityType.SLIME,
                "Common Slime",
                100.0D,
                50.0D,
                3,
                "gems",
                "<glyph:purple_orb_icon>",
                (player, amount) -> plugin.getGemsManager().addUnits(player, amount)
        ));
    }

    public void registerDefinition(StagedDungeonDefinition definition) {
        if (definition == null || definition.id() == null || definition.id().isBlank()) return;
        definitions.put(definition.id().toLowerCase(), definition);
    }

    public Optional<StagedDungeonDefinition> getDefinition(String id) {
        return Optional.ofNullable(definitions.get(id == null ? "" : id.toLowerCase()));
    }

    public Collection<StagedDungeonDefinition> getDefinitions() {
        return java.util.Collections.unmodifiableCollection(definitions.values());
    }

    public boolean isInRun(UUID playerId) {
        return activeRuns.containsKey(playerId);
    }

    public int getHighestCleared(Player player, StagedDungeonDefinition definition) {
        Integer slot = profileManager.getActiveSlot(player.getUniqueId());
        return slot == null ? 0 : playerConfig.getStagedDungeonBestStage(player.getUniqueId(), slot, definition.id());
    }

    public int getSweepsUsed(Player player, StagedDungeonDefinition definition) {
        Integer slot = profileManager.getActiveSlot(player.getUniqueId());
        if (slot == null) return 0;
        String today = currentSweepResetKey();
        String stored = playerConfig.getStagedDungeonSweepResetKey(player.getUniqueId(), slot, definition.id());
        if (!today.equals(stored)) {
            playerConfig.setStagedDungeonSweeps(player.getUniqueId(), slot, definition.id(), 0, today);
            playerConfig.savePlayer(player.getUniqueId());
            return 0;
        }
        return playerConfig.getStagedDungeonSweepsUsed(player.getUniqueId(), slot, definition.id());
    }

    public int getSweepsLeft(Player player, StagedDungeonDefinition definition) {
        return Math.max(0, definition.sweepAttempts() - getSweepsUsed(player, definition));
    }

    public void startStage(Player player, StagedDungeonDefinition definition) {
        if (activeRuns.containsKey(player.getUniqueId())) {
            ChatMessageUtil.send(player, MessageType.WARNING, "You are already inside a dungeon.");
            return;
        }
        if (instanceManager == null || !instanceManager.isTemplateLoaded()) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Dungeon arenas are unavailable right now.");
            return;
        }
        int stage = definition.nextStage(getHighestCleared(player, definition));
        ArenaInstance instance = instanceManager.createInstance(definition.worldPrefix());
        if (instance == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Failed to create a dungeon instance.");
            return;
        }
        StagedDungeonRun run = new StagedDungeonRun(player.getUniqueId(), definition, stage,
                definition.mobHealth(stage), player.getLocation(), instance);
        activeRuns.put(player.getUniqueId(), run);
        TeleportUtils.safeTeleport(player, instance.getFirstSpawn());
        spawnStageMob(run);
        ChatMessageUtil.send(player, MessageType.SUCCESS,
                "Entering " + definition.themeColor() + definition.displayName() + ChatColor.GREEN
                        + " Stage " + ChatColor.WHITE + stage + ChatColor.GREEN + ".");
    }

    public void sweep(Player player, StagedDungeonDefinition definition) {
        if (activeRuns.containsKey(player.getUniqueId())) {
            ChatMessageUtil.send(player, MessageType.WARNING, "Finish your active dungeon before sweeping.");
            return;
        }
        int highest = getHighestCleared(player, definition);
        if (highest <= 0) {
            ChatMessageUtil.send(player, MessageType.WARNING, "Clear Stage 1 before sweeping this dungeon.");
            return;
        }
        int left = getSweepsLeft(player, definition);
        if (left <= 0) {
            ChatMessageUtil.send(player, MessageType.WARNING, "You have no sweeps left for " + definition.displayName() + ".");
            return;
        }
        Integer slot = profileManager.getActiveSlot(player.getUniqueId());
        if (slot == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "No active profile found.");
            return;
        }
        int stage = definition.sweepStage(highest);
        int reward = definition.rewardForStage(stage);
        definition.rewardGrant().grant(player, reward);
        int used = getSweepsUsed(player, definition) + 1;
        playerConfig.setStagedDungeonSweeps(player.getUniqueId(), slot, definition.id(), used, currentSweepResetKey());
        playerConfig.savePlayer(player.getUniqueId());
        ChatMessageUtil.send(player, MessageType.REWARD,
                "You received " + definition.themeColor() + NumberUtil.formatCommas(reward) + " "
                        + definition.rewardGlyph() + " " + ChatColor.GOLD + definition.rewardName()
                        + ChatColor.GOLD + " from sweeping " + definition.displayName() + " Stage "
                        + ChatColor.WHITE + stage + ChatColor.GOLD + ".");
    }

    public void stopAll() {
        for (StagedDungeonRun run : new java.util.ArrayList<>(activeRuns.values())) {
            finishRun(run, false, false);
        }
        activeRuns.clear();
    }

    private void spawnStageMob(StagedDungeonRun run) {
        Location spawn = run.instance.getSecondSpawn();
        Attribute maxHealthAttr = AttributeUtil.resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        LivingEntity entity = (LivingEntity) spawn.getWorld().spawnEntity(spawn, run.definition.mobType());
        if (maxHealthAttr != null && entity.getAttribute(maxHealthAttr) != null) {
            entity.getAttribute(maxHealthAttr).setBaseValue(run.mobHealth);
            entity.setHealth(run.mobHealth);
        }
        if (entity instanceof Slime slime) {
            slime.setSize(2);
        }
        entity.setCustomName(run.definition.themeColor() + run.definition.mobDisplayName()
                + ChatColor.GRAY + " [Stage " + run.stage + "]");
        entity.setCustomNameVisible(true);
        entity.addScoreboardTag(RUN_MOB_TAG);
        entity.addScoreboardTag("staged_dungeon_" + run.definition.id());
        run.mobId = entity.getUniqueId();
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!event.getEntity().getScoreboardTags().contains(RUN_MOB_TAG)) return;
        UUID mobId = event.getEntity().getUniqueId();
        StagedDungeonRun run = activeRuns.values().stream()
                .filter(candidate -> mobId.equals(candidate.mobId))
                .findFirst()
                .orElse(null);
        if (run == null) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        completeRun(run);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        StagedDungeonRun run = activeRuns.remove(event.getPlayer().getUniqueId());
        if (run == null) return;
        run.removeMob();
        instanceManager.destroyInstance(run.instance);
        updateProfileLocation(run.playerId, run.returnLocation);
    }

    private void completeRun(StagedDungeonRun run) {
        int reward = run.definition.rewardForStage(run.stage);
        Player player = run.getPlayer();
        if (player != null) {
            run.definition.rewardGrant().grant(player, reward);
            persistHighestCleared(player, run.definition, run.stage);
            sendCompletionMessage(player, run, reward);
        }
        finishRun(run, true, true);
    }

    private void finishRun(StagedDungeonRun run, boolean teleportBack, boolean removeFromActive) {
        if (removeFromActive) activeRuns.remove(run.playerId);
        run.removeMob();
        Player player = run.getPlayer();
        if (teleportBack && player != null) {
            TeleportUtils.safeTeleport(player, run.returnLocation);
            updateProfileLocation(player.getUniqueId(), run.returnLocation);
        }
        instanceManager.destroyInstance(run.instance);
    }

    private void persistHighestCleared(Player player, StagedDungeonDefinition definition, int stage) {
        Integer slot = profileManager.getActiveSlot(player.getUniqueId());
        if (slot == null) return;
        int current = playerConfig.getStagedDungeonBestStage(player.getUniqueId(), slot, definition.id());
        playerConfig.setStagedDungeonBestStage(player.getUniqueId(), slot, definition.id(), Math.max(current, stage));
        playerConfig.savePlayer(player.getUniqueId());
    }

    private void sendCompletionMessage(Player player, StagedDungeonRun run, int reward) {
        ChatFormatter.constructDivider(player, run.definition.themeColor() + "§l-", 45);
        ChatFormatter.sendCenteredMessage(player, run.definition.themeColor() + "§l" + run.definition.displayName().toUpperCase() + " CLEARED");
        ChatFormatter.sendCenteredMessage(player, "");
        ChatFormatter.sendCenteredMessage(player,
                ChatColor.GRAY + "Stage " + ChatColor.WHITE + run.stage + ChatColor.GRAY + " defeated.");
        ChatFormatter.sendCenteredMessage(player,
                ChatColor.GRAY + "Reward: " + run.definition.themeColor() + NumberUtil.formatCommas(reward)
                        + " " + run.definition.rewardGlyph() + " " + run.definition.rewardName());
        ChatFormatter.constructDivider(player, run.definition.themeColor() + "§l-", 45);
    }

    private void updateProfileLocation(UUID id, Location back) {
        if (back == null) return;
        Integer slot = profileManager.getActiveSlot(id);
        if (slot != null) {
            playerConfig.setProfileLocation(id, slot, back);
            playerConfig.savePlayer(id);
        }
    }

    public boolean isInstanceWorld(World world) {
        if (world == null) return false;
        return activeRuns.values().stream().anyMatch(run -> run.instance.getWorld().equals(world));
    }

    private String currentSweepResetKey() {
        return LocalDate.now(ZoneOffset.UTC).toString();
    }
}
