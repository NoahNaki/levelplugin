package me.nakilex.levelplugin.environment;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.dungeon.VoidWorldGenerator;
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
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

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

    private final Main plugin;
    private final Map<UUID, EnvironmentAreaSession> sessions = new HashMap<>();

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
        for (BuildingTemplate building : BUILDINGS) {
            Location marker = findMarker(session.world(), building);
            String tag = HOLOGRAM_TAG_PREFIX + session.ownerId() + ":" + building.slot();
            session.holograms().addAll(spawnClickableHologram(marker, tag, List.of(
                    ChatColor.GREEN + "Build " + ChatColor.WHITE + building.displayName(),
                    ChatColor.DARK_GRAY + ChatColor.STRIKETHROUGH.toString() + "--------------------",
                    TooltipUtil.bulletLine(ChatColor.GRAY + "Aligns template and foundation gold blocks."),
                    ChatColor.YELLOW + "Right Click " + ChatColor.GRAY + "to build")));
        }
    }

    private Location findMarker(World world, BuildingTemplate building) {
        int minX = PASTE_X + (building.placement().minX() - AREA.minX());
        int maxX = PASTE_X + (building.placement().maxX() - AREA.minX());
        int y = PASTE_Y + (building.placement().minY() - AREA.minY());
        int minZ = PASTE_Z + (building.placement().minZ() - AREA.minZ());
        int maxZ = PASTE_Z + (building.placement().maxZ() - AREA.minZ());
        for (int x = Math.min(minX, maxX); x <= Math.max(minX, maxX); x++) {
            for (int z = Math.min(minZ, maxZ); z <= Math.max(minZ, maxZ); z++) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() == building.marker()) {
                    return block.getLocation().add(0.5, 2.0, 0.5);
                }
            }
        }
        return new Location(world,
                (Math.min(minX, maxX) + Math.max(minX, maxX)) / 2.0 + 0.5,
                y + 2.0,
                (Math.min(minZ, maxZ) + Math.max(minZ, maxZ)) / 2.0 + 0.5);
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
        BuildingTemplate building = BUILDINGS.stream().filter(candidate -> candidate.slot() == slot).findFirst().orElse(null);
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
        Location destinationMarker = findAlignmentMarker(session.world(), building);
        if (sourceMarker == null || destinationMarker == null) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.ERROR,
                    "Missing " + ALIGNMENT_MARKER + " alignment marker for " + building.displayName() + ".");
            return;
        }
        int baseX = destinationMarker.getBlockX() - sourceMarker.x();
        int baseY = destinationMarker.getBlockY() - sourceMarker.y();
        int baseZ = destinationMarker.getBlockZ() - sourceMarker.z();
        alignedTemplate.template().paste(session.world(), baseX, baseY, baseZ);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1f);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                "Built " + ChatColor.WHITE + building.displayName() + ChatColor.GREEN + ".");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked(), () -> event.setCancelled(true));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
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

    private Location findAlignmentMarker(World world, BuildingTemplate building) {
        if (world == null || building == null) {
            return null;
        }
        int minX = PASTE_X + (building.placement().minX() - AREA.minX());
        int maxX = PASTE_X + (building.placement().maxX() - AREA.minX());
        int minY = PASTE_Y + (building.placement().minY() - AREA.minY());
        int maxY = PASTE_Y + (building.placement().maxY() - AREA.minY());
        int minZ = PASTE_Z + (building.placement().minZ() - AREA.minZ());
        int maxZ = PASTE_Z + (building.placement().maxZ() - AREA.minZ());
        for (int x = Math.min(minX, maxX); x <= Math.max(minX, maxX); x++) {
            for (int y = Math.min(minY, maxY); y <= Math.max(minY, maxY); y++) {
                for (int z = Math.min(minZ, maxZ); z <= Math.max(minZ, maxZ); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == ALIGNMENT_MARKER) {
                        return block.getLocation();
                    }
                }
            }
        }
        return null;
    }

    private record AlignedTemplate(CuboidTemplate template, CuboidTemplate.BlockCopy alignmentMarker) { }

    private record EnvironmentAreaSession(UUID ownerId,
                                          World world,
                                          Map<Integer, AlignedTemplate> buildingTemplates,
                                          List<Entity> holograms) {
        private EnvironmentAreaSession(UUID ownerId, World world, Map<Integer, AlignedTemplate> buildingTemplates) {
            this(ownerId, world, buildingTemplates, new ArrayList<>());
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
}
