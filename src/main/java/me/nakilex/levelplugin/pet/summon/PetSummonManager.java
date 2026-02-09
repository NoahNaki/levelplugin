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
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
    private static final int GLOW_DELAY_TICKS = 30;
    private static final int ORBIT_TICKS = 60;
    private static final int END_BUFFER_TICKS = 20;
    private static final double BASE_OFFSET = 6.0;
    private static final double ORBIT_RADIUS = 3.6;
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

        Location base = summonBaseLocation(player);
        List<Location> slots = buildDisplaySlots(detailed.pulls().size(), base);
        for (int i = 0; i < detailed.pulls().size(); i++) {
            PetPullEntry entry = detailed.pulls().get(i);
            Location slot = slots.get(Math.min(i, slots.size() - 1));
            int spawnDelay = SPAWN_DELAY_TICKS + i * SPAWN_INTERVAL_TICKS;
            scheduleSpawn(player, session, entry, slot, spawnDelay, i);
        }

        int totalTicks = calculateTotalDuration(detailed.pulls().size());
        session.tasks.add(Bukkit.getScheduler().runTaskLater(plugin,
                () -> finishSession(player, true), totalTicks));
        session.tasks.add(Bukkit.getScheduler().runTaskTimer(plugin,
                () -> checkCutsceneState(player), 20L, 20L));
    }

    private void scheduleSpawn(Player player,
                               SummonSession session,
                               PetPullEntry entry,
                               Location slot,
                               int delay,
                               int index) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player == null || !player.isOnline()) {
                return;
            }
            Item item = spawnSummonItem(slot, entry.definition());
            session.spawned.add(item);
            applyVisibility(player, item);
            double phase = index * Math.PI / 6.0;
            startOrbitAnimation(player, item, slot, phase, session);
            playSound(player, SPAWN_SOUND, SPAWN_VOLUME, SPAWN_PITCH);
            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> {
                        applyGlow(item, entry.definition());
                        playSound(player, REVEAL_SOUND, REVEAL_VOLUME, REVEAL_PITCH);
                    }, GLOW_DELAY_TICKS);
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

    private void applyGlow(Item item, PetDefinition definition) {
        if (item == null || item.isDead()) {
            return;
        }
        ItemRarity rarity = definition.rarity();
        if (rarity != null) {
            GlowUtil.applyGlowWithColor(item, rarity.getColor());
        }
    }

    private List<Location> buildDisplaySlots(int amount, Location base) {
        int count = Math.max(1, amount);
        int columns = Math.min(5, count);
        int rows = (int) Math.ceil(count / (double) columns);
        double spacing = 2.8;
        double startX = -((columns - 1) * spacing) / 2.0;
        double startZ = -((rows - 1) * spacing) / 2.0;
        List<Location> slots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int row = i / columns;
            int col = i % columns;
            double offsetX = startX + col * spacing;
            double offsetZ = startZ + row * spacing;
            slots.add(base.clone().add(offsetX, 0.0, offsetZ));
        }
        return slots;
    }

    private Location summonBaseLocation(Player player) {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        Location base = new Location(world, SUMMON_X, SUMMON_Y, SUMMON_Z);
        Vector direction = player.getLocation().getDirection();
        if (direction.lengthSquared() < 0.001) {
            direction = new Vector(0, 0, 1);
        }
        Vector offset = direction.normalize().multiply(BASE_OFFSET);
        base.add(offset);
        base.add(0, 1.2, 0);
        return base;
    }

    private int calculateTotalDuration(int pulls) {
        int count = Math.max(1, pulls);
        return SPAWN_DELAY_TICKS + (count - 1) * SPAWN_INTERVAL_TICKS + ORBIT_TICKS + END_BUFFER_TICKS;
    }

    private void startOrbitAnimation(Player player, Item item, Location anchor, double phase, SummonSession session) {
        BukkitTask task = new org.bukkit.scheduler.BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= ORBIT_TICKS || item.isDead()) {
                    item.teleport(anchor);
                    cancel();
                    return;
                }
                double angle = tick * 0.35 + phase;
                double radius = ORBIT_RADIUS + 0.45 * Math.sin(tick * 0.15 + phase);
                double height = 0.4 + 0.04 * tick + 0.35 * Math.sin(tick * 0.12 + phase);
                Location target = anchor.clone().add(
                        Math.cos(angle) * radius,
                        height,
                        Math.sin(angle) * radius);
                item.teleport(target);
                player.spawnParticle(Particle.PORTAL, target, 6, 0.1, 0.1, 0.1, 0.01);
                player.spawnParticle(Particle.END_ROD, target, 1, 0.05, 0.15, 0.05, 0.0);
                if (tick % 12 == 0) {
                    playSound(player, ORBIT_SOUND, ORBIT_VOLUME, ORBIT_PITCH);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        session.tasks.add(task);
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
        if (!player.isOnline() || !cutsceneManager.isInCutscene(player)) {
            finishSession(player, player.isOnline());
        }
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
        private final List<Item> spawned = new ArrayList<>();
        private final List<BukkitTask> tasks = new ArrayList<>();

        private SummonSession(Location returnLocation, PetPullDetailed pulls) {
            this.returnLocation = returnLocation;
            this.pulls = pulls;
        }
    }
}
