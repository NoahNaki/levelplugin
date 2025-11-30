package me.nakilex.levelplugin.dungeon.verified;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonLayout;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.mob.utils.MythicMobModifier;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class CrimsonReliquaryDungeon implements VerifiedDungeonDefinition {
    private static final String DISPLAY = "Crimson Reliquary";
    private static final String KEY = DungeonManager.normalizeKey(DISPLAY);
    private static final String SOURCE_WORLD = "assets";
    private static final int MIN_X = -245;
    private static final int MIN_Y = -57;
    private static final int MIN_Z = -5936;
    private static final int MAX_X = 138;
    private static final int MAX_Y = 107;
    private static final int MAX_Z = -5387;

    private final Main plugin;

    public CrimsonReliquaryDungeon(Main plugin) {
        this.plugin = plugin;
    }

    private static final class TemplateMarkers {
        private final List<Location> bossMarkers = new ArrayList<>();
        private final List<Location> miniBossMarkers = new ArrayList<>();
        private final List<Location> normalMarkers = new ArrayList<>();
        private final List<Location> yellowFlowers = new ArrayList<>();
        private final List<Location> bluePlacements = new ArrayList<>();
        private final List<Location> brownRewards = new ArrayList<>();
        private final List<Location> chestMarkers = new ArrayList<>();
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY;
    }

    @Override
    public void register(DungeonManager manager) {
        DungeonLayout layout = new DungeonLayout();
        layout.setStep(0);
        manager.registerVerifiedLayout(KEY, DISPLAY, 10, layout);
    }

    @Override
    public void startInstance(DungeonManager manager, Player player) {
        plugin.getWorldManager().ensureWorldsLoaded(SOURCE_WORLD);

        World source = Bukkit.getWorld(SOURCE_WORLD);
        if (source == null) {
            player.sendMessage(Component.text("Verified dungeon template world is missing (assets).", NamedTextColor.RED));
            return;
        }

        String worldName = "dgn_verified_" + System.currentTimeMillis();
        World world = manager.createVoidWorld(worldName);
        if (world == null) return;

        Dungeon dungeon = new Dungeon(world, KEY);
        DungeonManager.Instance inst = manager.createTrackedInstance(dungeon, KEY, world);

        List<Player> participants = new ArrayList<>();
        me.nakilex.levelplugin.party.PartyManager pm = plugin.getPartyManager();
        me.nakilex.levelplugin.party.Party party = pm.getParty(player.getUniqueId());
        if (party != null && party.isLeader(player.getUniqueId())) {
            for (UUID id : party.getMembers()) {
                Player mem = Bukkit.getPlayer(id);
                if (mem != null && mem.isOnline()) {
                    participants.add(mem);
                    inst.addReturnLocation(id, mem.getLocation());
                }
            }
        } else {
            participants.add(player);
            inst.addReturnLocation(player.getUniqueId(), player.getLocation());
        }

        for (Player p : participants) {
            ChatMessageUtil.send(p, MessageType.INFO, "Preparing Crimson Reliquary instance. Please wait...");
        }

        int width = MAX_X - MIN_X + 1;
        int height = MAX_Y - MIN_Y + 1;
        int depth = MAX_Z - MIN_Z + 1;
        int offsetX = 0;
        int offsetZ = 0;
        int offsetY = 64 - MIN_Y;
        Location origin = new Location(world, offsetX, offsetY, offsetZ);

        int minX = offsetX;
        int minY = offsetY;
        int minZ = offsetZ;
        int maxX = offsetX + width - 1;
        int maxY = offsetY + height - 1;
        int maxZ = offsetZ + depth - 1;
        Location center = origin.clone().add((width - 1) / 2.0, 1, (depth - 1) / 2.0);
        Dungeon.RoomInstance bounds = new Dungeon.RoomInstance(null, 0, center, minX, minY, minZ, maxX, maxY, maxZ, null, List.of(), null);
        dungeon.addRoom(bounds);

        world.setDifficulty(org.bukkit.Difficulty.HARD);
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        world.setSpawnLocation(center);

        TemplateMarkers markers = new TemplateMarkers();
        copyTemplateAsync(source, world, origin, markers, () -> finalizeInstance(manager, inst, origin, center, participants, markers));
    }

    private void copyTemplateAsync(World source, World dest, Location origin, TemplateMarkers markers, Runnable done) {
        int minChunkX = Math.floorDiv(MIN_X, 16);
        int maxChunkX = Math.floorDiv(MAX_X, 16);
        int minChunkZ = Math.floorDiv(MIN_Z, 16);
        int maxChunkZ = Math.floorDiv(MAX_Z, 16);

        List<int[]> chunkQueue = new ArrayList<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                chunkQueue.add(new int[]{cx, cz});
            }
        }

        final int chunksPerTick = 1;
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int processed = 0;
            while (processed < chunksPerTick && !chunkQueue.isEmpty()) {
                int[] pair = chunkQueue.remove(0);
                processChunk(source, dest, origin, markers, pair[0], pair[1]);
                processed++;
            }

            if (chunkQueue.isEmpty()) {
                task.cancel();
                done.run();
            }
        }, 0L, 1L);
    }

    private void processChunk(World source, World dest, Location origin, TemplateMarkers markers, int chunkX, int chunkZ) {
        var srcChunk = source.getChunkAt(chunkX, chunkZ);
        srcChunk.load();

        int destChunkX = Math.floorDiv(origin.getBlockX() + (chunkX << 4) - MIN_X, 16);
        int destChunkZ = Math.floorDiv(origin.getBlockZ() + (chunkZ << 4) - MIN_Z, 16);
        var dstChunk = dest.getChunkAt(destChunkX, destChunkZ);
        dstChunk.load();

        var snapshot = srcChunk.getChunkSnapshot();

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        for (int x = 0; x < 16; x++) {
            int worldX = baseX + x;
            if (worldX < MIN_X || worldX > MAX_X) continue;
            for (int z = 0; z < 16; z++) {
                int worldZ = baseZ + z;
                if (worldZ < MIN_Z || worldZ > MAX_Z) continue;
                for (int y = MIN_Y; y <= MAX_Y; y++) {
                    Material mat = snapshot.getBlockType(x, y, z);
                    if (mat == Material.AIR) continue;

                    int destX = origin.getBlockX() + (worldX - MIN_X);
                    int destY = origin.getBlockY() + (y - MIN_Y);
                    int destZ = origin.getBlockZ() + (worldZ - MIN_Z);
                    Location destLoc = new Location(dest, destX, destY, destZ);
                    switch (mat) {
                        case BLACK_WOOL -> {
                            markers.bossMarkers.add(destLoc.clone().add(0.5, 0, 0.5));
                            dest.getBlockAt(destLoc).setType(Material.AIR, false);
                        }
                        case CYAN_WOOL -> {
                            markers.miniBossMarkers.add(destLoc.clone().add(0.5, 0, 0.5));
                            dest.getBlockAt(destLoc).setType(Material.AIR, false);
                        }
                        case MAGENTA_WOOL -> {
                            markers.normalMarkers.add(destLoc.clone().add(0.5, 0, 0.5));
                            dest.getBlockAt(destLoc).setType(Material.AIR, false);
                        }
                        case YELLOW_WOOL -> {
                            markers.yellowFlowers.add(destLoc);
                            dest.getBlockAt(destLoc).setType(Material.AIR, false);
                        }
                        case BLUE_WOOL -> {
                            markers.bluePlacements.add(destLoc);
                            dest.getBlockAt(destLoc).setType(Material.AIR, false);
                        }
                        case BROWN_WOOL -> {
                            markers.brownRewards.add(destLoc);
                            dest.getBlockAt(destLoc).setType(Material.AIR, false);
                        }
                        case CHEST, TRAPPED_CHEST -> {
                            markers.chestMarkers.add(destLoc);
                            dest.getBlockAt(destLoc).setType(Material.AIR, false);
                        }
                        default -> {
                            dest.getBlockAt(destLoc).setBlockData(snapshot.getBlockData(x, y, z), false);
                        }
                    }
                }
            }
        }
    }

    private void finalizeInstance(DungeonManager manager,
                                  DungeonManager.Instance inst,
                                  Location origin,
                                  Location center,
                                  List<Player> participants,
                                  TemplateMarkers markers) {
        class State { boolean allowFlight; boolean flying; boolean invul; State(Player p){allowFlight=p.getAllowFlight();flying=p.isFlying();invul=p.isInvulnerable();}}
        java.util.Map<Player, State> prev = new java.util.HashMap<>();
        for (Player p : participants) {
            prev.put(p, new State(p));
            if (p.isOnline()) {
                p.setAllowFlight(true);
                p.setFlying(true);
                p.setInvulnerable(true);
                p.teleport(center);
            }
        }

        List<Material> flowerTypes = Arrays.asList(
                Material.POPPY,
                Material.DANDELION,
                Material.BLUE_ORCHID,
                Material.ALLIUM
        );
        for (Location yellow : markers.yellowFlowers) {
            Material choice = flowerTypes.get(ThreadLocalRandom.current().nextInt(flowerTypes.size()));
            yellow.getBlock().setType(choice, false);
        }
        for (Location marker : markers.bluePlacements) {
            marker.getBlock().setType(Material.LIGHT_BLUE_GLAZED_TERRACOTTA, false);
            marker.getWorld().spawn(marker.clone().add(0.5, 0.1, 0.5), org.bukkit.entity.ArmorStand.class, as -> {
                as.setVisible(false);
                as.setGravity(false);
                as.setMarker(true);
                as.customName(net.kyori.adventure.text.Component.text("Place Flower"));
                as.setCustomNameVisible(true);
                as.addScoreboardTag("dungeon_flower_slot");
            });
        }

        for (Location brown : markers.brownRewards) {
            brown.getBlock().setType(Material.SOUL_SAND, false);
        }

        int tier = Math.max(1, manager.getThreatLevel(KEY));
        for (Location chest : markers.chestMarkers) {
            int id = manager.getLootChestManager().createAndSpawnChest(chest, tier);
            inst.addChestId(id);
        }

        for (Location magenta : markers.normalMarkers) {
            String[] mobs = new String[]{
                    "Nocsy_Bokoblin_Shaman",
                    "Nocsy_Bokoblin_Swordsman",
                    "Nocsy_Bokoblin_Warrior"
            };
            String mob = mobs[ThreadLocalRandom.current().nextInt(mobs.length)];
            MythicMobModifier.spawnModifiedMob(mob, magenta, null, null, null, null);
        }
        for (Location cyan : markers.miniBossMarkers) {
            MythicMobModifier.spawnModifiedMob("Nocsy_Ganon", cyan, null, null, null, null);
        }
        for (Location boss : markers.bossMarkers) {
            var mob = MythicMobModifier.spawnModifiedMob("MSO_Demon_General", boss, null, null, null, null);
            if (mob != null) {
                mob.getEntity().getBukkitEntity().addScoreboardTag("dungeon_boss");
            }
        }

        for (int y = -35; y <= -31; y++) {
            for (int z = -5803; z <= -5798; z++) {
                Location dest = origin.clone().add(-222 - MIN_X, y - MIN_Y, z - MIN_Z);
                dest.getBlock().setType(Material.NETHER_PORTAL, false);
            }
        }

        for (Player p : participants) {
            State st = prev.get(p);
            if (st != null && p.isOnline()) {
                p.setInvulnerable(st.invul);
                p.setAllowFlight(st.allowFlight);
                p.setFlying(st.allowFlight && st.flying);
                ChatMessageUtil.send(p, MessageType.SUCCESS, "Crimson Reliquary is ready.");
            }
        }
    }
}
