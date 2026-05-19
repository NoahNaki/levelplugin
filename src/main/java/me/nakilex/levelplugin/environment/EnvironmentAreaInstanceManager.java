package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.VoidWorldGenerator;
import me.nakilex.levelplugin.utils.ModelEngineUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.CuboidTemplate;
import me.nakilex.levelplugin.utils.TooltipUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Debug/test harness for the new environment-area flow. It captures configured
 * cuboid templates directly from the source world and pastes them into a fresh
 * instanced flat world, mirroring the stronghold/dungeon template workflow.
 */
public final class EnvironmentAreaInstanceManager implements Listener {
    private static EnvironmentAreaInstanceManager instance;

    private static final String SOURCE_WORLD = "flatland";
    private static final int PASTE_X = 0;
    private static final int PASTE_Y = 64;
    private static final int PASTE_Z = 0;
    private static final String HOLOGRAM_TAG_PREFIX = "environment_area_build:";
    private static final Material ALIGNMENT_MARKER = Material.GOLD_BLOCK;
    private static final Material HOLOGRAM_PLACEHOLDER_MARKER = Material.LIGHT_BLUE_CONCRETE;
    private static final int BUILD_COST_COINS = 100;
    private static final long PAYMENT_ANIMATION_TICKS = 28L;
    private static final int BUILD_ANIMATION_TOTAL_TICKS = 40;
    private static final long COIN_SEND_INTERVAL_TICKS = 2L;
    private static final List<CoinVisual> PAYMENT_COIN_VISUALS = List.of(
            new CoinVisual(100, Material.GOLD_NUGGET, "gold_coin"),
            new CoinVisual(10, Material.IRON_NUGGET, "iron_coin"),
            new CoinVisual(1, Material.COPPER_INGOT, "copper_coin")
    );

    private static final Cuboid AREA = new Cuboid(-29, -61, 718, 19, -61, 670);

    private static final List<BuildingTemplate> BUILDINGS = List.of(
            new BuildingTemplate(1, "diamond_template", "Diamond Template", Material.DIAMOND_BLOCK,
                    new Cuboid(-31, -60, 720, -41, -51, 730), new Cuboid(-22, -61, 681, -10, -61, 693)),
            new BuildingTemplate(2, "lapis_template", "Lapis Template", Material.LAPIS_BLOCK,
                    new Cuboid(-31, -60, 732, -41, -51, 742), new Cuboid(-2, -61, 690, 10, -61, 678)),
            new BuildingTemplate(3, "emerald_template", "Emerald Template", Material.EMERALD_BLOCK,
                    new Cuboid(-43, -60, 732, -53, -51, 742), new Cuboid(4, -61, 694, 16, -61, 706)),
            new BuildingTemplate(4, "redstone_template", "Redstone Template", Material.REDSTONE_BLOCK,
                    new Cuboid(-43, -60, 720, -53, -51, 730), new Cuboid(-10, -61, 712, -22, -61, 700))
    );

    private static final Map<Integer, BuildingTemplate> BUILDINGS_BY_SLOT = BUILDINGS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(BuildingTemplate::slot, building -> building));

    private final Main plugin;
    private final Map<UUID, EnvironmentAreaSession> sessions = new HashMap<>();
    private final Map<UUID, BukkitTask> activeBuildTasks = new HashMap<>();

    private EnvironmentAreaInstanceManager(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static EnvironmentAreaInstanceManager getInstance(Main plugin) {
        if (instance == null) {
            instance = new EnvironmentAreaInstanceManager(plugin);
        }
        return instance;
    }

    public boolean initialize(Player target) {
        if (target == null || !target.isOnline()) {
            return false;
        }
        World source = Bukkit.getWorld(SOURCE_WORLD);
        if (source == null) {
            ChatMessageUtil.send(target, ChatMessageUtil.MessageType.ERROR,
                    "Environment source world '" + SOURCE_WORLD + "' is not loaded.");
            return false;
        }

        CuboidTemplate areaTemplate = capture(source, AREA);
        Map<Integer, AlignedTemplate> buildingTemplates = new HashMap<>();
        for (BuildingTemplate building : BUILDINGS) {
            buildingTemplates.put(building.slot(), captureAlignedTemplate(source, building));
        }

        World world = recreateWorld(target.getUniqueId());
        if (world == null) {
            ChatMessageUtil.send(target, ChatMessageUtil.MessageType.ERROR,
                    "Could not create environment instance world.");
            return false;
        }

        areaTemplate.paste(world, PASTE_X, PASTE_Y, PASTE_Z);
        EnvironmentAreaSession old = sessions.remove(target.getUniqueId());
        if (old != null) {
            old.removeHolograms();
        }

        EnvironmentAreaSession session = new EnvironmentAreaSession(target.getUniqueId(), world, buildingTemplates);
        sessions.put(target.getUniqueId(), session);
        spawnBuildHolograms(session);

        Location spawn = new Location(world,
                PASTE_X + (AREA.width() / 2.0),
                PASTE_Y + 1.0,
                PASTE_Z + (AREA.depth() / 2.0));
        target.teleport(spawn);
        ChatMessageUtil.send(target, ChatMessageUtil.MessageType.SUCCESS,
                "Initialized environment area in " + ChatColor.WHITE + world.getName() + ChatColor.GREEN + ".");
        return true;
    }


    public List<String> scanBlocks(String templateName) {
        if (templateName == null || templateName.isBlank()) {
            return List.of();
        }
        World source = Bukkit.getWorld(SOURCE_WORLD);
        if (source == null) {
            return List.of("Source world not loaded: " + SOURCE_WORLD);
        }

        BuildingTemplate building = BUILDINGS.stream()
                .filter(candidate -> candidate.id().equalsIgnoreCase(templateName)
                        || candidate.displayName().equalsIgnoreCase(templateName))
                .findFirst()
                .orElse(null);
        if (building == null) {
            return List.of("Unknown template: " + templateName);
        }

        CuboidTemplate template = capture(source, building.source());
        Map<Material, Integer> counts = new HashMap<>();
        for (CuboidTemplate.BlockCopy copy : template.blocks()) {
            counts.merge(copy.data().getMaterial(), 1, Integer::sum);
        }

        List<Map.Entry<Material, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((left, right) -> {
            int byCount = Integer.compare(right.getValue(), left.getValue());
            if (byCount != 0) {
                return byCount;
            }
            return left.getKey().name().compareTo(right.getKey().name());
        });

        List<String> lines = new ArrayList<>();
        lines.add("Template " + building.id() + " block totals:");
        boolean hasMarker = counts.containsKey(ALIGNMENT_MARKER);
        lines.add("Alignment marker " + ALIGNMENT_MARKER + ": " + (hasMarker ? "FOUND" : "MISSING"));
        for (Map.Entry<Material, Integer> entry : sorted) {
            String pretty = entry.getKey().name().toLowerCase(Locale.ROOT).replace('_', ' ');
            lines.add(entry.getValue() + "x " + pretty);
        }
        return lines;
    }

    public List<String> templateNames() {
        List<String> names = new ArrayList<>();
        for (BuildingTemplate building : BUILDINGS) {
            names.add(building.id());
        }
        return names;
    }

    private CuboidTemplate capture(World source, Cuboid cuboid) {
        return CuboidTemplate.capture(
                new Location(source, cuboid.x1(), cuboid.y1(), cuboid.z1()),
                new Location(source, cuboid.x2(), cuboid.y2(), cuboid.z2()));
    }

    private AlignedTemplate captureAlignedTemplate(World source, BuildingTemplate building) {
        CuboidTemplate template = capture(source, building.source());
        CuboidTemplate.BlockCopy marker = template.firstBlock(ALIGNMENT_MARKER).orElse(null);
        if (marker == null) {
            plugin.getLogger().warning("[EnvironmentArea] Building template '" + building.id()
                    + "' has no " + ALIGNMENT_MARKER + " alignment marker.");
        }
        return new AlignedTemplate(template, marker);
    }

    private World recreateWorld(UUID ownerId) {
        String worldName = "environment_" + ownerId.toString().substring(0, 8).toLowerCase(Locale.ROOT);
        World existing = Bukkit.getWorld(worldName);
        if (existing != null) {
            for (Player player : new ArrayList<>(existing.getPlayers())) {
                player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            }
            Bukkit.unloadWorld(existing, false);
        }
        deleteWorldFolder(worldName);

        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.FLAT);
        creator.generator(new VoidWorldGenerator());
        creator.generateStructures(false);
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            return null;
        }
        world.setAutoSave(false);
        world.setKeepSpawnInMemory(false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        world.setTime(6000L);
        world.setSpawnLocation(PASTE_X + AREA.width() / 2, PASTE_Y + 1, PASTE_Z + AREA.depth() / 2);
        return world;
    }

    private void deleteWorldFolder(String worldName) {
        Path path = Bukkit.getWorldContainer().toPath().resolve(worldName);
        if (!Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ex) {
                            plugin.getLogger().warning("Could not delete environment world file " + p + ": " + ex.getMessage());
                        }
                    });
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not delete environment world folder '" + worldName + "': " + ex.getMessage());
        }
    }

    private void spawnBuildHolograms(EnvironmentAreaSession session) {
        session.removeHolograms();
        session.alignmentAnchors().clear();
        for (BuildingTemplate building : BUILDINGS) {
            Location marker = findMarker(session.world(), building);
            Location alignmentAnchor = findAlignmentMarker(session.world(), building);
            if (alignmentAnchor != null) {
                session.alignmentAnchors().put(building.slot(), alignmentAnchor.clone());
            }
            String tag = HOLOGRAM_TAG_PREFIX + session.ownerId() + ":" + building.slot();
            session.holograms().addAll(spawnClickableHologram(marker, tag, List.of(
                    ChatColor.GREEN + "Build " + ChatColor.WHITE + building.displayName(),
                    ChatColor.GRAY + "Cost: " + ChatColor.GOLD + BUILD_COST_COINS + " <glyph:coins_icon>",
                    ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH.toString() + "--------------------",
                    TooltipUtil.bulletLine(ChatColor.GRAY + "Aligns template and foundation gold blocks."),
                    ChatColor.YELLOW + "Right Click " + ChatColor.GRAY + "to build")));
        }
    }

    private Location findMarker(World world, BuildingTemplate building) {
        WorldCuboid placementBounds = toPastedCuboid(building.placement());
        Block hologramMarker = findFirstBlock(world, placementBounds, HOLOGRAM_PLACEHOLDER_MARKER, true);
        if (hologramMarker != null) {
            return hologramMarker.getLocation().add(0.5, 1.0, 0.5);
        }
        Block fallback = findFirstBlock(world, placementBounds, building.marker(), false);
        if (fallback != null) {
            return fallback.getLocation().add(0.5, 2.0, 0.5);
        }
        return placementBounds.centerTop(world, 2.0);
    }

    private List<Entity> spawnClickableHologram(Location base, String tag, List<String> lines) {
        List<Entity> entities = new ArrayList<>();
        Interaction clicker = base.getWorld().spawn(base, Interaction.class, interaction -> {
            interaction.setInteractionWidth(2.0f);
            interaction.setInteractionHeight(2.0f);
            interaction.addScoreboardTag(tag);
        });
        entities.add(clicker);

        double offset = 0.0;
        for (String line : lines) {
            TextDisplay display = (TextDisplay) base.getWorld().spawnEntity(base.clone().add(0, offset, 0), EntityType.TEXT_DISPLAY);
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadowRadius(0f);
            display.setShadowStrength(0f);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            display.setText(line);
            display.addScoreboardTag(tag);
            entities.add(display);
            offset -= 0.25;
        }
        return entities;
    }

    private void handleInteract(Player player, Entity entity, Runnable cancelAction) {
        if (player == null || entity == null) {
            return;
        }
        for (String tag : entity.getScoreboardTags()) {
            if (!tag.startsWith(HOLOGRAM_TAG_PREFIX)) {
                continue;
            }
            cancelAction.run();
            handleBuildTag(player, tag);
            return;
        }
    }

    private void handleBuildTag(Player player, String tag) {
        String payload = tag.substring(HOLOGRAM_TAG_PREFIX.length());
        String[] parts = payload.split(":");
        if (parts.length != 2) {
            return;
        }
        UUID ownerId;
        int slot;
        try {
            ownerId = UUID.fromString(parts[0]);
            slot = Integer.parseInt(parts[1]);
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (!ownerId.equals(player.getUniqueId())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "This environment area belongs to another player.");
            return;
        }
        EnvironmentAreaSession session = sessions.get(ownerId);
        BuildingTemplate building = BUILDINGS_BY_SLOT.get(slot);
        if (session == null || building == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Environment build session is no longer active.");
            return;
        }
        AlignedTemplate alignedTemplate = session.buildingTemplates().get(slot);
        if (alignedTemplate == null || alignedTemplate.template() == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Template is missing for this build slot.");
            return;
        }
        CuboidTemplate.BlockCopy sourceMarker = alignedTemplate.alignmentMarker();
        Location destinationMarker = resolveAlignmentMarker(session, building);
        if (sourceMarker == null || destinationMarker == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Missing " + ALIGNMENT_MARKER + " alignment marker for " + building.displayName() + ".");
            return;
        }
        int coins = plugin.getEconomyManager().getBalance(player);
        if (coins < BUILD_COST_COINS) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "You need " + ChatColor.GOLD + BUILD_COST_COINS + " <glyph:coins_icon>"
                            + ChatColor.RED + " to build this.");
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 0.7f);
            return;
        }
        plugin.getEconomyManager().deductCoins(player, BUILD_COST_COINS);
        playCoinPaymentVisual(player, destinationMarker, BUILD_COST_COINS);
        BukkitTask existing = activeBuildTasks.remove(ownerId);
        if (existing != null) {
            existing.cancel();
        }
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    activeBuildTasks.remove(ownerId);
                    cancel();
                    return;
                }
                buildTemplateLayered(player, session, building, alignedTemplate, destinationMarker, sourceMarker);
                removeBuildHologram(session, tag);
                activeBuildTasks.remove(ownerId);
            }
        }.runTaskLater(plugin, PAYMENT_ANIMATION_TICKS);
        activeBuildTasks.put(ownerId, task);
    }

    private void playCoinPaymentVisual(Player player, Location destinationMarker, int amount) {
        if (player == null || destinationMarker == null || amount <= 0) {
            return;
        }
        World world = destinationMarker.getWorld();
        if (world == null || player.getWorld() == null || !player.getWorld().equals(world)) {
            return;
        }
        Location target = destinationMarker.clone().add(0.5, 1.0, 0.5);
        List<CoinVisual> emissions = buildCoinVisualSequence(amount);
        if (emissions.isEmpty()) {
            return;
        }
        new BukkitRunnable() {
            int sent = 0;
            @Override
            public void run() {
                if (!player.isOnline() || !player.getWorld().equals(world) || sent >= emissions.size()) {
                    cancel();
                    return;
                }
                Location source = player.getLocation().clone().add(0.0, 1.1, 0.0);
                CoinVisual visual = emissions.get(sent);
                Item coin = world.dropItem(source, new org.bukkit.inventory.ItemStack(visual.material(), 1));
                coin.setPickupDelay(Integer.MAX_VALUE);
                coin.setCanMobPickup(false);
                coin.setUnlimitedLifetime(false);
                ModelEngineUtil.applyFirstAvailableModel(coin, java.util.List.of(visual.modelId()), plugin);
                var vec = target.toVector().subtract(coin.getLocation().toVector());
                if (vec.lengthSquared() > 0.001) {
                    coin.setVelocity(vec.normalize().multiply(0.42));
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (coin.isValid()) {
                        coin.remove();
                    }
                }, 12L);
                world.playSound(target, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.45f, 1.5f + (sent * 0.01f));
                sent++;
            }
        }.runTaskTimer(plugin, 0L, COIN_SEND_INTERVAL_TICKS);
    }

    private List<CoinVisual> buildCoinVisualSequence(int amount) {
        if (amount <= 0) {
            return List.of();
        }
        int remaining = amount;
        List<CoinVisual> sequence = new ArrayList<>();
        for (CoinVisual visual : PAYMENT_COIN_VISUALS) {
            int count = remaining / visual.value();
            remaining %= visual.value();
            for (int i = 0; i < count; i++) {
                sequence.add(visual);
            }
        }
        return sequence;
    }

    private void removeBuildHologram(EnvironmentAreaSession session, String tag) {
        if (session == null || tag == null || tag.isBlank()) {
            return;
        }
        session.holograms().removeIf(entity -> {
            if (entity == null || entity.isDead()) {
                return true;
            }
            if (!entity.getScoreboardTags().contains(tag)) {
                return false;
            }
            entity.remove();
            return true;
        });
    }

    private void buildTemplateLayered(Player player,
                                      EnvironmentAreaSession session,
                                      BuildingTemplate building,
                                      AlignedTemplate alignedTemplate,
                                      Location destinationMarker,
                                      CuboidTemplate.BlockCopy sourceMarker) {
        if (player == null || session == null || building == null || alignedTemplate == null
                || alignedTemplate.template() == null || destinationMarker == null || sourceMarker == null) {
            return;
        }
        int baseX = destinationMarker.getBlockX() - sourceMarker.x();
        int baseY = destinationMarker.getBlockY() - sourceMarker.y();
        int baseZ = destinationMarker.getBlockZ() - sourceMarker.z();
        List<CuboidTemplate.BlockCopy> copies = new ArrayList<>(alignedTemplate.template().blocks());
        copies.sort(Comparator.comparingInt(CuboidTemplate.BlockCopy::y));
        int blocksPerTick = Math.max(1, copies.size() / Math.max(1, BUILD_ANIMATION_TOTAL_TICKS));
        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                World world = session.world();
                if (world == null) {
                    cancel();
                    return;
                }
                for (int i = 0; i < blocksPerTick && index < copies.size(); i++, index++) {
                    CuboidTemplate.BlockCopy copy = copies.get(index);
                    world.getBlockAt(baseX + copy.x(), baseY + copy.y(), baseZ + copy.z())
                            .setBlockData(copy.data(), false);
                }
                if (index >= copies.size()) {
                    player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
                    ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                            "Built " + ChatColor.WHITE + building.displayName() + ChatColor.GREEN + ".");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked(), () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractAt(PlayerInteractAtEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked(), () -> event.setCancelled(true));
    }

    private record Cuboid(int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX() { return Math.min(x1, x2); }
        int minY() { return Math.min(y1, y2); }
        int minZ() { return Math.min(z1, z2); }
        int maxX() { return Math.max(x1, x2); }
        int maxY() { return Math.max(y1, y2); }
        int maxZ() { return Math.max(z1, z2); }
        int width() { return Math.abs(x1 - x2) + 1; }
        int depth() { return Math.abs(z1 - z2) + 1; }
    }

    private record BuildingTemplate(int slot,
                                    String id,
                                    String displayName,
                                    Material marker,
                                    Cuboid source,
                                    Cuboid placement) { }

    private Location resolveAlignmentMarker(EnvironmentAreaSession session, BuildingTemplate building) {
        if (session == null || building == null) {
            return null;
        }
        Location cached = session.alignmentAnchors().get(building.slot());
        if (cached != null) {
            return cached.clone();
        }
        Location discovered = findAlignmentMarker(session.world(), building);
        if (discovered != null) {
            session.alignmentAnchors().put(building.slot(), discovered.clone());
        }
        return discovered;
    }

    private Location findAlignmentMarker(World world, BuildingTemplate building) {
        Block marker = findFirstBlock(world, toPastedCuboid(building.placement()), ALIGNMENT_MARKER, true);
        return marker == null ? null : marker.getLocation();
    }

    private WorldCuboid toPastedCuboid(Cuboid source) {
        return new WorldCuboid(
                PASTE_X + (source.minX() - AREA.minX()),
                PASTE_Y + (source.minY() - AREA.minY()),
                PASTE_Z + (source.minZ() - AREA.minZ()),
                PASTE_X + (source.maxX() - AREA.minX()),
                PASTE_Y + (source.maxY() - AREA.minY()),
                PASTE_Z + (source.maxZ() - AREA.minZ()));
    }

    private Block findFirstBlock(World world, WorldCuboid cuboid, Material material, boolean includeY) {
        if (world == null || cuboid == null || material == null) {
            return null;
        }
        for (int x = cuboid.minX(); x <= cuboid.maxX(); x++) {
            if (includeY) {
                for (int y = cuboid.minY(); y <= cuboid.maxY(); y++) {
                    for (int z = cuboid.minZ(); z <= cuboid.maxZ(); z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() == material) {
                            return block;
                        }
                    }
                }
                continue;
            }
            int y = cuboid.minY();
            for (int z = cuboid.minZ(); z <= cuboid.maxZ(); z++) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == material) {
                    return block;
                }
            }
        }
        return null;
    }

    private record WorldCuboid(int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX() { return Math.min(x1, x2); }
        int minY() { return Math.min(y1, y2); }
        int minZ() { return Math.min(z1, z2); }
        int maxX() { return Math.max(x1, x2); }
        int maxY() { return Math.max(y1, y2); }
        int maxZ() { return Math.max(z1, z2); }

        Location centerTop(World world, double yOffset) {
            return new Location(world,
                    (minX() + maxX()) / 2.0 + 0.5,
                    minY() + yOffset,
                    (minZ() + maxZ()) / 2.0 + 0.5);
        }
    }

    private record AlignedTemplate(CuboidTemplate template, CuboidTemplate.BlockCopy alignmentMarker) { }
    private record CoinVisual(int value, Material material, String modelId) { }

    private record EnvironmentAreaSession(UUID ownerId,
                                          World world,
                                          Map<Integer, AlignedTemplate> buildingTemplates,
                                          List<Entity> holograms,
                                          Map<Integer, Location> alignmentAnchors) {
        private EnvironmentAreaSession(UUID ownerId, World world, Map<Integer, AlignedTemplate> buildingTemplates) {
            this(ownerId, world, buildingTemplates, new ArrayList<>(), new HashMap<>());
        }

        private void removeHolograms() {
            for (Entity hologram : holograms) {
                if (hologram != null && !hologram.isDead()) {
                    hologram.remove();
                }
            }
            holograms.clear();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) {
            return;
        }
        shutdown();
    }

    public void shutdown() {
        for (BukkitTask task : new ArrayList<>(activeBuildTasks.values())) {
            if (task != null) {
                task.cancel();
            }
        }
        activeBuildTasks.clear();
        for (EnvironmentAreaSession session : new ArrayList<>(sessions.values())) {
            if (session != null) {
                session.removeHolograms();
                session.alignmentAnchors().clear();
            }
        }
        sessions.clear();
    }
}
