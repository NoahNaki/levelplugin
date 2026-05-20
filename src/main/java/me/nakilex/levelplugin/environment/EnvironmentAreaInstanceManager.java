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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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
import java.util.concurrent.ConcurrentHashMap;

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
    private static final int AREA_SPACING_BLOCKS = 1500;
    private static final Cuboid FINISHED_WORLD_BORDER = new Cuboid(3489, -44, -3143, 4058, 271, -2685);
    private static final String HOLOGRAM_TAG_PREFIX = "environment_area_build:";
    private static final int BUILD_COST_COINS = 100;
    private static final long PAYMENT_ANIMATION_TICKS = 28L;
    private static final int BUILD_ANIMATION_TOTAL_TICKS = 40;
    private static final long COIN_SEND_INTERVAL_TICKS = 2L;
    private static final List<CoinVisual> PAYMENT_COIN_VISUALS = List.of(
            new CoinVisual(100, Material.GOLD_NUGGET, "gold_coin"),
            new CoinVisual(10, Material.IRON_NUGGET, "iron_coin"),
            new CoinVisual(1, Material.COPPER_INGOT, "copper_coin")
    );

    private static final Cuboid AREA = new Cuboid(4058, -44, -3603, 3489, 230, -3145);
    private static final Cuboid FINISHED_WORLD_AREA = new Cuboid(4058, -44, -2685, 3489, 230, -3143);
    private static final WorldPoint FINISHED_WORLD_ANCHOR = new WorldPoint(3489, -23, -3143);
    private static final WorldPoint EMPTY_WORLD_ANCHOR = new WorldPoint(3489, -23, -3603);

    private static final List<BuildingTemplate> BUILDINGS = List.of(
            new BuildingTemplate(1, "bar", "Bar", Material.BRICKS,
                    new Cuboid(3821, -5, -2852, 3780, 60, -2805),
                    projectFinishedToEmpty(new Cuboid(3821, -5, -2852, 3780, 60, -2805)),
                    projectFinishedToEmpty(new WorldPoint(3799, 0, -2851))),
            new BuildingTemplate(2, "blacksmith", "Blacksmith", Material.ANVIL,
                    new Cuboid(3875, -20, -2976, 3922, 51, -3035),
                    projectFinishedToEmpty(new Cuboid(3875, -20, -2976, 3922, 51, -3035)),
                    projectFinishedToEmpty(new WorldPoint(3883, -10, -2982))),
            new BuildingTemplate(3, "fishing", "Fishing", Material.WATER_BUCKET,
                    new Cuboid(3860, -15, -2807, 3921, 61, -2880),
                    projectFinishedToEmpty(new Cuboid(3860, -15, -2807, 3921, 61, -2880)),
                    projectFinishedToEmpty(new WorldPoint(3877, -8, -2836))),
            new BuildingTemplate(4, "castle", "Castle", Material.STONE_BRICKS,
                    new Cuboid(3717, 13, -2849, 3583, 213, -3027),
                    projectFinishedToEmpty(new Cuboid(3717, 13, -2849, 3583, 213, -3027)),
                    projectFinishedToEmpty(new WorldPoint(3693, 25, -2934)))
    );

    private static final Map<Integer, BuildingTemplate> BUILDINGS_BY_SLOT = BUILDINGS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(BuildingTemplate::slot, building -> building));

    private final Main plugin;
    private final Map<UUID, EnvironmentAreaSession> sessions = new HashMap<>();
    private final Map<UUID, BukkitTask> activeBuildTasks = new HashMap<>();
    private final Map<String, CuboidTemplate> templateCache = new ConcurrentHashMap<>();
    private final Map<UUID, java.util.Set<Integer>> builtSlotsByProfile = new HashMap<>();

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

        CuboidTemplate areaTemplate = getOrCaptureTemplate(source, "area:base", AREA);
        Map<Integer, CuboidTemplate> buildingTemplates = new HashMap<>();
        for (BuildingTemplate building : BUILDINGS) {
            buildingTemplates.put(building.slot(),
                    getOrCaptureTemplate(source, "building:" + building.id().toLowerCase(Locale.ROOT), building.source()));
        }

        World world = recreateWorld(target.getUniqueId());
        if (world == null) {
            ChatMessageUtil.send(target, ChatMessageUtil.MessageType.ERROR,
                    "Could not create environment instance world.");
            return false;
        }

        SlotOffset offset = slotOffsetFor(target.getUniqueId());
        int originX = PASTE_X + offset.dx();
        int originY = PASTE_Y;
        int originZ = PASTE_Z + offset.dz();
        areaTemplate.paste(world, originX, originY, originZ);
        EnvironmentAreaSession old = sessions.remove(target.getUniqueId());
        if (old != null) {
            old.removeHolograms();
        }

        WorldCuboid border = toPastedCuboid(FINISHED_WORLD_BORDER, originX, originY, originZ);
        EnvironmentAreaSession session = new EnvironmentAreaSession(target.getUniqueId(), world, buildingTemplates, originX, originY, originZ, border);
        sessions.put(target.getUniqueId(), session);
        spawnBuildHolograms(session);
        applySavedBuilds(target, session);

        Location spawn = new Location(world,
                originX + (AREA.width() / 2.0),
                originY + 1.0,
                originZ + (AREA.depth() / 2.0));
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

        CuboidTemplate template = getOrCaptureTemplate(source,
                "building:" + building.id().toLowerCase(Locale.ROOT), building.source());
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

    private CuboidTemplate getOrCaptureTemplate(World source, String templateKey, Cuboid cuboid) {
        String worldScopedKey = source.getUID() + ":" + templateKey;
        return templateCache.computeIfAbsent(worldScopedKey, ignored -> capture(source, cuboid));
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
        for (BuildingTemplate building : BUILDINGS) {
            Location marker = findMarker(session, building);
            String tag = HOLOGRAM_TAG_PREFIX + session.ownerId() + ":" + building.slot();
            session.holograms().addAll(spawnClickableHologram(marker, tag, List.of(
                    ChatColor.GREEN + "Build " + ChatColor.WHITE + building.displayName(),
                    ChatColor.GRAY + "Cost: " + ChatColor.GOLD + BUILD_COST_COINS + " <glyph:coins_icon>",
                    ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH.toString() + "--------------------",
                    TooltipUtil.bulletLine(ChatColor.GRAY + "Replaces the matching empty-world area."),
                    ChatColor.YELLOW + "Right Click " + ChatColor.GRAY + "to build")));
        }
    }

    private Location findMarker(EnvironmentAreaSession session, BuildingTemplate building) {
        World world = session.world();
        WorldCuboid placementBounds = toPastedCuboid(building.placement(), session.originX(), session.originY(), session.originZ());
        Location configured = toPastedLocation(world, building.hologramPoint(), session.originX(), session.originY(), session.originZ());
        if (configured != null) {
            return configured.add(0.5, 1.0, 0.5);
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
        UUID sessionOwner = resolveAreaOwner(player);
        if (!ownerId.equals(sessionOwner)) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "This environment area belongs to another player.");
            return;
        }
        EnvironmentAreaSession session = sessions.get(sessionOwner);
        BuildingTemplate building = BUILDINGS_BY_SLOT.get(slot);
        if (session == null || building == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Environment build session is no longer active.");
            return;
        }
        CuboidTemplate template = session.buildingTemplates().get(slot);
        if (template == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR, "Template is missing for this build slot.");
            return;
        }
        WorldCuboid destinationArea = toPastedCuboid(building.placement(), session.originX(), session.originY(), session.originZ());
        Location destinationMarker = destinationArea.centerTop(session.world(), 1.0);
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
        BukkitTask existing = activeBuildTasks.remove(sessionOwner);
        if (existing != null) {
            existing.cancel();
        }
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    activeBuildTasks.remove(sessionOwner);
                    cancel();
                    return;
                }
                buildTemplateLayered(player, session, building, template, destinationArea, destinationMarker);
                removeBuildHologram(session, tag);
                markBuiltForProfile(player, slot);
                activeBuildTasks.remove(sessionOwner);
            }
        }.runTaskLater(plugin, PAYMENT_ANIMATION_TICKS);
        activeBuildTasks.put(sessionOwner, task);
    }

    private UUID resolveAreaOwner(Player player) {
        return player.getUniqueId();
    }

    private UUID resolveProfileScopedId(Player player) {
        Integer slot = me.nakilex.levelplugin.player.profile.ProfileManager.getInstance().getActiveSlot(player.getUniqueId());
        int safeSlot = slot == null ? 0 : Math.max(0, slot);
        String key = resolveAreaOwner(player) + ":" + safeSlot;
        return UUID.nameUUIDFromBytes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void markBuiltForProfile(Player player, int slot) {
        UUID scoped = resolveProfileScopedId(player);
        builtSlotsByProfile.computeIfAbsent(scoped, ignored -> new java.util.HashSet<>()).add(slot);
        saveBuiltSlots(scoped);
    }

    private void applySavedBuilds(Player player, EnvironmentAreaSession session) {
        UUID scoped = resolveProfileScopedId(player);
        java.util.Set<Integer> built = loadBuiltSlots(scoped);
        for (Integer slot : built) {
            BuildingTemplate building = BUILDINGS_BY_SLOT.get(slot);
            CuboidTemplate template = session.buildingTemplates().get(slot);
            if (building == null || template == null) continue;
            WorldCuboid area = toPastedCuboid(building.placement(), session.originX(), session.originY(), session.originZ());
            template.paste(session.world(), area.minX(), area.minY(), area.minZ());
            removeBuildHologram(session, HOLOGRAM_TAG_PREFIX + session.ownerId() + ":" + slot);
        }
    }

    private java.util.Set<Integer> loadBuiltSlots(UUID scoped) {
        java.util.Set<Integer> cached = builtSlotsByProfile.get(scoped);
        if (cached != null) return cached;
        var cfg = plugin.getPlayerConfig().getConfig().getIntegerList("players." + scoped + ".environment.area.built-slots");
        java.util.Set<Integer> built = new java.util.HashSet<>(cfg);
        builtSlotsByProfile.put(scoped, built);
        return built;
    }

    private void saveBuiltSlots(UUID scoped) {
        plugin.getPlayerConfig().getConfig().set("players." + scoped + ".environment.area.built-slots",
                new java.util.ArrayList<>(builtSlotsByProfile.getOrDefault(scoped, java.util.Set.of())));
        plugin.getPlayerConfig().saveConfigFile();
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
                                      CuboidTemplate template,
                                      WorldCuboid destinationArea,
                                      Location destinationMarker) {
        if (player == null || session == null || building == null || template == null
                || destinationArea == null || destinationMarker == null) {
            return;
        }
        int baseX = destinationArea.minX();
        int baseY = destinationArea.minY();
        int baseZ = destinationArea.minZ();
        List<CuboidTemplate.BlockCopy> copies = new ArrayList<>(template.blocks());
        copies.sort(Comparator.comparingInt(CuboidTemplate.BlockCopy::y));
        plugin.getLogger().info("[EnvironmentArea] Building '" + building.id() + "' for " + player.getName()
                + " -> sourceDims=" + template.width() + "x" + template.height() + "x" + template.depth()
                + ", destMin=" + baseX + "," + baseY + "," + baseZ
                + ", destMax=" + destinationArea.maxX() + "," + destinationArea.maxY() + "," + destinationArea.maxZ()
                + ", blockCount=" + copies.size());
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;
        EnvironmentAreaSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null) return;
        if (!event.getPlayer().getWorld().equals(session.world())) return;
        if (session.border().contains(event.getTo())) return;
        Location fallback = new Location(session.world(),
                session.originX() + (AREA.width() / 2.0),
                session.originY() + 1.0,
                session.originZ() + (AREA.depth() / 2.0));
        event.getPlayer().teleport(fallback);
        ChatMessageUtil.send(event.getPlayer(), ChatMessageUtil.MessageType.WARNING, "You cannot leave your area border.");
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
        Cuboid translate(int dx, int dy, int dz) {
            return new Cuboid(x1 + dx, y1 + dy, z1 + dz, x2 + dx, y2 + dy, z2 + dz);
        }
    }

    private record BuildingTemplate(int slot,
                                    String id,
                                    String displayName,
                                    Material marker,
                                    Cuboid source,
                                    Cuboid placement,
                                    WorldPoint hologramPoint) { }

    private record WorldPoint(int x, int y, int z) { }


    private WorldCuboid toPastedCuboid(Cuboid source, int originX, int originY, int originZ) {
        return new WorldCuboid(
                originX + (source.minX() - AREA.minX()),
                originY + (source.minY() - AREA.minY()),
                originZ + (source.minZ() - AREA.minZ()),
                originX + (source.maxX() - AREA.minX()),
                originY + (source.maxY() - AREA.minY()),
                originZ + (source.maxZ() - AREA.minZ()));
    }

    private static Cuboid projectFinishedToEmpty(Cuboid finishedSelection) {
        int dx = EMPTY_WORLD_ANCHOR.x() - FINISHED_WORLD_ANCHOR.x();
        int dy = EMPTY_WORLD_ANCHOR.y() - FINISHED_WORLD_ANCHOR.y();
        int dz = EMPTY_WORLD_ANCHOR.z() - FINISHED_WORLD_ANCHOR.z();
        return finishedSelection.translate(dx, dy, dz);
    }

    private static WorldPoint projectFinishedToEmpty(WorldPoint finishedPoint) {
        int dx = EMPTY_WORLD_ANCHOR.x() - FINISHED_WORLD_ANCHOR.x();
        int dy = EMPTY_WORLD_ANCHOR.y() - FINISHED_WORLD_ANCHOR.y();
        int dz = EMPTY_WORLD_ANCHOR.z() - FINISHED_WORLD_ANCHOR.z();
        return new WorldPoint(finishedPoint.x() + dx, finishedPoint.y() + dy, finishedPoint.z() + dz);
    }

    private Location toPastedLocation(World world, WorldPoint source, int originX, int originY, int originZ) {
        if (world == null || source == null) {
            return null;
        }
        return new Location(
                world,
                originX + (source.x() - AREA.minX()),
                originY + (source.y() - AREA.minY()),
                originZ + (source.z() - AREA.minZ()));
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

        boolean contains(Location location) {
            if (location == null) return false;
            double x = location.getX(), y = location.getY(), z = location.getZ();
            return x >= minX() && x <= maxX() + 1
                    && y >= minY() && y <= maxY() + 1
                    && z >= minZ() && z <= maxZ() + 1;
        }
    }

    private record CoinVisual(int value, Material material, String modelId) { }

    private record EnvironmentAreaSession(UUID ownerId,
                                          World world,
                                          Map<Integer, CuboidTemplate> buildingTemplates,
                                          List<Entity> holograms,
                                          int originX,
                                          int originY,
                                          int originZ,
                                          WorldCuboid border) {
        private EnvironmentAreaSession(UUID ownerId, World world, Map<Integer, CuboidTemplate> buildingTemplates,
                                       int originX, int originY, int originZ, WorldCuboid border) {
            this(ownerId, world, buildingTemplates, new ArrayList<>(), originX, originY, originZ, border);
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

    private SlotOffset slotOffsetFor(UUID ownerId) {
        int hash = Math.abs(ownerId.hashCode());
        int col = hash % 8;
        int row = (hash / 8) % 8;
        return new SlotOffset(col * AREA_SPACING_BLOCKS, row * AREA_SPACING_BLOCKS);
    }

    private record SlotOffset(int dx, int dz) {}

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() != plugin) {
            return;
        }
        shutdown();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        EnvironmentAreaSession session = sessions.remove(id);
        if (session == null) {
            return;
        }
        session.removeHolograms();
        Bukkit.unloadWorld(session.world(), false);
        deleteWorldFolder(session.world().getName());
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
                    }
        }
        sessions.clear();
    }
}
