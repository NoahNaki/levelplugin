package me.nakilex.levelplugin.catacombs;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Direction;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.FileUtil;
import me.nakilex.levelplugin.utils.AttributeUtil;
import me.nakilex.levelplugin.utils.TeleportUtils;
import me.nakilex.levelplugin.player.config.PlayerConfig;
import me.nakilex.levelplugin.player.profile.ProfileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;

/**
 * Lightweight instanced catacombs flow that reuses the dungeon template
 * placement logic. Rooms are placed on-demand as the player progresses
 * through stages.
 */
public class CatacombsManager implements Listener {
    public record StageStatus(int stage, int mobsRemaining, int secondsLeft) {}

    private final Main plugin;
    private final DungeonManager dungeonManager;
    private final PlayerConfig playerConfig;
    private final ProfileManager profileManager;
    private final RoomTemplate waitingTemplate;
    private final RoomTemplate combatTemplate;
    private final Map<UUID, CatacombRun> runs = new HashMap<>();

    public CatacombsManager(Main plugin, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.dungeonManager = dungeonManager;
        this.playerConfig = plugin.getPlayerConfig();
        this.profileManager = ProfileManager.getInstance();
        this.waitingTemplate = dungeonManager.getHallway();
        this.combatTemplate = dungeonManager.getStraight();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public boolean isInCatacombs(UUID playerId) {
        return runs.containsKey(playerId);
    }

    public StageStatus getStage(UUID playerId) {
        CatacombRun run = runs.get(playerId);
        if (run == null || !run.isActive()) return null;
        int seconds = (int) Math.max(0, (run.deadline - System.currentTimeMillis()) / 1000L);
        return new StageStatus(run.stage, run.mobsRemaining, seconds);
    }

    public void startRun(Player player) {
        if (runs.containsKey(player.getUniqueId())) {
            ChatMessageUtil.send(player, MessageType.WARNING, "You are already inside the Catacombs.");
            return;
        }
        if (waitingTemplate == null || combatTemplate == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Catacomb templates are unavailable.");
            return;
        }
        Location returnLoc = player.getLocation();
        String worldName = "catacombs_" + player.getUniqueId();
        World world = dungeonManager.createVoidWorld(worldName);
        if (world == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Failed to create Catacombs world.");
            return;
        }
        world.setDifficulty(org.bukkit.Difficulty.HARD);
        Dungeon dungeon = new Dungeon(world, worldName);
        Location anchor = new Location(world, 0, 70, 0);

        RoomPlacement waiting = placeInitialRoom(dungeon, waitingTemplate, anchor);
        if (waiting == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Unable to place the waiting room.");
            cleanupWorld(world);
            return;
        }
        RoomPlacement combat = attachRoom(dungeon, combatTemplate, waiting.exitConnector);
        if (combat == null) {
            ChatMessageUtil.send(player, MessageType.ERROR, "Unable to place the combat room.");
            dungeon.delete();
            cleanupWorld(world);
            return;
        }

        int highestCleared = getHighestCleared(player.getUniqueId());
        CatacombRun run = new CatacombRun(player.getUniqueId(), dungeon, waiting, combat, returnLoc, highestCleared);
        runs.put(player.getUniqueId(), run);
        TeleportUtils.safeTeleport(player, waiting.spawnLocation);
        ChatMessageUtil.send(player, MessageType.SUCCESS, "You descend into the Catacombs.");
        beginStage(run);
    }

    public void exit(Player player) {
        CatacombRun run = runs.remove(player.getUniqueId());
        if (run == null) {
            ChatMessageUtil.send(player, MessageType.WARNING, "You are not inside the Catacombs.");
            return;
        }
        persistProgress(run);
        run.end();
        TeleportUtils.safeTeleport(player, run.returnLocation);
        updateProfileLocation(player.getUniqueId(), run.returnLocation);
        cleanupWorld(run.world);
        ChatMessageUtil.send(player, MessageType.INFO, "You leave the Catacombs.");
    }

    private RoomPlacement placeInitialRoom(Dungeon dungeon, RoomTemplate template, Location center) {
        DungeonManager.PasteResult result = dungeonManager.pasteRoom(dungeon, template, 0, center, null, false);
        if (!result.success()) return null;
        RoomTemplate.Connector entrance = chooseEntrance(template);
        RoomTemplate.Connector exit = chooseExit(template, entrance);
        Location spawn = center.clone().add(0.5, 1.2, 0.5);
        Location exitLoc = connectorLocation(template, exit, center, 0);
        Direction exitFacing = exit == null ? Direction.NORTH : rotate(exit.facing, 0);
        return new RoomPlacement(result.instance(), new Connection(exitLoc, exitFacing), spawn);
    }

    private RoomPlacement attachRoom(Dungeon dungeon, RoomTemplate template, Connection target) {
        if (target == null) return null;
        Placement placement = computePlacement(template, target);
        if (placement == null) return null;
        DungeonManager.PasteResult result = dungeonManager.pasteRoom(dungeon, template, placement.rotation, placement.center, null, false);
        if (!result.success()) return null;
        RoomTemplate.Connector entrance = placement.match;
        RoomTemplate.Connector exit = chooseExit(template, entrance);
        Location exitLoc = connectorLocation(template, exit, placement.center, placement.rotation);
        Location spawn = connectorLocation(template, entrance, placement.center, placement.rotation).add(0, 1.2, 0);
        Direction exitFacing = exit == null ? target.facing : rotate(exit.facing, placement.rotation);
        return new RoomPlacement(result.instance(), new Connection(exitLoc, exitFacing), spawn);
    }

    private RoomTemplate.Connector chooseEntrance(RoomTemplate template) {
        for (RoomTemplate.Connector c : template.getConnectors()) {
            if (c.entrance) return c;
        }
        return template.getConnectors().isEmpty() ? null : template.getConnectors().get(0);
    }

    private RoomTemplate.Connector chooseExit(RoomTemplate template, RoomTemplate.Connector entrance) {
        if (template.getConnectors().isEmpty()) return entrance;
        for (RoomTemplate.Connector c : template.getConnectors()) {
            if (entrance == null || c != entrance) return c;
        }
        return entrance;
    }

    private Placement computePlacement(RoomTemplate template, Connection target) {
        RoomTemplate.Connector match = null;
        int rotation = 0;
        List<RoomTemplate.Connector> entrances = new ArrayList<>();
        for (RoomTemplate.Connector c : template.getConnectors()) {
            if (c.entrance) entrances.add(c);
        }
        List<RoomTemplate.Connector> pool = entrances.isEmpty() ? template.getConnectors() : entrances;
        for (RoomTemplate.Connector c : pool) {
            rotation = (target.facing.opposite().ordinal() - c.facing.ordinal()) & 3;
            match = c;
            break;
        }
        if (match == null) return null;
        int[] vec = RoomTemplate.rotate(match.x - (int) Math.round(template.getCenterX()),
                match.z - (int) Math.round(template.getCenterZ()), rotation);
        Location center = target.location.clone().subtract(vec[0], match.bottomY - template.getConnectorMinY(), vec[1]);
        return new Placement(match, rotation, center);
    }

    private Direction rotate(Direction dir, int rot) {
        return Direction.values()[(dir.ordinal() + rot) & 3];
    }

    private Location connectorLocation(RoomTemplate template, RoomTemplate.Connector connector, Location center, int rotation) {
        if (connector == null) return center.clone();
        int[] vec = RoomTemplate.rotate(connector.x - (int) Math.round(template.getCenterX()),
                connector.z - (int) Math.round(template.getCenterZ()), rotation);
        return center.clone().add(vec[0], connector.bottomY - template.getConnectorMinY(), vec[1]);
    }

    private void beginStage(CatacombRun run) {
        run.mobsRemaining = spawnMobs(run);
        run.deadline = System.currentTimeMillis() + 45_000L;
        if (run.timer != null) run.timer.cancel();
        run.timer = new BukkitRunnable() {
            @Override
            public void run() {
                if (!run.isActive()) {
                    cancel();
                    return;
                }
                if (System.currentTimeMillis() >= run.deadline) {
                    failStage(run);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private int spawnMobs(CatacombRun run) {
        Location center = run.combatRoom.instance.center.clone().add(0.5, 1, 0.5);
        int count = Math.max(3, 2 + run.stage);
        Attribute maxHealthAttr = AttributeUtil.resolve("GENERIC_MAX_HEALTH", "MAX_HEALTH");
        Attribute attackDamageAttr = AttributeUtil.resolve("GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE");
        for (int i = 0; i < count; i++) {
            Location spawn = center.clone().add((i % 3) - 1, 0, (i / 3));
            LivingEntity entity = spawn.getWorld().spawn(spawn, org.bukkit.entity.Zombie.class, e -> {
                double health = 20.0 + (run.stage * 10.0);
                double damage = 4.0 + (run.stage * 1.5);
                if (maxHealthAttr != null && e.getAttribute(maxHealthAttr) != null) {
                    e.getAttribute(maxHealthAttr).setBaseValue(health);
                    e.setHealth(health);
                }
                if (attackDamageAttr != null && e.getAttribute(attackDamageAttr) != null) {
                    e.getAttribute(attackDamageAttr).setBaseValue(damage);
                }
                e.setCustomName(ChatColor.DARK_RED + "Catacomb Foe");
                e.addScoreboardTag("catacombs_mob");
            });
            run.mobIds.add(entity.getUniqueId());
        }
        return count;
    }

    private void failStage(CatacombRun run) {
        if (!run.isActive()) return;
        if (run.timer != null) {
            run.timer.cancel();
            run.timer = null;
        }
        run.clearMobs();
        run.deadline = 0L;
        ChatMessageUtil.send(run.getPlayer(), MessageType.ERROR, "You failed to clear the stage in time!");
        TeleportUtils.safeTeleport(run.getPlayer(), run.waitingRoom.spawnLocation);
        beginStage(run);
    }

    private void completeStage(CatacombRun run) {
        run.stage++;
        run.highestCleared = Math.max(run.highestCleared, run.stage - 1);
        persistProgress(run);
        RoomPlacement previousWaiting = run.waitingRoom;
        RoomPlacement nextWaiting = attachRoom(run.dungeon, waitingTemplate, run.combatRoom.exitConnector);
        if (nextWaiting == null) {
            ChatMessageUtil.send(run.getPlayer(), MessageType.ERROR, "No further rooms can be placed. Exiting...");
            exit(run.getPlayer());
            return;
        }
        RoomPlacement nextCombat = attachRoom(run.dungeon, combatTemplate, nextWaiting.exitConnector);
        if (nextCombat == null) {
            ChatMessageUtil.send(run.getPlayer(), MessageType.ERROR, "Unable to continue deeper.");
            exit(run.getPlayer());
            return;
        }
        run.waitingRoom = nextWaiting;
        run.combatRoom = nextCombat;
        scheduleRemoval(run, previousWaiting);
        ChatMessageUtil.send(run.getPlayer(), MessageType.SUCCESS,
                "Stage " + (run.stage - 1) + " cleared! Preparing the next fight.");
        beginStage(run);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!event.getEntity().getScoreboardTags().contains("catacombs_mob")) return;
        World world = event.getEntity().getWorld();
        CatacombRun run = runs.values().stream()
                .filter(r -> r.world.equals(world))
                .findFirst()
                .orElse(null);
        if (run == null) return;
        if (!run.mobIds.remove(event.getEntity().getUniqueId())) return;
        run.mobsRemaining = Math.max(0, run.mobsRemaining - 1);
        if (run.mobsRemaining <= 0) {
            run.clearMobs();
            if (run.timer != null) run.timer.cancel();
            completeStage(run);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        CatacombRun run = runs.remove(event.getPlayer().getUniqueId());
        if (run != null) {
            persistProgress(run);
            run.end();
            updateProfileLocation(run.playerId, run.returnLocation);
            cleanupWorld(run.world);
        }
    }

    private void cleanupWorld(World world) {
        Bukkit.unloadWorld(world, false);
        FileUtil.deleteDirectory(world.getWorldFolder());
    }

    private void updateProfileLocation(UUID id, Location back) {
        if (back == null) return;
        Integer slot = profileManager.getActiveSlot(id);
        if (slot != null) {
            playerConfig.setProfileLocation(id, slot, back);
            playerConfig.savePlayer(id);
        }
    }

    private int getHighestCleared(UUID playerId) {
        Integer slot = profileManager.getActiveSlot(playerId);
        if (slot == null) return 0;
        return playerConfig.getCatacombsBestStage(playerId, slot);
    }

    private void persistProgress(CatacombRun run) {
        Integer slot = profileManager.getActiveSlot(run.playerId);
        if (slot == null) return;
        playerConfig.setCatacombsBestStage(run.playerId, slot, run.highestCleared);
        playerConfig.savePlayer(run.playerId);
    }

    private void scheduleRemoval(CatacombRun run, RoomPlacement toRemove) {
        if (toRemove == null) return;
        new BukkitRunnable() {
            int attempts = 0;

            @Override
            public void run() {
                attempts++;
                if (!runs.containsKey(run.playerId)) {
                    cancel();
                    return;
                }
                Player player = run.getPlayer();
                boolean playerInside = player != null && toRemove.instance.contains(player.getLocation());
                if (playerInside && attempts < 10) {
                    return;
                }
                run.dungeon.deleteRoom(toRemove.instance);
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private class CatacombRun {
        final UUID playerId;
        final Dungeon dungeon;
        final World world;
        final Location returnLocation;
        RoomPlacement waitingRoom;
        RoomPlacement combatRoom;
        int stage;
        int highestCleared;
        long deadline = 0L;
        int mobsRemaining = 0;
        BukkitTask timer;
        final Set<UUID> mobIds = new HashSet<>();

        CatacombRun(UUID playerId, Dungeon dungeon, RoomPlacement waitingRoom,
                    RoomPlacement combatRoom, Location returnLocation, int highestCleared) {
            this.playerId = playerId;
            this.dungeon = dungeon;
            this.world = dungeon.getWorld();
            this.waitingRoom = waitingRoom;
            this.combatRoom = combatRoom;
            this.returnLocation = returnLocation;
            this.highestCleared = highestCleared;
            this.stage = Math.max(1, highestCleared + 1);
        }

        boolean isActive() {
            Player player = getPlayer();
            return player != null && player.isOnline();
        }

        Player getPlayer() {
            return Bukkit.getPlayer(playerId);
        }

        void clearMobs() {
            for (UUID id : new HashSet<>(mobIds)) {
                var ent = Bukkit.getEntity(id);
                if (ent instanceof LivingEntity living) {
                    living.remove();
                }
            }
            mobIds.clear();
        }

        void end() {
            if (timer != null) timer.cancel();
            clearMobs();
            dungeon.delete();
        }
    }

    private record RoomPlacement(Dungeon.RoomInstance instance,
                                 Connection exitConnector,
                                 Location spawnLocation) {}

    private record Connection(Location location, Direction facing) {}

    private record Placement(RoomTemplate.Connector match, int rotation, Location center) {}
}
