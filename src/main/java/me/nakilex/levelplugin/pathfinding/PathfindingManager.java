package me.nakilex.levelplugin.pathfinding;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.event.NavigationStuckEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.trait.Equipment;
import io.lumine.mythic.bukkit.MythicBukkit;
import me.nakilex.levelplugin.spells.managers.CooldownManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages editable location sequences and executes them with Citizens NPCs.
 * Designed to be generic so different systems can reuse stored paths.
 */
public class PathfindingManager {
    private final Plugin plugin;
    private final Map<Integer, Location> editingPoints = new HashMap<>();
    private final Map<String, List<Location>> paths = new HashMap<>();
    private final File file;
    private final FileConfiguration config;

    public PathfindingManager(Plugin plugin) {
        this.plugin = plugin;
        plugin.saveResource("paths.yml", false);
        this.file = new File(plugin.getDataFolder(), "paths.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadPaths();
    }

    private void loadPaths() {
        if (!config.isConfigurationSection("paths")) {
            return;
        }
        for (String name : config.getConfigurationSection("paths").getKeys(false)) {
            List<Location> list = (List<Location>) config.getList("paths." + name);
            if (list != null) {
                paths.put(name.toLowerCase(Locale.ROOT), list);
            }
        }
    }

    private void savePath(String name, List<Location> list) {
        config.set("paths." + name, list);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Store or replace a temporary point for the next path creation. */
    public void setPoint(int index, Location loc) {
        editingPoints.put(index, loc);
    }

    /**
     * Creates a named path from the currently stored points.
     * Points are ordered by their numeric index.
     */
    public void createPath(String name) {
        List<Location> list = editingPoints.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
        String key = name.toLowerCase(Locale.ROOT);
        paths.put(key, list);
        savePath(key, list);
        editingPoints.clear();
    }

    /** Execute a previously created path. */
    public void executePath(String name) {
        List<Location> list = paths.get(name.toLowerCase(Locale.ROOT));
        if (list == null || list.isEmpty()) {
            return;
        }
        new PathRunner(plugin, list).start();
    }

    public Set<String> getPathNames() {
        return paths.keySet();
    }

    public int nextPointIndex() {
        return editingPoints.size() + 1;
    }

    private static class PathRunner implements Listener {
        private final Plugin plugin;
        private final NPC npc;
        private final List<Location> points;
        private BukkitTask task;
        private int index = 1;
        private LivingEntity combatTarget;
        private static final String SKILL_DASH = "awakassassin_ravagingdash";
        private static final String SKILL_LETHAL = "awakassassin_lethalcombo";
        private static final String SKILL_SHADOW = "awakassassin_shadowstep";
        private static final String SKILL_FLURRY = "awakassassin_bladeflurry";
        private final CooldownManager cd = CooldownManager.getInstance();

        PathRunner(Plugin plugin, List<Location> points) {
            this.plugin = plugin;
            this.points = points;
            this.npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "PathNPC");
            Bukkit.getPluginManager().registerEvents(this, plugin);
        }

        void start() {
            npc.spawn(points.get(0));

            // Increase walking speed 1.5x beyond the previous triple boost
            var params = npc.getNavigator().getDefaultParameters();
            params.baseSpeed(params.baseSpeed() * 4.5f);

            // Equip the NPC with mythic weapon and netherite armor
            equipGear();

            if (points.size() <= 1) {
                cleanup();
                return;
            }
            npc.getNavigator().setTarget(points.get(1));
            task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (combatTarget != null) {
                    if (combatTarget.isDead() || !combatTarget.isValid()) {
                        combatTarget = null;
                        npc.getNavigator().setTarget(points.get(index));
                        return;
                    }

                    Location npcLoc = npc.getEntity().getLocation();
                    Location targetLoc = combatTarget.getLocation();
                    double distSq = npcLoc.distanceSquared(targetLoc);

                    if (distSq > 9) {
                        if (!castWithCooldown(SKILL_DASH, 5, combatTarget)) {
                            npc.getNavigator().setTarget(combatTarget, true);
                        }
                    } else {
                        castWithCooldown(SKILL_LETHAL, 1, combatTarget);
                        castWithCooldown(SKILL_SHADOW, 8, combatTarget);
                        castWithCooldown(SKILL_FLURRY, 10, combatTarget);
                    }
                    return;
                }

                LivingEntity hostile = findNearestHostile();
                if (hostile != null) {
                    combatTarget = hostile;
                    castWithCooldown(SKILL_DASH, 5, hostile);
                    return;
                }

                if (!npc.getNavigator().isNavigating()) {
                    if (++index >= points.size()) {
                        cleanup();
                        return;
                    }
                    npc.getNavigator().setTarget(points.get(index));
                }
            }, 10L, 10L);
        }

        @EventHandler
        public void onStuck(NavigationStuckEvent event) {
            if (event.getNPC().equals(npc)) {
                if (combatTarget != null && combatTarget.isValid()) {
                    npc.getNavigator().setTarget(combatTarget, true);
                } else {
                    npc.getNavigator().setTarget(points.get(index));
                }
            }
        }

        private void cleanup() {
            if (task != null) {
                task.cancel();
            }
            npc.despawn();
            npc.destroy();
            HandlerList.unregisterAll(this);
        }

        private LivingEntity findNearestHostile() {
            if (!(npc.getEntity() instanceof LivingEntity le)) {
                return null;
            }
            Location loc = le.getLocation();
            double radius = 10;
            return loc.getWorld().getNearbyEntities(loc, radius, radius, radius).stream()
                    .filter(e -> e instanceof Monster)
                    .map(e -> (LivingEntity) e)
                    .min(Comparator.comparingDouble(e -> e.getLocation().distanceSquared(loc)))
                    .orElse(null);
        }

        private void equipGear() {
            Equipment equip = npc.getOrAddTrait(Equipment.class);
            ItemStack weapon = MythicBukkit.inst().getItemManager()
                    .getItemStack("awakened_assassin_duskblade")
                    .orElse(new ItemStack(Material.NETHERITE_SWORD));
            equip.set(Equipment.EquipmentSlot.HAND, weapon);
            equip.set(Equipment.EquipmentSlot.HELMET, new ItemStack(Material.NETHERITE_HELMET));
            equip.set(Equipment.EquipmentSlot.CHESTPLATE, new ItemStack(Material.NETHERITE_CHESTPLATE));
            equip.set(Equipment.EquipmentSlot.LEGGINGS, new ItemStack(Material.NETHERITE_LEGGINGS));
            equip.set(Equipment.EquipmentSlot.BOOTS, new ItemStack(Material.NETHERITE_BOOTS));
        }

        private boolean castWithCooldown(String skill, double cooldownSeconds, LivingEntity target) {
            UUID id = npc.getEntity().getUniqueId();
            if (cd.isOnCooldown(id, skill)) {
                return false;
            }
            try {
                MythicBukkit.inst().getAPIHelper().castSkill(npc.getEntity(), skill, target);
            } catch (NoSuchMethodError e) {
                MythicBukkit.inst().getAPIHelper().castSkill(npc.getEntity(), skill);
            }
            cd.setCooldown(id, skill, cooldownSeconds);
            return true;
        }
    }
}

