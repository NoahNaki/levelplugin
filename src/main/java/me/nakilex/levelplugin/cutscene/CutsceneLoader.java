package me.nakilex.levelplugin.cutscene;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.cutscene.actor.ActorDefinition;
import me.nakilex.levelplugin.cutscene.effects.EffectSettings;
import me.nakilex.levelplugin.cutscene.frames.ActorActionFrame;
import me.nakilex.levelplugin.cutscene.frames.BranchFrame;
import me.nakilex.levelplugin.cutscene.frames.DialogueFrame;
import me.nakilex.levelplugin.cutscene.frames.EffectFrame;
import me.nakilex.levelplugin.cutscene.frames.Frame;
import me.nakilex.levelplugin.cutscene.frames.Keyframe;
import me.nakilex.levelplugin.cutscene.frames.TeleportFrame;
import me.nakilex.levelplugin.cutscene.frames.WaitFrame;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CutsceneLoader {
    private CutsceneLoader() {}

    public static Cutscene load(Main plugin, FileConfiguration cfg, String fallbackId) throws CutsceneFormatException {
        int version = cfg.getInt("version", 1);
        String id = cfg.getString("id", fallbackId);
        if (version < 2) {
            return loadLegacy(plugin, cfg, id);
        }
        CutsceneMetadata metadata = parseMetadata(cfg.getConfigurationSection("metadata"));
        Map<String, EffectSettings> bundles = parseEffectBundles(cfg.getConfigurationSection("effectBundles"));
        Map<String, ActorDefinition> actors = parseActors(plugin, cfg.getConfigurationSection("actors"), bundles);
        List<Frame> frames = parseFrames(plugin, cfg.getMapList("frames"), bundles);
        return new Cutscene(id, version, metadata, actors, bundles, frames);
    }

    private static Cutscene loadLegacy(Main plugin, FileConfiguration cfg, String id) throws CutsceneFormatException {
        List<Map<?, ?>> frameSection = cfg.getMapList("frames");
        List<Frame> frames = new ArrayList<>();
        for (Map<?, ?> map : frameSection) {
            String pos = (String) map.get("pos");
            String world = map.containsKey("world") ? (String) map.get("world") : null;
            String type = map.containsKey("type") ? (String) map.get("type") : "teleport";
            String lookAtStr = (String) map.get("lookAt");
            long duration = map.get("duration") != null ? ((Number) map.get("duration")).longValue() : 2000L;
            double speed = map.get("speed") != null ? ((Number) map.get("speed")).doubleValue() : 0.0;
            String title = (String) map.get("title");
            String subtitle = (String) map.get("subtitle");
            String actionBar = (String) map.get("actionBar");
            String sound = (String) map.get("sound");
            String command = (String) map.get("command");
            Location loc = CutsceneIO.parseLocation(plugin, world, pos);
            Location lookAt = CutsceneIO.parseVector(plugin, world, lookAtStr);
            if ("key".equalsIgnoreCase(type) || "keyframe".equalsIgnoreCase(type)) {
                frames.add(new Keyframe(loc, lookAt, duration, world));
            } else {
                EffectSettings effects = EffectSettings.fromLegacy(title, subtitle, actionBar, sound, command);
                frames.add(new TeleportFrame(loc, duration, effects, world, speed));
            }
        }
        return new Cutscene(id, 1, new CutsceneMetadata("", List.of(), false, List.of()), Map.of(), Map.of(), frames);
    }

    private static CutsceneMetadata parseMetadata(ConfigurationSection section) {
        if (section == null) {
            return new CutsceneMetadata("", List.of(), false, List.of());
        }
        String description = section.getString("description", "");
        List<String> tags = section.getStringList("tags");
        boolean autoStart = section.getBoolean("autoStart", false);
        List<String> endCommands = section.getStringList("endCommands");
        return new CutsceneMetadata(description, tags, autoStart, endCommands);
    }

    private static Map<String, EffectSettings> parseEffectBundles(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, EffectSettings> bundles = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Map<String, Object> raw = new HashMap<>();
            for (String inner : section.getConfigurationSection(key).getKeys(false)) {
                raw.put(inner, section.getConfigurationSection(key).get(inner));
            }
            bundles.put(key.toLowerCase(Locale.ROOT), EffectSettings.fromMap(raw, bundles::get));
        }
        return bundles;
    }

    private static Map<String, ActorDefinition> parseActors(Main plugin, ConfigurationSection section,
                                                            Map<String, EffectSettings> bundles) throws CutsceneFormatException {
        if (section == null) {
            return Map.of();
        }
        Map<String, ActorDefinition> actors = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            Map<?, ?> data = section.getConfigurationSection(key).getValues(false);
            actors.put(key.toLowerCase(Locale.ROOT), ActorDefinition.fromConfig(plugin, key, data, bundles::get));
        }
        return actors;
    }

    private static List<Frame> parseFrames(Main plugin, List<Map<?, ?>> list,
                                           Map<String, EffectSettings> bundles) throws CutsceneFormatException {
        List<Frame> frames = new ArrayList<>();
        for (Map<?, ?> raw : list) {
            String type = raw.get("type") instanceof String t ? t.toLowerCase(Locale.ROOT) : "teleport";
            long duration = raw.get("duration") instanceof Number num ? num.longValue() : 2000L;
            switch (type) {
                case "teleport" -> frames.add(parseTeleportFrame(plugin, raw, duration, bundles));
                case "key" -> frames.add(parseKeyframe(plugin, raw, duration));
                case "keyframe" -> frames.add(parseKeyframe(plugin, raw, duration));
                case "dialogue" -> frames.add(parseDialogue(raw, duration, bundles));
                case "effect" -> frames.add(new EffectFrame(parseEffects(raw, bundles), duration));
                case "wait" -> frames.add(new WaitFrame(duration, raw.get("actor") instanceof String s ? s : null));
                case "actor" -> frames.add(parseActorAction(raw, duration));
                case "branch" -> frames.add(parseBranch(raw, duration));
                default -> throw new CutsceneFormatException("Unknown frame type '" + type + "'");
            }
        }
        return frames;
    }

    private static Frame parseTeleportFrame(Main plugin, Map<?, ?> raw, long duration,
                                            Map<String, EffectSettings> bundles) {
        String pos = raw.get("pos") instanceof String s ? s : null;
        String world = raw.get("world") instanceof String s ? s : null;
        double speed = raw.get("speed") instanceof Number num ? num.doubleValue() : 0.0;
        Location location = CutsceneIO.parseLocation(plugin, world, pos);
        EffectSettings effects = parseEffects(raw, bundles);
        return new TeleportFrame(location, duration, effects, world, speed);
    }

    private static Frame parseKeyframe(Main plugin, Map<?, ?> raw, long duration) {
        String pos = raw.get("pos") instanceof String s ? s : null;
        String world = raw.get("world") instanceof String s ? s : null;
        String lookAtStr = raw.get("lookAt") instanceof String s ? s : null;
        Location location = CutsceneIO.parseLocation(plugin, world, pos);
        Location lookAt = CutsceneIO.parseVector(plugin, world, lookAtStr);
        return new Keyframe(location, lookAt, duration, world);
    }

    private static Frame parseDialogue(Map<?, ?> raw, long duration, Map<String, EffectSettings> bundles) {
        String speaker = raw.get("actor") instanceof String s ? s : null;
        String text = raw.get("text") instanceof String s ? s : null;
        String subtitle = raw.get("subtitle") instanceof String s ? s : null;
        EffectSettings effects = parseEffects(raw, bundles);
        return new DialogueFrame(speaker, text, subtitle, duration, effects);
    }

    private static Frame parseActorAction(Map<?, ?> raw, long duration) {
        String actor = raw.get("actor") instanceof String s ? s : null;
        String action = raw.get("action") instanceof String s ? s : null;
        Map<String, Object> params = new LinkedHashMap<>();
        if (raw.get("params") instanceof Map<?, ?> map) {
            map.forEach((key, value) -> {
                if (key instanceof String str) {
                    params.put(str, value);
                }
            });
        }
        if (params.isEmpty()) {
            raw.forEach((key, value) -> {
                if (key instanceof String str && !List.of("type", "actor", "action", "duration").contains(str)) {
                    params.put(str, value);
                }
            });
        }
        return new ActorActionFrame(actor, action, params, duration);
    }

    private static Frame parseBranch(Map<?, ?> raw, long duration) {
        String permission = raw.get("permission") instanceof String s ? s : null;
        boolean invert = raw.get("invert") instanceof Boolean b ? b
                : raw.get("invert") instanceof String str && Boolean.parseBoolean(str);
        String message = raw.get("message") instanceof String s ? s : null;
        return new BranchFrame(permission, invert, message, duration);
    }

    private static EffectSettings parseEffects(Map<?, ?> raw, Map<String, EffectSettings> bundles) {
        Map<?, ?> effectMap = raw.get("effects") instanceof Map<?, ?> map ? map : null;
        return EffectSettings.fromMap(effectMap, bundles::get);
    }
}
