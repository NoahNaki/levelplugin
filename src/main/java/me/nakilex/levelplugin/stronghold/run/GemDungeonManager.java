package me.nakilex.levelplugin.stronghold.run;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import static me.nakilex.levelplugin.utils.ChatMessageUtil.send;

public class GemDungeonManager implements Listener {
    private static final String WORLD_NAME = "gem_dungeon";
    private static final int MAX_SWEEPS_PER_DAY = 3;
    private static final int TIME_LIMIT_SECONDS = 20;
    private static final double BASE_HP = 1000.0;

    private final Main plugin;
    private final Map<UUID, ActiveChallenge> active = new HashMap<>();
    private final Map<UUID, Integer> highestCleared = new HashMap<>();
    private final Map<UUID, Integer> sweepsUsedToday = new HashMap<>();
    private LocalDateMarker lastReset = LocalDateMarker.todayUtc();
    private final File dataFile;
    private final YamlConfiguration data;

    public GemDungeonManager(Main plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "gem_dungeon.yml");
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        load();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void challenge(Player player) {
        if (player == null || !player.isOnline()) return;
        cleanupDayBoundary();
        if (active.containsKey(player.getUniqueId())) {
            send(player, MessageType.WARNING, "You are already in a Gem Dungeon challenge.");
            return;
        }
        int stage = Math.max(1, highestCleared.getOrDefault(player.getUniqueId(), 0) + 1);
        startChallenge(player, stage);
    }

    public void sweep(Player player) {
        if (player == null || !player.isOnline()) return;
        cleanupDayBoundary();
        UUID id = player.getUniqueId();
        int best = highestCleared.getOrDefault(id, 0);
        if (best <= 0) {
            send(player, MessageType.WARNING, "Clear at least one stage before sweeping.");
            return;
        }
        int used = sweepsUsedToday.getOrDefault(id, 0);
        if (used >= MAX_SWEEPS_PER_DAY) {
            send(player, MessageType.WARNING, "You already used all daily sweeps (3/3).");
            return;
        }
        int gems = rewardForStage(best);
        awardGems(player, gems);
        sweepsUsedToday.put(id, used + 1);
        save();
        send(player, MessageType.SUCCESS, ChatColor.AQUA + "Sweep complete" + ChatColor.GRAY + ": Stage "
                + ChatColor.WHITE + best + ChatColor.GRAY + " reward claimed (" + ChatColor.LIGHT_PURPLE + gems + ChatColor.GRAY + " gems).");
    }

    private void startChallenge(Player player, int stage) {
        World world = ensureWorld();
        if (world == null) {
            send(player, MessageType.ERROR, "Unable to create Gem Dungeon world.");
            return;
        }
        Location spawn = world.getSpawnLocation().clone().add(0.5, 1.0, 0.5);
        player.teleport(spawn);
        Location dummyLoc = spawn.clone().add(player.getLocation().getDirection().normalize().multiply(10.0));
        dummyLoc.setY(spawn.getY());
        LivingEntity dummy = spawnDummy(dummyLoc, stage);
        if (dummy == null) {
            send(player, MessageType.ERROR, "Could not spawn combat dummy.");
            return;
        }
        send(player, MessageType.INFO, ChatColor.AQUA + "Gem Dungeon Stage " + ChatColor.WHITE + stage
                + ChatColor.GRAY + " started. Defeat the dummy in " + ChatColor.WHITE + TIME_LIMIT_SECONDS + ChatColor.GRAY + "s.");
        send(player, MessageType.INFO, ChatColor.GRAY + "Opening " + ChatColor.WHITE + "5" + ChatColor.GRAY + " rank-up spell selections.");
        int task = Bukkit.getScheduler().runTaskLater(plugin, () -> failChallenge(player.getUniqueId(), "Time limit reached."), TIME_LIMIT_SECONDS * 20L).getTaskId();
        active.put(player.getUniqueId(), new ActiveChallenge(stage, player.getLocation().clone(), dummy.getUniqueId(), task));
    }

    private LivingEntity spawnDummy(Location at, int stage) {
        LivingEntity entity = at.getWorld().spawn(at, org.bukkit.entity.Zombie.class, zombie -> zombie.setAdult());
        if (entity == null) return null;
        double hp = hpForStage(stage);
        if (entity.getAttribute(Attribute.MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(hp);
        }
        entity.setHealth(Math.min(hp, entity.getAttribute(Attribute.MAX_HEALTH) == null ? hp : entity.getAttribute(Attribute.MAX_HEALTH).getValue()));
        entity.setAI(false);
        entity.setGravity(false);
        entity.setSilent(true);
        entity.setCustomName(ChatColor.GOLD + "training_dummy" + ChatColor.GRAY + " [stage_" + stage + "]");
        if (Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
            ModelEngineUtil.applyFirstAvailableModel(entity, ModelEngineUtil.buildModelCandidates("combat_dummy"), plugin);
        }
        entity.setCustomNameVisible(true);
        return entity;
    }

    @EventHandler
    public void onDummyDeath(EntityDeathEvent event) {
        UUID dead = event.getEntity().getUniqueId();
        UUID winner = null;
        for (var entry : active.entrySet()) {
            if (entry.getValue().dummyId.equals(dead)) {
                winner = entry.getKey();
                break;
            }
        }
        if (winner == null) return;
        Player player = Bukkit.getPlayer(winner);
        ActiveChallenge challenge = active.remove(winner);
        if (challenge == null) return;
        Bukkit.getScheduler().cancelTask(challenge.failTaskId);
        int gems = rewardForStage(challenge.stage);
        if (player != null) {
            awardGems(player, gems);
            send(player, MessageType.SUCCESS, ChatColor.GREEN + "Stage cleared! " + ChatColor.GRAY + "You earned " + ChatColor.LIGHT_PURPLE + gems + ChatColor.GRAY + " gems.");
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        highestCleared.merge(winner, challenge.stage, Math::max);
        save();
    }

    private void failChallenge(UUID playerId, String reason) {
        ActiveChallenge challenge = active.remove(playerId);
        if (challenge == null) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            send(player, MessageType.WARNING, ChatColor.RED + "Gem Dungeon failed: " + ChatColor.GRAY + reason);
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        Entity entity = Bukkit.getEntity(challenge.dummyId);
        if (entity != null) entity.remove();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        failChallenge(event.getPlayer().getUniqueId(), "Challenge cancelled.");
    }

    private void awardGems(Player player, int amount) {
        GemsManager gems = plugin.getGemsManager();
        if (gems != null) gems.addUnits(player, amount);
    }

    private double hpForStage(int stage) { return BASE_HP * stage; }
    private int rewardForStage(int stage) { return (int) Math.floor(hpForStage(stage) * 0.10); }

    private World ensureWorld() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world != null) return world;
        WorldCreator creator = new WorldCreator(WORLD_NAME);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        return creator.createWorld();
    }

    private void cleanupDayBoundary() {
        LocalDateMarker now = LocalDateMarker.todayUtc();
        if (!now.equals(lastReset)) {
            sweepsUsedToday.clear();
            lastReset = now;
            save();
        }
    }

    private void load() {
        lastReset = LocalDateMarker.parse(data.getString("last-reset", LocalDateMarker.todayUtc().toString()));
        if (data.isConfigurationSection("players")) {
            for (String key : Objects.requireNonNull(data.getConfigurationSection("players")).getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    highestCleared.put(id, Math.max(0, data.getInt("players." + key + ".highest", 0)));
                    sweepsUsedToday.put(id, Math.max(0, data.getInt("players." + key + ".sweeps", 0)));
                } catch (Exception ignored) {}
            }
        }
    }

    private void save() {
        data.set("last-reset", lastReset.toString());
        data.set("players", null);
        for (UUID id : highestCleared.keySet()) {
            data.set("players." + id + ".highest", highestCleared.getOrDefault(id, 0));
            data.set("players." + id + ".sweeps", sweepsUsedToday.getOrDefault(id, 0));
        }
        try { data.save(dataFile); } catch (IOException ignored) {}
    }

    private record ActiveChallenge(int stage, Location returnLocation, UUID dummyId, int failTaskId) {}
    private record LocalDateMarker(int year, int month, int day) {
        static LocalDateMarker todayUtc() { Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC")); return new LocalDateMarker(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH)); }
        static LocalDateMarker parse(String s) { try { String[] p = s.split("-"); return new LocalDateMarker(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])); } catch (Exception e) { return todayUtc(); } }
        public String toString() { return year + "-" + month + "-" + day; }
    }
}
