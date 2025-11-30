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

        int width = MAX_X - MIN_X + 1;
        int height = MAX_Y - MIN_Y + 1;
        int depth = MAX_Z - MIN_Z + 1;
        int offsetX = 0;
        int offsetZ = 0;
        int offsetY = 64 - MIN_Y;
        Location origin = new Location(world, offsetX, offsetY, offsetZ);

        List<Location> bossMarkers = new ArrayList<>();
        List<Location> miniBossMarkers = new ArrayList<>();
        List<Location> normalMarkers = new ArrayList<>();
        List<Location> yellowFlowers = new ArrayList<>();
        List<Location> bluePlacements = new ArrayList<>();
        List<Location> brownRewards = new ArrayList<>();
        List<Location> chestMarkers = new ArrayList<>();

        for (int x = MIN_X; x <= MAX_X; x++) {
            for (int y = MIN_Y; y <= MAX_Y; y++) {
                for (int z = MIN_Z; z <= MAX_Z; z++) {
                    Location sourceLoc = new Location(source, x, y, z);
                    Material mat = sourceLoc.getBlock().getType();
                    Location destLoc = origin.clone().add(x - MIN_X, y - MIN_Y, z - MIN_Z);

                    switch (mat) {
                        case BLACK_WOOL -> bossMarkers.add(destLoc.clone().add(0.5, 0, 0.5));
                        case CYAN_WOOL -> miniBossMarkers.add(destLoc.clone().add(0.5, 0, 0.5));
                        case MAGENTA_WOOL -> normalMarkers.add(destLoc.clone().add(0.5, 0, 0.5));
                        case YELLOW_WOOL -> yellowFlowers.add(destLoc);
                        case BLUE_WOOL -> bluePlacements.add(destLoc);
                        case BROWN_WOOL -> brownRewards.add(destLoc);
                        case CHEST, TRAPPED_CHEST -> {
                            chestMarkers.add(destLoc);
                            continue;
                        }
                        default -> {
                            var data = sourceLoc.getBlock().getBlockData().clone();
                            world.getBlockAt(destLoc).setBlockData(data, false);
                            continue;
                        }
                    }
                    world.getBlockAt(destLoc).setType(Material.AIR, false);
                }
            }
        }

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

        class State { boolean allowFlight; boolean flying; boolean invul; State(Player p){allowFlight=p.getAllowFlight();flying=p.isFlying();invul=p.isInvulnerable();}}
        java.util.Map<Player, State> prev = new java.util.HashMap<>();
        for (Player p : participants) {
            prev.put(p, new State(p));
            p.setAllowFlight(true);
            p.setFlying(true);
            p.setInvulnerable(true);
            p.teleport(center);
        }

        List<Material> flowerTypes = Arrays.asList(
                Material.POPPY,
                Material.DANDELION,
                Material.BLUE_ORCHID,
                Material.ALLIUM
        );
        for (Location yellow : yellowFlowers) {
            Material choice = flowerTypes.get(ThreadLocalRandom.current().nextInt(flowerTypes.size()));
            yellow.getBlock().setType(choice, false);
        }
        for (Location marker : bluePlacements) {
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

        for (Location brown : brownRewards) {
            brown.getBlock().setType(Material.SOUL_SAND, false);
        }

        int tier = manager.getThreatLevel(KEY);
        for (Location chest : chestMarkers) {
            int id = manager.getLootChestManager().createAndSpawnChest(chest, tier);
            inst.addChestId(id);
        }

        for (Location magenta : normalMarkers) {
            String[] mobs = new String[]{
                    "Nocsy_Bokoblin_Shaman",
                    "Nocsy_Bokoblin_Swordsman",
                    "Nocsy_Bokoblin_Warrior"
            };
            String mob = mobs[ThreadLocalRandom.current().nextInt(mobs.length)];
            MythicMobModifier.spawnModifiedMob(mob, magenta, null, null, null, null);
        }
        for (Location cyan : miniBossMarkers) {
            MythicMobModifier.spawnModifiedMob("Nocsy_Ganon", cyan, null, null, null, null);
        }
        for (Location boss : bossMarkers) {
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
            }
        }
    }
}
