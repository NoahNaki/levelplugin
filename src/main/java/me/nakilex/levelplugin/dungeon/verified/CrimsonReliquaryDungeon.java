package me.nakilex.levelplugin.dungeon.verified;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.Dungeon;
import me.nakilex.levelplugin.dungeon.DungeonLayout;
import me.nakilex.levelplugin.dungeon.DungeonManager;
import me.nakilex.levelplugin.dungeon.RoomTemplate;
import me.nakilex.levelplugin.mob.utils.MythicMobModifier;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import me.nakilex.levelplugin.utils.MultiLineHologram;
import me.nakilex.levelplugin.utils.TooltipUtil;
import me.nakilex.levelplugin.utils.items.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CrimsonReliquaryDungeon implements VerifiedDungeonDefinition {
    private static final String DISPLAY = "Crimson Reliquary";
    private static final String KEY = DungeonManager.normalizeKey(DISPLAY);
    private static final String SOURCE_WORLD = "flatland";
    private static final int MIN_X = -245;
    private static final int MIN_Y = -57;
    private static final int MIN_Z = -5936;
    private static final int MAX_X = 138;
    private static final int MAX_Y = 107;
    private static final int MAX_Z = -5387;
    private static final Location TEMPLATE_SPAWN = new Location(null, -211, -34, -5801);
    private static final NamespacedKey DUNGEON_FLOWER_KEY = new NamespacedKey(Main.getInstance(), "crimson_flower");

    private final Main plugin;
    private final Map<World, InstanceState> activeInstances = new HashMap<>();
    private volatile RoomTemplate cachedTemplate;

    public CrimsonReliquaryDungeon(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(new InteractionListener(), plugin);
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

    private static final class MobMarker {
        private final Location loc;
        private final String mobId;
        private boolean spawned;

        private MobMarker(Location loc, String mobId) {
            this.loc = loc;
            this.mobId = mobId;
        }
    }

    private enum FlowerType {
        POPPY(Material.POPPY, "Scarlet Poppy", "Blood-red petals hide a subtle glow."),
        DANDELION(Material.DANDELION, "Sunlit Dandelion", "Warm to the touch despite the chill."),
        BLUE_ORCHID(Material.BLUE_ORCHID, "Azure Orchid", "Its perfume hums with latent mana."),
        ALLIUM(Material.ALLIUM, "Amethyst Allium", "A violet bloom that refuses to wilt.");

        final Material block;
        final String display;
        final String description;

        FlowerType(Material block, String display, String description) {
            this.block = block;
            this.display = display;
            this.description = description;
        }
    }

    private static final class InstanceState {
        final Map<Location, FlowerType> pluckable = new HashMap<>();
        final Map<Location, FlowerType> placements = new HashMap<>();
        final Map<Location, MultiLineHologram> pluckHolograms = new HashMap<>();
        final List<Location> rewardFountains = new ArrayList<>();
        final List<Player> participants = new ArrayList<>();
        final List<MobMarker> mobMarkers = new ArrayList<>();
        boolean puzzleComplete;
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

    private RoomTemplate getTemplate() {
        if (cachedTemplate != null) {
            return cachedTemplate;
        }
        plugin.getWorldManager().ensureWorldsLoaded(SOURCE_WORLD);
        World source = Bukkit.getWorld(SOURCE_WORLD);
        if (source == null) {
            return null;
        }
        cachedTemplate = RoomTemplate.capture(source, MIN_X, MIN_Y, MIN_Z, MAX_X, MAX_Y, MAX_Z, false);
        return cachedTemplate;
    }

    @Override
    public void startInstance(DungeonManager manager, Player player) {
        RoomTemplate template = getTemplate();
        if (template == null) {
            player.sendMessage(Component.text("Verified dungeon template world is missing (flatland).", NamedTextColor.RED));
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
        Location spawn = origin.clone().add(TEMPLATE_SPAWN.getX() - MIN_X + 0.5, TEMPLATE_SPAWN.getY() - MIN_Y, TEMPLATE_SPAWN.getZ() - MIN_Z + 0.5);
        Dungeon.RoomInstance bounds = new Dungeon.RoomInstance(null, 0, spawn, minX, minY, minZ, maxX, maxY, maxZ, null, List.of(), null);
        dungeon.addRoom(bounds);

        world.setDifficulty(org.bukkit.Difficulty.HARD);
        world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        world.setSpawnLocation(spawn);

        TemplateMarkers markers = new TemplateMarkers();
        pasteTemplateAsync(template, world, origin, markers, () -> finalizeInstance(manager, inst, origin, spawn, participants, markers));
    }

    private void handleTemplateBlock(World dest, Location destLoc, BlockData data, TemplateMarkers markers) {
        Material mat = data.getMaterial();
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
            default -> dest.getBlockAt(destLoc).setBlockData(data, false);
        }
    }

    private void pasteTemplateAsync(RoomTemplate template, World dest, Location origin, TemplateMarkers markers, Runnable done) {
        for (RoomTemplate.ChestMarker chest : template.getChests()) {
            Location loc = origin.clone().add(chest.x, chest.y, chest.z);
            markers.chestMarkers.add(loc);
        }

        List<RoomTemplate.BlockDef> blocks = template.getBlocks();
        Map<Long, List<RoomTemplate.BlockDef>> byChunk = blocks.stream()
                .collect(Collectors.groupingBy(b -> chunkKey(origin, b)));

        final int chunksPerTick = 6;
        new BukkitRunnable() {
            final List<Map.Entry<Long, List<RoomTemplate.BlockDef>>> chunkEntries = new ArrayList<>(byChunk.entrySet());
            int idx = 0;

            @Override
            public void run() {
                int processedChunks = 0;
                while (idx < chunkEntries.size() && processedChunks < chunksPerTick) {
                    Map.Entry<Long, List<RoomTemplate.BlockDef>> entry = chunkEntries.get(idx++);
                    for (RoomTemplate.BlockDef b : entry.getValue()) {
                        Location destLoc = origin.clone().add(b.x, b.y, b.z);
                        handleTemplateBlock(dest, destLoc, b.data, markers);
                    }
                    processedChunks++;
                }
                if (idx >= chunkEntries.size()) {
                    cancel();
                    done.run();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private long chunkKey(Location origin, RoomTemplate.BlockDef b) {
        int cx = (origin.getBlockX() + b.x) >> 4;
        int cz = (origin.getBlockZ() + b.z) >> 4;
        return (((long) cx) << 32) ^ (cz & 0xffffffffL);
    }

    private void finalizeInstance(DungeonManager manager,
                                  DungeonManager.Instance inst,
                                  Location origin,
                                  Location spawn,
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
                p.teleport(spawn);
            }
        }
        InstanceState state = new InstanceState();
        state.participants.addAll(participants);
        activeInstances.put(origin.getWorld(), state);

        List<FlowerType> flowers = new ArrayList<>(Arrays.asList(FlowerType.values()));
        int idx = 0;
        for (Location yellow : markers.yellowFlowers) {
            FlowerType choice = flowers.get(idx % flowers.size());
            if (idx >= flowers.size()) {
                choice = flowers.get(ThreadLocalRandom.current().nextInt(flowers.size()));
            }
            idx++;
            yellow.getBlock().setType(choice.block, false);
            state.pluckable.put(yellow, choice);
            MultiLineHologram holo = new MultiLineHologram(yellow.clone().add(0.5, 1.1, 0.5), "crimson_flower_pluck");
            holo.spawn(List.of(ChatColor.GRAY + "Right-click to pluck"));
            state.pluckHolograms.put(yellow, holo);
        }
        for (Location marker : markers.bluePlacements) {
            marker.getBlock().setType(Material.AIR, false);
            marker.getWorld().spawn(marker.clone().add(0.5, 0.1, 0.5), org.bukkit.entity.ArmorStand.class, as -> {
                as.setVisible(false);
                as.setGravity(false);
                as.setMarker(true);
                as.customName(net.kyori.adventure.text.Component.text("Place Flower"));
                as.setCustomNameVisible(true);
                as.addScoreboardTag("dungeon_flower_slot");
            });
            state.placements.put(marker, null);
        }

        for (Location brown : markers.brownRewards) {
            brown.getBlock().setType(Material.SOUL_SAND, false);
            state.rewardFountains.add(brown);
        }

        int tier = Math.max(1, manager.getThreatLevel(KEY));
        for (Location chest : markers.chestMarkers) {
            int id = manager.getLootChestManager().createAndSpawnChest(chest, tier, getFacingFromData(chest.getBlock().getBlockData()));
            inst.addChestId(id);
        }

        queueMobs(state, markers);
        startMobWatcher(state);

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

    private ItemStack createFlowerItem(FlowerType type) {
        ItemStack stack = new ItemStack(type.block);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + type.display);
            meta.setLore(TooltipUtil.dungeonItemLore(type.description, true));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(ItemUtil.SOULBOUND_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(DUNGEON_FLOWER_KEY, PersistentDataType.STRING, type.name());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private org.bukkit.block.BlockFace getFacingFromData(BlockData data) {
        if (data instanceof org.bukkit.block.data.Directional dir) {
            return dir.getFacing();
        }
        return org.bukkit.block.BlockFace.NORTH;
    }

    private void queueMobs(InstanceState state, TemplateMarkers markers) {
        String[] mobs = new String[]{
                "Nocsy_Bokoblin_Shaman",
                "Nocsy_Bokoblin_Swordsman",
                "Nocsy_Bokoblin_Warrior"
        };
        for (Location magenta : markers.normalMarkers) {
            String mob = mobs[ThreadLocalRandom.current().nextInt(mobs.length)];
            state.mobMarkers.add(new MobMarker(magenta, mob));
        }
        for (Location cyan : markers.miniBossMarkers) {
            state.mobMarkers.add(new MobMarker(cyan, "Nocsy_Ganon"));
        }
        for (Location boss : markers.bossMarkers) {
            state.mobMarkers.add(new MobMarker(boss, "MSO_Demon_General"));
        }
    }

    private void startMobWatcher(InstanceState state) {
        final double radiusSq = 24 * 24;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (state.participants.stream().noneMatch(p -> p != null && p.isOnline())) {
                    cancel();
                    return;
                }
                for (MobMarker marker : state.mobMarkers) {
                    if (marker.spawned) continue;
                    for (Player p : state.participants) {
                        if (p == null || !p.isOnline() || p.getWorld() != marker.loc.getWorld()) continue;
                        if (p.getLocation().distanceSquared(marker.loc) <= radiusSq) {
                            var mob = MythicMobModifier.spawnModifiedMob(marker.mobId, marker.loc, null, null, null, null);
                            if (mob != null && marker.mobId.equals("MSO_Demon_General")) {
                                mob.getEntity().getBukkitEntity().addScoreboardTag("dungeon_boss");
                            }
                            marker.spawned = true;
                            break;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private boolean isDungeonFlower(ItemStack stack) {
        if (stack == null) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        String type = meta.getPersistentDataContainer().get(DUNGEON_FLOWER_KEY, PersistentDataType.STRING);
        return type != null;
    }

    private FlowerType getFlowerType(ItemStack stack) {
        if (stack == null) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String type = meta.getPersistentDataContainer().get(DUNGEON_FLOWER_KEY, PersistentDataType.STRING);
        if (type == null) return null;
        try {
            return FlowerType.valueOf(type);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void completePuzzle(InstanceState state) {
        if (state.puzzleComplete) return;
        state.puzzleComplete = true;
        for (Player p : state.participants) {
            if (p != null && p.isOnline()) {
                ChatMessageUtil.send(p, MessageType.SUCCESS, "Puzzle complete!");
            }
        }
        for (Location fountain : state.rewardFountains) {
            new BukkitRunnable() {
                int drops = 0;

                @Override
                public void run() {
                    if (drops++ >= 8) {
                        cancel();
                        return;
                    }
                    ItemStack reward = switch (ThreadLocalRandom.current().nextInt(4)) {
                        case 0 -> new ItemStack(Material.DIAMOND_SWORD);
                        case 1 -> new ItemStack(Material.GOLDEN_APPLE, 2);
                        case 2 -> new ItemStack(Material.EMERALD, 6);
                        default -> new ItemStack(Material.POTION);
                    };
                    Vector vel = new Vector(ThreadLocalRandom.current().nextDouble(-0.3, 0.3),
                            0.4 + ThreadLocalRandom.current().nextDouble(0.1, 0.3),
                            ThreadLocalRandom.current().nextDouble(-0.3, 0.3));
                    fountain.getWorld().dropItem(fountain.clone().add(0.5, 1, 0.5), reward).setVelocity(vel);
                }
            }.runTaskTimer(plugin, 0L, 6L);
        }
    }

    private void removeDungeonItems(Player player) {
        if (player == null) return;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isDungeonFlower(stack)) {
                player.getInventory().clear(i);
            }
        }
    }

    private class InteractionListener implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void onInteract(PlayerInteractEvent event) {
            if (event.getHand() == EquipmentSlot.OFF_HAND) return;
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
            if (event.getClickedBlock() == null) return;
            World world = event.getClickedBlock().getWorld();
            InstanceState state = activeInstances.get(world);
            if (state == null) return;
            Location loc = event.getClickedBlock().getLocation();
            FlowerType type = state.pluckable.get(loc);
            if (type == null) return;
            event.setCancelled(true);
            ItemStack reward = createFlowerItem(type);
            Map<Integer, ItemStack> overflow = event.getPlayer().getInventory().addItem(reward);
            overflow.values().forEach(item -> world.dropItemNaturally(event.getPlayer().getLocation(), item));
            event.getClickedBlock().setType(Material.AIR, false);
            MultiLineHologram holo = state.pluckHolograms.remove(loc);
            if (holo != null) holo.despawn();
            state.pluckable.remove(loc);
            ChatMessageUtil.send(event.getPlayer(), MessageType.SUCCESS, "You pluck the flower.");
        }

        @EventHandler(ignoreCancelled = true)
        public void onInteractEntity(PlayerInteractAtEntityEvent event) {
            if (event.getHand() == EquipmentSlot.OFF_HAND) return;
            if (!event.getRightClicked().getScoreboardTags().contains("dungeon_flower_slot")) return;
            World world = event.getRightClicked().getWorld();
            InstanceState state = activeInstances.get(world);
            if (state == null) return;
            event.setCancelled(true);
            ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
            if (!isDungeonFlower(held)) {
                ChatMessageUtil.send(event.getPlayer(), MessageType.ERROR, "Hold a dungeon flower to place it.");
                return;
            }
            FlowerType type = getFlowerType(held);
            if (type == null) {
                ChatMessageUtil.send(event.getPlayer(), MessageType.ERROR, "That flower has faded.");
                return;
            }
            Location base = event.getRightClicked().getLocation().getBlock().getLocation();
            if (!state.placements.containsKey(base)) {
                return;
            }
            base.getBlock().setType(type.block, false);
            event.getRightClicked().remove();
            held.setAmount(held.getAmount() - 1);
            state.placements.put(base, type);
            ChatMessageUtil.send(event.getPlayer(), MessageType.SUCCESS, "Flower placed.");
            long distinct = state.placements.values().stream().filter(java.util.Objects::nonNull).map(ft -> ft.block).distinct().count();
            if (distinct >= 4) {
                completePuzzle(state);
            }
        }

        @EventHandler
        public void onTeleport(PlayerTeleportEvent event) {
            if (event.getFrom() == null || event.getTo() == null) return;
            World from = event.getFrom().getWorld();
            World to = event.getTo().getWorld();
            if (from != null && to != from && activeInstances.containsKey(from)) {
                removeDungeonItems(event.getPlayer());
            }
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            World world = event.getPlayer().getWorld();
            if (activeInstances.containsKey(world)) {
                removeDungeonItems(event.getPlayer());
            }
        }
    }
}
