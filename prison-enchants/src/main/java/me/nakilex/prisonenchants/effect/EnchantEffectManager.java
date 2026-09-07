package me.nakilex.prisonenchants.effect;

import me.nakilex.prisonenchants.hook.PrisonBridge;
import me.nakilex.prisonenchants.mine.MineArea;
import me.nakilex.prisonenchants.mine.MineRegionResolver;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class EnchantEffectManager {
    private static final Set<Material> PROTECTED_MATERIALS = Set.of(
            Material.BEDROCK, Material.BARRIER, Material.STRUCTURE_BLOCK,
            Material.JIGSAW, Material.END_PORTAL_FRAME, Material.END_PORTAL,
            Material.NETHER_PORTAL, Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK, Material.REPEATING_COMMAND_BLOCK
    );

    private final JavaPlugin plugin;
    private final MineRegionResolver mines;
    private final PrisonBridge prison;
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Set<Entity> spawnedEntities = new HashSet<>();

    public EnchantEffectManager(JavaPlugin plugin, MineRegionResolver mines, PrisonBridge prison) {
        this.plugin = plugin;
        this.mines = mines;
        this.prison = prison;
    }

    public void reload() {
        mines.reload();
    }

    public void trigger(String enchantId, Player player, Location origin, double level) {
        MineArea area = mines.resolve(origin).orElse(null);
        if (area == null) {
            sendMessage(player, "messages.mine-not-found",
                    "&8[&c&l!&8] &c&lPickaxe &8→ &7Your enchant could not find a protected mine region here.");
            return;
        }
        if (!begin(player)) return;

        try {
            switch (enchantId.toLowerCase(Locale.ROOT)) {
                case "tornado" -> tornado(player, area, origin, level);
                case "blackhole" -> blackHole(player, area, origin, level);
                case "meteors" -> meteors(player, area, origin, level);
                case "acidrain" -> acidRain(player, area, origin, level);
                default -> end(player);
            }
        } catch (RuntimeException ex) {
            end(player);
            plugin.getLogger().severe("Failed to run " + enchantId + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private boolean begin(Player player) {
        int limit = Math.max(1, plugin.getConfig().getInt("max-concurrent-effects", 4));
        if (activePlayers.contains(player.getUniqueId()) || activePlayers.size() >= limit) return false;
        activePlayers.add(player.getUniqueId());
        return true;
    }

    private void end(Player player) {
        activePlayers.remove(player.getUniqueId());
    }

    private void tornado(Player player, MineArea area, Location origin, double level) {
        double scale = levelScale("tornado", level);
        int duration = plugin.getConfig().getInt("tornado.duration-ticks", 70);
        double radius = lerp(plugin.getConfig().getDouble("tornado.base-radius", 5.0),
                plugin.getConfig().getDouble("tornado.max-radius", 8.0), scale);
        int maxBlocks = lerpInt(plugin.getConfig().getInt("tornado.base-blocks", 42),
                plugin.getConfig().getInt("tornado.max-blocks", 90), scale);

        Location center = origin.clone().add(0.5, 1.0, 0.5);
        List<Block> blocks = collectCylinder(area, origin, radius, 4, maxBlocks);
        List<FlyingBlock> flying = new ArrayList<>();
        for (Block block : blocks) {
            BlockDisplay display = spawnBlockDisplay(block.getLocation(), block.getBlockData(), 0.72f, Color.WHITE);
            Vector offset = display.getLocation().toVector().subtract(center.toVector());
            flying.add(new FlyingBlock(display, display.getLocation(), Math.atan2(offset.getZ(), offset.getX()),
                    Math.max(0.8, Math.sqrt(offset.getX() * offset.getX() + offset.getZ() * offset.getZ()))));
        }
        int processed = prison.process(player, blocks, "tornado");

        sendMessage(player, "messages.tornado-result",
                "&8[&b&l!&8] &b&lTornado &8→ &7Ripped through &f%blocks% &7blocks!",
                "%blocks%", String.valueOf(processed));
        area.world().playSound(center, Sound.ENTITY_PHANTOM_FLAP, 1.2f, 0.55f);

        new BukkitRunnable() {
            int tick;

            @Override
            public void run() {
                if (tick >= duration || !player.isOnline()) {
                    flying.forEach(block -> remove(block.display));
                    end(player);
                    cancel();
                    return;
                }

                double progress = tick / (double) duration;
                for (int i = 0; i < flying.size(); i++) {
                    FlyingBlock block = flying.get(i);
                    double angle = block.angle + tick * 0.31 + i * 0.12;
                    double orbit = Math.max(0.35, block.radius * (1.0 - progress * 0.72));
                    double y = center.getY() + progress * 9.0 + (i % 7) * 0.18;
                    Location next = new Location(area.world(),
                            center.getX() + Math.cos(angle) * orbit,
                            y,
                            center.getZ() + Math.sin(angle) * orbit);
                    block.display.teleport(next);
                }

                for (int i = 0; i < 12; i++) {
                    double angle = tick * 0.34 + i * Math.PI / 6.0;
                    double particleRadius = 1.0 + (i / 12.0) * radius * (1.0 - progress * 0.35);
                    Location particle = center.clone().add(
                            Math.cos(angle) * particleRadius,
                            (i / 12.0) * 7.0,
                            Math.sin(angle) * particleRadius);
                    area.world().spawnParticle(Particle.CLOUD, particle, 1, 0.08, 0.08, 0.08, 0.01);
                }
                if (tick % 12 == 0) {
                    area.world().playSound(center, Sound.ENTITY_PHANTOM_FLAP, 0.65f, 0.65f + (float) progress);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void blackHole(Player player, MineArea area, Location origin, double level) {
        double scale = levelScale("blackhole", level);
        int duration = plugin.getConfig().getInt("blackhole.duration-ticks", 55);
        double radius = lerp(plugin.getConfig().getDouble("blackhole.base-radius", 5.5),
                plugin.getConfig().getDouble("blackhole.max-radius", 9.0), scale);
        int maxBlocks = lerpInt(plugin.getConfig().getInt("blackhole.base-blocks", 50),
                plugin.getConfig().getInt("blackhole.max-blocks", 110), scale);
        Location center = origin.clone().add(0.5, 2.5, 0.5);

        BlockDisplay core = spawnBlockDisplay(center.clone().subtract(1.1, 1.1, 1.1),
                Material.BLACK_CONCRETE.createBlockData(), 2.2f, Color.PURPLE);
        List<Block> blocks = collectSphere(area, origin, radius, maxBlocks);
        List<PullBlock> pulling = new ArrayList<>();
        for (Block block : blocks) {
            BlockDisplay display = spawnBlockDisplay(block.getLocation(), block.getBlockData(), 0.68f, Color.PURPLE);
            pulling.add(new PullBlock(display, display.getLocation(),
                    ThreadLocalRandom.current().nextDouble(0.0, Math.PI * 2.0)));
        }
        int processed = prison.process(player, blocks, "blackhole");
        sendMessage(player, "messages.blackhole-result",
                "&8[&5&l!&8] &5&lBlack Hole &8→ &7Consumed &f%blocks% &7blocks!",
                "%blocks%", String.valueOf(processed));
        area.world().playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.45f);

        new BukkitRunnable() {
            int tick;

            @Override
            public void run() {
                if (tick >= duration || !player.isOnline()) {
                    pulling.forEach(block -> remove(block.display));
                    remove(core);
                    end(player);
                    cancel();
                    return;
                }
                double progress = tick / (double) duration;
                double eased = progress * progress;
                for (PullBlock block : pulling) {
                    Vector delta = center.toVector().subtract(block.start.toVector());
                    Location next = block.start.clone().add(delta.multiply(eased));
                    double remaining = 1.0 - progress;
                    double angle = block.phase + tick * 0.38;
                    next.add(Math.cos(angle) * remaining * 1.35, Math.sin(angle * 0.7) * remaining,
                            Math.sin(angle) * remaining * 1.35);
                    block.display.teleport(next);
                }

                area.world().spawnParticle(Particle.REVERSE_PORTAL, center, 18,
                        radius * 0.45, radius * 0.35, radius * 0.45, 0.18);
                area.world().spawnParticle(Particle.SQUID_INK, center, 3, 0.8, 0.8, 0.8, 0.02);
                if (tick % 10 == 0) {
                    area.world().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 0.45f + (float) progress);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void meteors(Player player, MineArea area, Location origin, double level) {
        double scale = levelScale("meteors", level);
        int meteorCount = lerpInt(plugin.getConfig().getInt("meteors.base-count", 7),
                plugin.getConfig().getInt("meteors.max-count", 14), scale);
        int interval = Math.max(2, plugin.getConfig().getInt("meteors.interval-ticks", 6));
        int impactRadius = Math.max(1, plugin.getConfig().getInt("meteors.impact-radius", 2));
        int searchRadius = Math.max(3, plugin.getConfig().getInt("meteors.search-radius", 12));
        int maxPerImpact = Math.max(8, plugin.getConfig().getInt("meteors.max-blocks-per-impact", 28));
        List<Meteor> active = new ArrayList<>();

        sendMessage(player, "messages.meteors-start",
                "&8[&c&l!&8] &c&lMeteor Shower &8→ &7Meteors are raining over the mine!");

        new BukkitRunnable() {
            int tick;
            int spawned;
            int processed;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    active.forEach(meteor -> remove(meteor.display));
                    end(player);
                    cancel();
                    return;
                }

                if (spawned < meteorCount && tick % interval == 0) {
                    Block target = randomTopBlock(area, origin, searchRadius);
                    spawned++;
                    if (target != null) {
                        Location impact = target.getLocation().add(0.5, 0.5, 0.5);
                        Location start = impact.clone().add(
                                ThreadLocalRandom.current().nextDouble(-5.0, 5.0),
                                15.0,
                                ThreadLocalRandom.current().nextDouble(-5.0, 5.0));
                        BlockDisplay display = spawnBlockDisplay(start.clone().subtract(0.45, 0.45, 0.45),
                                Material.MAGMA_BLOCK.createBlockData(), 0.9f, Color.RED);
                        active.add(new Meteor(display, start, impact, tick));
                        area.world().playSound(start, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.55f, 0.65f);
                    }
                }

                Iterator<Meteor> iterator = active.iterator();
                while (iterator.hasNext()) {
                    Meteor meteor = iterator.next();
                    int age = tick - meteor.spawnTick;
                    double progress = Math.min(1.0, age / 14.0);
                    Vector travel = meteor.impact.toVector().subtract(meteor.start.toVector()).multiply(progress);
                    Location next = meteor.start.clone().add(travel);
                    meteor.display.teleport(next);
                    area.world().spawnParticle(Particle.FLAME, next, 4, 0.18, 0.18, 0.18, 0.015);
                    area.world().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, next, 2, 0.12, 0.12, 0.12, 0.01);

                    if (progress >= 1.0) {
                        remove(meteor.display);
                        List<Block> impactBlocks = collectSphere(area, meteor.impact, impactRadius, maxPerImpact);
                        processed += prison.process(player, impactBlocks, "meteors");
                        area.world().spawnParticle(Particle.EXPLOSION, meteor.impact, 4, 1.0, 0.7, 1.0, 0.05);
                        area.world().spawnParticle(Particle.LAVA, meteor.impact, 12, 1.3, 0.6, 1.3, 0.08);
                        area.world().playSound(meteor.impact, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.25f);
                        iterator.remove();
                    }
                }

                if (spawned >= meteorCount && active.isEmpty()) {
                    sendMessage(player, "messages.meteors-result",
                            "&8[&c&l!&8] &c&lMeteor Shower &8→ &7Destroyed &f%blocks% &7blocks!",
                            "%blocks%", String.valueOf(processed));
                    end(player);
                    cancel();
                    return;
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void acidRain(Player player, MineArea area, Location origin, double level) {
        double scale = levelScale("acidrain", level);
        int duration = plugin.getConfig().getInt("acidrain.duration-ticks", 100);
        int interval = Math.max(2, plugin.getConfig().getInt("acidrain.pulse-interval-ticks", 5));
        int radius = Math.max(3, plugin.getConfig().getInt("acidrain.radius", 9));
        int columns = lerpInt(plugin.getConfig().getInt("acidrain.base-columns", 5),
                plugin.getConfig().getInt("acidrain.max-columns", 10), scale);
        int maxBlocks = Math.max(1, plugin.getConfig().getInt("acidrain.max-total-blocks", 130));
        int fallTicks = Math.max(4, plugin.getConfig().getInt("acidrain.fall-ticks", 12));
        double spawnHeight = Math.max(6.0, plugin.getConfig().getDouble("acidrain.spawn-height", 14.0));
        List<Column> targets = randomColumns(area, origin, radius, columns);
        List<AcidDrop> drops = new ArrayList<>();
        WeatherType oldWeather = player.getPlayerWeather();
        player.setPlayerWeather(WeatherType.DOWNFALL);
        sendMessage(player, "messages.acidrain-start",
                "&8[&a&l!&8] &a&lAcid Rain &8→ &7Corrosive rain is falling over the mine!");
        area.world().playSound(player.getLocation(), Sound.WEATHER_RAIN, 1.0f, 0.75f);

        new BukkitRunnable() {
            int tick;
            int processed;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    restoreWeather(player, oldWeather);
                    drops.clear();
                    end(player);
                    cancel();
                    return;
                }

                if (tick < duration && processed < maxBlocks && tick % interval == 0) {
                    for (Column column : targets) {
                        Block top = topSolid(area, column.x, column.z);
                        if (top == null) continue;
                        double x = column.x + ThreadLocalRandom.current().nextDouble(0.18, 0.82);
                        double z = column.z + ThreadLocalRandom.current().nextDouble(0.18, 0.82);
                        double maxSpawnY = area.world().getMaxHeight() - 2.0;
                        double startY = Math.min(maxSpawnY, Math.max(area.maxY() + 4.0, top.getY() + spawnHeight));
                        drops.add(new AcidDrop(column, x, z, startY, tick));
                    }
                }

                Set<Block> impacts = new LinkedHashSet<>();
                Iterator<AcidDrop> iterator = drops.iterator();
                while (iterator.hasNext()) {
                    AcidDrop drop = iterator.next();
                    Block currentTop = topSolid(area, drop.column.x, drop.column.z);
                    if (currentTop == null) {
                        iterator.remove();
                        continue;
                    }

                    double progress = Math.min(1.0, (tick - drop.spawnTick) / (double) fallTicks);
                    double eased = progress * progress;
                    double impactY = currentTop.getY() + 1.05;
                    double y = drop.startY + (impactY - drop.startY) * eased;
                    Location at = new Location(area.world(), drop.x, y, drop.z);
                    renderAcidStreak(at);

                    if (progress >= 1.0) {
                        if (processed + impacts.size() < maxBlocks) impacts.add(currentTop);
                        renderAcidSplash(currentTop.getLocation().add(0.5, 1.0, 0.5));
                        iterator.remove();
                    }
                }

                if (!impacts.isEmpty()) {
                    processed += prison.process(player, new ArrayList<>(impacts), "acidrain");
                    area.world().playSound(origin, Sound.BLOCK_FIRE_EXTINGUISH, 0.65f, 1.55f);
                }

                if ((tick >= duration || processed >= maxBlocks) && drops.isEmpty()) {
                    restoreWeather(player, oldWeather);
                    sendMessage(player, "messages.acidrain-result",
                            "&8[&a&l!&8] &a&lAcid Rain &8→ &7Melted &f%blocks% &7blocks!",
                            "%blocks%", String.valueOf(processed));
                    end(player);
                    cancel();
                    return;
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void renderAcidStreak(Location head) {
        Particle.DustTransition acid = new Particle.DustTransition(
                Color.fromRGB(185, 255, 45), Color.fromRGB(35, 185, 15), 1.35f);
        for (int i = 0; i < 4; i++) {
            areaParticle(head.clone().add(0, i * 0.38, 0), Particle.DUST_COLOR_TRANSITION, acid);
        }
    }

    private <T> void areaParticle(Location location, Particle particle, T data) {
        location.getWorld().spawnParticle(particle, location, 1, 0.025, 0.025, 0.025, 0.0, data);
    }

    private void renderAcidSplash(Location location) {
        Particle.DustTransition acid = new Particle.DustTransition(
                Color.fromRGB(205, 255, 70), Color.fromRGB(25, 150, 10), 1.7f);
        location.getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, location, 14,
                0.42, 0.16, 0.42, 0.03, acid);
        location.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, location, 4,
                0.3, 0.12, 0.3, 0.015);
    }

    private void sendMessage(Player player, String path, String fallback, String... replacements) {
        String message = plugin.getConfig().getString(path, fallback);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private void restoreWeather(Player player, WeatherType oldWeather) {
        if (!player.isOnline()) return;
        if (oldWeather == null) player.resetPlayerWeather();
        else player.setPlayerWeather(oldWeather);
    }

    private BlockDisplay spawnBlockDisplay(Location location, BlockData data, float scale, Color glow) {
        BlockDisplay display = location.getWorld().spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(data);
            entity.setInterpolationDuration(2);
            entity.setTeleportDuration(2);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setGlowing(true);
            entity.setGlowColorOverride(glow);
            float shift = (1.0f - scale) / 2.0f;
            entity.setTransformation(new Transformation(
                    new Vector3f(shift, shift, shift), new AxisAngle4f(),
                    new Vector3f(scale, scale, scale), new AxisAngle4f()));
        });
        spawnedEntities.add(display);
        return display;
    }

    private void remove(Entity entity) {
        if (entity != null && entity.isValid()) entity.remove();
        spawnedEntities.remove(entity);
    }

    private List<Block> collectCylinder(MineArea area, Location center, double radius,
                                        int verticalRadius, int maxBlocks) {
        List<Block> candidates = new ArrayList<>();
        int r = (int) Math.ceil(radius);
        int minY = Math.max(area.minY(), center.getBlockY() - verticalRadius);
        int maxY = Math.min(area.maxY(), center.getBlockY() + verticalRadius);
        for (int x = center.getBlockX() - r; x <= center.getBlockX() + r; x++) {
            for (int z = center.getBlockZ() - r; z <= center.getBlockZ() + r; z++) {
                double dx = x + 0.5 - center.getX();
                double dz = z + 0.5 - center.getZ();
                if (dx * dx + dz * dz > radius * radius) continue;
                for (int y = minY; y <= maxY; y++) addCandidate(area, candidates, x, y, z);
            }
        }
        return shuffledLimit(candidates, maxBlocks);
    }

    private List<Block> collectSphere(MineArea area, Location center, double radius, int maxBlocks) {
        List<Block> candidates = new ArrayList<>();
        int r = (int) Math.ceil(radius);
        for (int x = center.getBlockX() - r; x <= center.getBlockX() + r; x++) {
            for (int y = Math.max(area.minY(), center.getBlockY() - r);
                 y <= Math.min(area.maxY(), center.getBlockY() + r); y++) {
                for (int z = center.getBlockZ() - r; z <= center.getBlockZ() + r; z++) {
                    double dx = x + 0.5 - center.getX();
                    double dy = y + 0.5 - center.getY();
                    double dz = z + 0.5 - center.getZ();
                    if (dx * dx + dy * dy + dz * dz <= radius * radius) {
                        addCandidate(area, candidates, x, y, z);
                    }
                }
            }
        }
        return shuffledLimit(candidates, maxBlocks);
    }

    private void addCandidate(MineArea area, List<Block> candidates, int x, int y, int z) {
        if (!area.contains(x, y, z) || !area.world().isChunkLoaded(x >> 4, z >> 4)) return;
        Block block = area.world().getBlockAt(x, y, z);
        if (isBreakable(block)) candidates.add(block);
    }

    private List<Block> shuffledLimit(List<Block> candidates, int maxBlocks) {
        Collections.shuffle(candidates);
        if (candidates.size() <= maxBlocks) return candidates;
        return new ArrayList<>(candidates.subList(0, Math.max(0, maxBlocks)));
    }

    private Block randomTopBlock(MineArea area, Location center, int radius) {
        for (int attempt = 0; attempt < 35; attempt++) {
            int x = center.getBlockX() + ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int z = center.getBlockZ() + ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            Block top = topSolid(area, x, z);
            if (top != null) return top;
        }
        return null;
    }

    private List<Column> randomColumns(MineArea area, Location center, int radius, int count) {
        Set<Column> columns = new LinkedHashSet<>();
        for (int attempt = 0; attempt < count * 20 && columns.size() < count; attempt++) {
            int x = center.getBlockX() + ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            int z = center.getBlockZ() + ThreadLocalRandom.current().nextInt(-radius, radius + 1);
            if (topSolid(area, x, z) != null) columns.add(new Column(x, z));
        }
        return new ArrayList<>(columns);
    }

    private Block topSolid(MineArea area, int x, int z) {
        if (!area.world().isChunkLoaded(x >> 4, z >> 4)) return null;
        for (int y = area.maxY(); y >= area.minY(); y--) {
            if (!area.contains(x, y, z)) continue;
            Block block = area.world().getBlockAt(x, y, z);
            if (isBreakable(block)) return block;
        }
        return null;
    }

    private boolean isBreakable(Block block) {
        Material material = block.getType();
        return !material.isAir() && !block.isLiquid() && material.isBlock()
                && !PROTECTED_MATERIALS.contains(material) && material.getHardness() >= 0.0f;
    }

    private double levelScale(String path, double level) {
        double max = Math.max(1.0, plugin.getConfig().getDouble(path + ".max-enchant-level", 10000.0));
        return Math.max(0.0, Math.min(1.0, level / max));
    }

    private double lerp(double min, double max, double scale) {
        return min + (max - min) * scale;
    }

    private int lerpInt(int min, int max, double scale) {
        return (int) Math.round(lerp(min, max, scale));
    }

    public void shutdown() {
        for (Entity entity : new ArrayList<>(spawnedEntities)) remove(entity);
        activePlayers.clear();
    }

    private record FlyingBlock(BlockDisplay display, Location start, double angle, double radius) {}
    private record PullBlock(BlockDisplay display, Location start, double phase) {}
    private record Meteor(BlockDisplay display, Location start, Location impact, int spawnTick) {}
    private record Column(int x, int z) {}
    private record AcidDrop(Column column, double x, double z, double startY, int spawnTick) {}
}
