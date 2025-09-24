package me.nakilex.levelplugin.cutscene.actor;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cutscene.CutsceneFormatException;
import me.nakilex.levelplugin.cutscene.CutsceneIO;
import me.nakilex.levelplugin.cutscene.effects.EffectSettings;
import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** Parsed configuration backing a {@link CutsceneActor}. */
public final class ActorDefinition {
    public enum Type { NPC }

    private final String name;
    private final Type type;
    private final String profileId;
    private final String defaultPath;
    private final Location spawn;
    private final boolean persistent;
    private final EffectSettings spawnEffects;
    private final Location lookAt;

    private ActorDefinition(String name, Type type, String profileId, String defaultPath,
                            Location spawn, boolean persistent, EffectSettings spawnEffects,
                            Location lookAt) {
        this.name = name;
        this.type = type;
        this.profileId = profileId;
        this.defaultPath = defaultPath;
        this.spawn = spawn;
        this.persistent = persistent;
        this.spawnEffects = spawnEffects;
        this.lookAt = lookAt;
    }

    public String name() {
        return name;
    }

    public Type type() {
        return type;
    }

    public String profileId() {
        return profileId;
    }

    public String defaultPath() {
        return defaultPath;
    }

    public Location spawn() {
        return spawn;
    }

    public boolean persistent() {
        return persistent;
    }

    public EffectSettings spawnEffects() {
        return spawnEffects;
    }

    public Location lookAt() {
        return lookAt;
    }

    public CutsceneActor create(Main plugin) {
        return switch (type) {
            case NPC -> new NpcActor(this, plugin);
        };
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type.name().toLowerCase(Locale.ROOT));
        if (profileId != null) map.put("profile", profileId);
        if (defaultPath != null) map.put("path", defaultPath);
        if (spawn != null) {
            map.put("spawn", CutsceneIO.formatLocation(spawn));
            if (spawn.getWorld() != null) {
                map.put("world", spawn.getWorld().getName());
            }
        }
        if (persistent) map.put("persistent", true);
        if (!spawnEffects.isEmpty()) map.put("effects", spawnEffects.toMap());
        if (lookAt != null) map.put("lookAt", CutsceneIO.formatVector(lookAt));
        return map;
    }

    @SuppressWarnings("unchecked")
    public static ActorDefinition fromConfig(Main plugin, String name, Map<?, ?> data,
                                             Function<String, EffectSettings> bundleResolver)
            throws CutsceneFormatException {
        if (data == null) {
            throw new CutsceneFormatException("Actor '" + name + "' has no data section");
        }
        Object typeObj = data.get("type");
        Type type = Type.NPC;
        if (typeObj instanceof String str) {
            try {
                type = Type.valueOf(str.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new CutsceneFormatException("Unknown actor type '" + str + "' for actor '" + name + "'");
            }
        }
        String worldName = data.get("world") instanceof String s ? s : null;
        String profile = data.get("profile") instanceof String s ? s : null;
        String path = data.get("path") instanceof String s ? s : null;
        Location spawn = data.get("spawn") instanceof String pos ?
                CutsceneIO.parseLocation(plugin, worldName, pos) : null;
        Location lookAt = data.get("lookAt") instanceof String pos ?
                CutsceneIO.parseVector(plugin, worldName, pos) : null;
        boolean persistent = data.get("persistent") instanceof Boolean bool ? bool :
                data.get("persistent") instanceof String boolStr && Boolean.parseBoolean(boolStr);
        EffectSettings effects = EffectSettings.fromMap((Map<?, ?>) data.get("effects"),
                bundleResolver);
        return new ActorDefinition(name, type, profile, path, spawn, persistent, effects, lookAt);
    }
}
