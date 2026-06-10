package me.nakilex.levelplugin.environment.npc;

import me.nakilex.levelplugin.Main;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class KingdomNpcSoundManager {
    private final Main plugin;
    private final Map<String, KingdomNpcSoundProfile> profilesByKey = new HashMap<>();
    private final Map<Integer, BukkitTask> tasksByNpcId = new HashMap<>();

    public KingdomNpcSoundManager(Main plugin) {
        this.plugin = plugin;
        registerDefaultProfiles();
    }

    private void registerDefaultProfiles() {
        registerProfile("blacksmith", new KingdomNpcSoundProfile(249L, List.of(
                new KingdomNpcSoundCue("block.anvil.place", 0.2F, 1.0F, 0L),
                new KingdomNpcSoundCue("block.anvil.place", 0.2F, 1.0F, 15L),
                new KingdomNpcSoundCue("block.anvil.place", 0.2F, 1.0F, 28L),
                new KingdomNpcSoundCue("block.anvil.place", 0.2F, 1.0F, 45L),
                new KingdomNpcSoundCue("block.anvil.place", 0.2F, 1.0F, 56L),
                new KingdomNpcSoundCue("block.anvil.place", 0.2F, 1.0F, 69L),
                new KingdomNpcSoundCue("block.anvil.place", 0.2F, 1.0F, 86L),
                new KingdomNpcSoundCue("block.anvil.place", 0.2F, 1.0F, 103L),
                new KingdomNpcSoundCue("block.anvil.place", 0.2F, 1.0F, 112L),
                new KingdomNpcSoundCue("block.anvil.place", 0.2F, 1.0F, 123L)
        )));
        registerAlias("scene_blacksmith_1", "blacksmith");
        registerAlias("scene blacksmith 1", "blacksmith");
        registerAlias("scene_blacksmith_1.bbmodel", "blacksmith");
    }

    public void registerProfile(String key, KingdomNpcSoundProfile profile) {
        String normalized = normalizeKey(key);
        if (normalized.isBlank() || profile == null) {
            return;
        }
        profilesByKey.put(normalized, profile);
    }

    public void registerAlias(String alias, String targetKey) {
        KingdomNpcSoundProfile target = resolveProfile(targetKey);
        if (target != null) {
            registerProfile(alias, target);
        }
    }

    public void startConfiguredSoundLoop(String npcName,
                                         String buildingId,
                                         String buildingDisplayName,
                                         String modelId,
                                         NPC npc) {
        if (npc == null) {
            return;
        }
        KingdomNpcSoundProfile profile = resolveProfile(npcName, buildingId, buildingDisplayName, modelId);
        if (profile == null || profile.intervalTicks() <= 0 || profile.cues().isEmpty()) {
            return;
        }

        stop(npc.getId());
        int npcId = npc.getId();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isUsable(npc)) {
                    stop(npcId);
                    cancel();
                    return;
                }
                for (KingdomNpcSoundCue cue : profile.cues()) {
                    if (cue == null || cue.sound() == null || cue.sound().isBlank()) {
                        continue;
                    }
                    Bukkit.getScheduler().runTaskLater(plugin, () -> playCue(npc, cue), Math.max(0L, cue.delayTicks()));
                }
            }
        }.runTaskTimer(plugin, 20L, profile.intervalTicks());

        tasksByNpcId.put(npcId, task);
        plugin.getLogger().info("[EnvironmentArea/NpcSound] Started sound profile for npcName='"
                + npcName + "' building='" + nullSafe(buildingId)
                + "' model='" + nullSafe(modelId)
                + "' npcId=" + npcId
                + " intervalTicks=" + profile.intervalTicks()
                + " cues=" + profile.cues().size());
    }

    public void stop(int npcId) {
        BukkitTask existing = tasksByNpcId.remove(npcId);
        if (existing != null) {
            existing.cancel();
        }
    }

    public void stopAll() {
        for (BukkitTask task : new ArrayList<>(tasksByNpcId.values())) {
            if (task != null) {
                task.cancel();
            }
        }
        tasksByNpcId.clear();
    }

    private void playCue(NPC npc, KingdomNpcSoundCue cue) {
        if (!isUsable(npc) || cue == null) {
            return;
        }
        Location location = npc.getEntity().getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(location, cue.sound(), cue.volume(), cue.pitch());
    }

    private boolean isUsable(NPC npc) {
        return npc != null
                && npc.isSpawned()
                && npc.getEntity() != null
                && npc.getEntity().isValid();
    }

    private KingdomNpcSoundProfile resolveProfile(String... keys) {
        for (String key : keys) {
            KingdomNpcSoundProfile exact = resolveProfile(key);
            if (exact != null) {
                return exact;
            }
        }
        for (String key : keys) {
            String normalized = normalizeKey(key);
            if (normalized.isBlank()) {
                continue;
            }
            for (Map.Entry<String, KingdomNpcSoundProfile> entry : profilesByKey.entrySet()) {
                if (normalized.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private KingdomNpcSoundProfile resolveProfile(String key) {
        String normalized = normalizeKey(key);
        if (normalized.isBlank()) {
            return null;
        }
        return profilesByKey.get(normalized);
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', key));
        if (stripped == null) {
            return "";
        }
        return stripped.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll(" +", " ");
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
