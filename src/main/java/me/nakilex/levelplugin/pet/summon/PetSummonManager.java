package me.nakilex.levelplugin.pet.summon;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cutscene.CutsceneManager;
import me.nakilex.levelplugin.items.data.ItemRarity;
import me.nakilex.levelplugin.pet.PetDefinition;
import me.nakilex.levelplugin.pet.PetManager;
import me.nakilex.levelplugin.pet.PetManager.PetPullDetailed;
import me.nakilex.levelplugin.pet.PetManager.PetPullEntry;
import me.nakilex.levelplugin.pet.utils.PetChatUtil;
import me.nakilex.levelplugin.pet.utils.PetDisplayUtil;
import me.nakilex.levelplugin.pet.utils.PetPullSummaryUtil;
import me.nakilex.levelplugin.utils.GlowUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PetSummonManager implements Listener {
    private static final String CUTSCENE_ID = "pet_pull";
    private static final int SPAWN_DELAY_TICKS = 20;
    private static final int SPAWN_INTERVAL_TICKS = 10;
    private static final int SPIN_TICKS = 120;
    private static final int REVEAL_START_DELAY_TICKS = 20;
    private static final int REVEAL_INTERVAL_TICKS = 14;
    private static final int END_BUFFER_TICKS = 20;
    private static final double BASE_OFFSET = 6.5;
    private static final double RING_RADIUS = 3.8;
    private static final double SPIN_TURNS = 3.5;
    private static final String SPAWN_SOUND = "minecraft:entity.experience_orb.pickup";
    private static final String REVEAL_SOUND = "minecraft:block.beacon.power_select";
    private static final String ORBIT_SOUND = "minecraft:block.amethyst_block.chime";
    private static final float SPAWN_VOLUME = 0.7f;
    private static final float SPAWN_PITCH = 1.3f;
    private static final float REVEAL_VOLUME = 0.9f;
    private static final float REVEAL_PITCH = 1.1f;
    private static final float ORBIT_VOLUME = 0.4f;
    private static final float ORBIT_PITCH = 1.6f;
    private static final double SUMMON_X = 234;
    private static final double SUMMON_Y = 177;
    private static final double SUMMON_Z = -203;

    private final Main plugin;
    private final PetManager petManager;
    private final CutsceneManager cutsceneManager;
    private final Map<UUID, SummonSession> sessions = new HashMap<>();

    public PetSummonManager(Main plugin, PetManager petManager, CutsceneManager cutsceneManager) {
        this.plugin = plugin;
        this.petManager = petManager;
        this.cutsceneManager = cutsceneManager;
    }

    public void startSummon(Player player, int amount) {
        if (player == null || petManager == null || cutsceneManager == null) {
            return;
        }
        if (amount <= 0) {
            PetChatUtil.send(player, "Invalid summon amount.");
            return;
        }
        if (sessions.containsKey(player.getUniqueId())) {
            PetChatUtil.send(player, "You are already summoning pets.");
            return;
        }
        if (cutsceneManager.isInCutscene(player)) {
            PetChatUtil.send(player, "Finish your current cutscene first.");
            return;
        }
        if (!cutsceneManager.listCutscenes().contains(CUTSCENE_ID)) {
            PetChatUtil.send(player, "Pet summon cutscene is unavailable.");
            return;
        }

        Location returnLocation = player.getLocation().clone();
        petManager.setPendingSummonReturn(player.getUniqueId(), returnLocation);

        PetPullDetailed detailed = petManager.pullPetsDetailed(player, amount);
        if (detailed.kept().isEmpty() && detailed.discarded().isEmpty()) {
            PetChatUtil.send(player, "No pets available to pull.");
            petManager.clearPendingSummonReturn(player.getUniqueId());
            return;
        }

        SummonSession session = new SummonSession(returnLocation, detailed);
        sessions.put(player.getUniqueId(), session);

        cutsceneManager.playCutscene(player, CUTSCENE_ID);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            SummonSession active = sessions.get(player.getUniqueId());
            if (active != null) {
                active.updateOrientationFromPlayer(player);
                active.setFrozenLocation(player.getLocation());
            }
        }, 1L);

        for (int i = 0; i < detailed.pulls().size(); i++) {
            PetPullEntry entry = detailed.pulls().get(i);
            int spawnDelay = SPAWN_DELAY_TICKS + i * SPAWN_INTERVAL_TICKS;
            scheduleSpawn(player, session, entry, spawnDelay, i);
        }

        int totalTicks = calculateTotalDuration(session);
        session.tasks.add(Bukkit.getScheduler().runTaskLater(plugin,
                () -> tryFinishSummon(player, session), totalTicks));
        session.tasks.add(Bukkit.getScheduler().runTaskTimer(plugin,
                () -> checkCutsceneState(player), 20L, 20L));
    }

    private void scheduleSpawn(Player player,
                               SummonSession session,
                               PetPullEntry entry,
                               int delay,
                               int index) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player == null || !player.isOnline()) {
                return;
            }
            if (session.center == null) {
                session.updateOrientationFromPlayer(player);
            }
            if (session.center == null) {
                return;
            }
            Item item = spawnSummonItem(session.center, entry.definition());
            item.setCustomNameVisible(false);
            session.spawned.add(item);
            session.entries.add(new SummonEntry(item, entry.definition(), index));
            applyVisibility(player, item);
            startSpinTask(player, session);
            playSound(player, SPAWN_SOUND, SPAWN_VOLUME, SPAWN_PITCH);
        }, delay);
        session.tasks.add(task);
    }

    private Item spawnSummonItem(Location location, PetDefinition definition) {
        ItemStack stack = createSummonStack(definition);
        World world = location.getWorld();
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        Location spawn = location.clone();
        Item item = world.dropItem(spawn, stack);
        item.setGravity(false);
        item.setVelocity(new Vector(0, 0, 0));
        item.setPickupDelay(Integer.MAX_VALUE);
        item.setInvulnerable(true);
        item.setSilent(true);
        item.setCustomName(PetDisplayUtil.formatDisplayName(definition));
        item.setCustomNameVisible(true);
        return item;
    }

    private ItemStack createSummonStack(PetDefinition definition) {
        ItemStack stack = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(PetDisplayUtil.formatDisplayName(definition));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private void applyGlow(Player player, Item item, PetDefinition definition) {
        if (item == null || item.isDead()) {
            return;
        }
        ItemRarity rarity = definition.rarity();
        if (rarity != null) {
            var sbManager = plugin.getScoreboardManager();
            var board = sbManager != null ? sbManager.getBoard(player) : null;
            GlowUtil.applyGlowWithColor(item, rarity.getColor(), board);
        }
    }

    private int calculateTotalDuration(SummonSession session) {
        int count = session.totalPulls;
        return SPAWN_DELAY_TICKS
                + session.spinTicks
                + REVEAL_START_DELAY_TICKS
                + count * REVEAL_INTERVAL_TICKS
                + END_BUFFER_TICKS;
    }

    private void startSpinTask(Player player, SummonSession session) {
        if (session.spinTask != null) {
            return;
        }
        org.bukkit.scheduler.BukkitRunnable runnable = new org.bukkit.scheduler.BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= session.spinTicks) {
                    cancel();
                    session.spinTask = null;
                    scheduleReveal(player, session);
                    return;
                }
                double progress = tick / (double) session.spinTicks;
                double eased = 1.0 - Math.pow(1.0 - progress, 3);
                session.spinProgress = eased;
                double baseAngle = eased * (Math.PI * 2.0 * SPIN_TURNS);
                updateRingPositions(player, session, baseAngle);
                if (tick % 12 == 0) {
                    playSound(player, ORBIT_SOUND, ORBIT_VOLUME, ORBIT_PITCH);
                }
                tick++;
            }
        };
        session.spinTask = runnable;
        session.tasks.add(runnable.runTaskTimer(plugin, 0L, 1L));
    }

    private void updateRingPositions(Player player, SummonSession session, double baseAngle) {
        int count = Math.max(1, session.totalPulls);
        if (session.center == null || session.right == null || session.up == null) {
            return;
        }
        double radius = RING_RADIUS * (0.25 + 0.75 * session.spinProgress);
        for (int i = 0; i < session.entries.size(); i++) {
            SummonEntry entry = session.entries.get(i);
            if (entry.item().isDead()) {
                continue;
            }
            double offset = count == 1 ? 0.0 : (Math.PI * 2.0 * entry.index() / count);
            double theta = baseAngle + offset;
            Vector right = session.right;
            Vector up = session.up;
            Location target = session.center.clone()
                    .add(right.clone().multiply(Math.cos(theta) * radius))
                    .add(up.clone().multiply(Math.sin(theta) * radius));
            entry.item().teleport(target);
            player.spawnParticle(Particle.PORTAL, target, 4, 0.08, 0.08, 0.08, 0.01);
        }
    }

    private void scheduleReveal(Player player, SummonSession session) {
        for (int i = 0; i < session.entries.size(); i++) {
            SummonEntry entry = session.entries.get(i);
            int delay = REVEAL_START_DELAY_TICKS + i * REVEAL_INTERVAL_TICKS;
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (entry.item().isDead()) {
                    return;
                }
                entry.item().setCustomNameVisible(true);
                applyGlow(player, entry.item(), entry.definition());
                player.spawnParticle(Particle.END_ROD, entry.item().getLocation(),
                        10, 0.2, 0.2, 0.2, 0.02);
                playSound(player, REVEAL_SOUND, REVEAL_VOLUME, REVEAL_PITCH);
                session.revealsDone++;
                if (session.revealsDone >= session.totalReveals) {
                    Bukkit.getScheduler().runTaskLater(plugin,
                            () -> tryFinishSummon(player, session), END_BUFFER_TICKS);
                }
            }, delay);
            session.tasks.add(task);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        SummonSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        Location frozen = session.frozenLocation();
        if (frozen == null) {
            session.setFrozenLocation(event.getFrom());
            return;
        }
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (to.getX() != frozen.getX() || to.getY() != frozen.getY() || to.getZ() != frozen.getZ()
                || to.getYaw() != frozen.getYaw() || to.getPitch() != frozen.getPitch()) {
            event.setTo(frozen.clone());
        }
    }

    private void applyVisibility(Player viewer, Item item) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.equals(viewer)) {
                    player.showEntity(plugin, item);
                } else {
                    player.hideEntity(plugin, item);
                }
            }
        }, 1L);
    }

    private void playSound(Player player, String sound, float volume, float pitch) {
        if (player == null || sound == null || sound.isBlank()) {
            return;
        }
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void checkCutsceneState(Player player) {
        if (player == null) {
            return;
        }
        SummonSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (!player.isOnline()) {
            finishSession(player, false);
            return;
        }
        if (!cutsceneManager.isInCutscene(player)) {
            if (session.revealsDone < session.totalReveals) {
                forceRevealAll(player, session);
            }
            finishSession(player, true);
        }
    }

    private void forceRevealAll(Player player, SummonSession session) {
        for (SummonEntry entry : session.entries) {
            if (entry.item().isDead()) {
                continue;
            }
            entry.item().setCustomNameVisible(true);
            applyGlow(player, entry.item(), entry.definition());
            player.spawnParticle(Particle.END_ROD, entry.item().getLocation(),
                    10, 0.2, 0.2, 0.2, 0.02);
        }
        session.revealsDone = session.totalReveals;
    }

    private void tryFinishSummon(Player player, SummonSession session) {
        if (player == null || session == null) {
            return;
        }
        SummonSession active = sessions.get(player.getUniqueId());
        if (active != session) {
            return;
        }
        if (session.revealsDone < session.totalReveals) {
            return;
        }
        finishSession(player, true);
    }

    private void finishSession(Player player, boolean teleport) {
        if (player == null) {
            return;
        }
        SummonSession session = sessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (cutsceneManager.isInCutscene(player)) {
            cutsceneManager.stopCutscene(player);
        }
        for (BukkitTask task : session.tasks) {
            task.cancel();
        }
        for (Item item : session.spawned) {
            if (item != null && !item.isDead()) {
                item.remove();
            }
        }
        if (teleport) {
            Location returnLocation = session.returnLocation;
            if (returnLocation != null) {
                player.teleport(returnLocation);
            }
            petManager.clearPendingSummonReturn(player.getUniqueId());
            PetChatUtil.send(player, "Pet summons complete.");
            PetPullSummaryUtil.sendSummary(player, "Pulled", session.pulls.kept());
            PetPullSummaryUtil.sendSummary(player, "Auto-discarded", session.pulls.discarded());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        SummonSession session = sessions.get(player.getUniqueId());
        if (session != null) {
            finishSession(player, false);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location pending = petManager.getPendingSummonReturn(player.getUniqueId());
        if (pending == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.teleport(pending);
            petManager.clearPendingSummonReturn(player.getUniqueId());
            PetChatUtil.send(player, "Your pet summon was interrupted; you were returned safely.");
        });
    }

    private static class SummonSession {
        private final Location returnLocation;
        private final PetPullDetailed pulls;
        private final List<SummonEntry> entries = new ArrayList<>();
        private final List<Item> spawned = new ArrayList<>();
        private final List<BukkitTask> tasks = new ArrayList<>();
        private final int totalPulls;
        private final int totalReveals;
        private final int spinTicks;
        private Location center;
        private Vector right;
        private Vector up;
        private Location frozenLocation;
        private org.bukkit.scheduler.BukkitRunnable spinTask;
        private double spinProgress;
        private int revealsDone;

        private SummonSession(Location returnLocation,
                              PetPullDetailed pulls) {
            this.returnLocation = returnLocation;
            this.pulls = pulls;
            this.totalPulls = Math.max(1, pulls.pulls().size());
            this.totalReveals = this.totalPulls;
            this.center = null;
            this.right = null;
            this.up = null;
            this.spinTicks = SPIN_TICKS + (this.totalPulls - 1) * SPAWN_INTERVAL_TICKS;
            this.frozenLocation = null;
            this.spinProgress = 0.0;
            this.revealsDone = 0;
        }

        private void updateOrientationFromPlayer(Player player) {
            if (player == null) {
                return;
            }
            Location base = new Location(player.getWorld(), SUMMON_X, SUMMON_Y, SUMMON_Z);
            Vector direction = player.getLocation().getDirection();
            if (direction.lengthSquared() < 0.001) {
                direction = new Vector(0, 0, 1);
            }
            Vector forward = direction.normalize();
            Vector up = new Vector(0, 1, 0);
            Vector right = forward.clone().crossProduct(up).normalize();
            if (right.lengthSquared() < 0.001) {
                right = new Vector(1, 0, 0);
            }
            Vector offset = forward.clone().multiply(BASE_OFFSET);
            base.add(offset);
            base.add(0, 1.2, 0);
            this.center = base;
            this.right = right;
            this.up = up;
        }

        private Location frozenLocation() {
            return frozenLocation == null ? null : frozenLocation.clone();
        }

        private void setFrozenLocation(Location location) {
            frozenLocation = location == null ? null : location.clone();
        }
    }

    private record SummonEntry(Item item, PetDefinition definition, int index) {}
}
