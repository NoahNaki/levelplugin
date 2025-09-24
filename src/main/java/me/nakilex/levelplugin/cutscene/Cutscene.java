package me.nakilex.levelplugin.cutscene;

import me.nakilex.levelplugin.cutscene.actor.ActorDefinition;
import me.nakilex.levelplugin.cutscene.effects.EffectSettings;
import me.nakilex.levelplugin.cutscene.frames.Frame;
import org.bukkit.Location;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Cutscene {
    private final String id;
    private final int version;
    private final CutsceneMetadata metadata;
    private final Map<String, ActorDefinition> actors;
    private final Map<String, EffectSettings> effectBundles;
    private final List<Frame> frames;

    public Cutscene(String id, int version, CutsceneMetadata metadata,
                    Map<String, ActorDefinition> actors,
                    Map<String, EffectSettings> effectBundles,
                    List<Frame> frames) {
        this.id = id;
        this.version = version;
        this.metadata = metadata == null ? new CutsceneMetadata("", List.of(), false, List.of()) : metadata;
        this.actors = actors == null ? Map.of() : Map.copyOf(actors);
        this.effectBundles = effectBundles == null ? Map.of() : Map.copyOf(effectBundles);
        this.frames = frames == null ? List.of() : List.copyOf(frames);
    }

    public String getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public CutsceneMetadata getMetadata() {
        return metadata;
    }

    public Map<String, ActorDefinition> getActors() {
        return actors;
    }

    public Map<String, EffectSettings> getEffectBundles() {
        return effectBundles;
    }

    public EffectSettings getEffectBundle(String name) {
        return effectBundles.get(name);
    }

    public List<Frame> getFrames() {
        return frames;
    }

    public Map<String, Object> serializeMetadata() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (!metadata.isEmpty()) {
            map.putAll(metadata.toMap());
        }
        if (!effectBundles.isEmpty()) {
            Map<String, Object> bundles = new LinkedHashMap<>();
            effectBundles.forEach((key, value) -> bundles.put(key, value.toMap()));
            map.put("effectBundles", bundles);
        }
        if (!actors.isEmpty()) {
            Map<String, Object> actorMap = new LinkedHashMap<>();
            actors.forEach((key, value) -> actorMap.put(key, value.serialize()));
            map.put("actors", actorMap);
        }
        return map;
    }

    public Location resolveEndLocation() {
        Location end = null;
        for (Frame frame : frames) {
            Location loc = frame.getTargetLocation();
            if (loc != null) {
                end = loc;
            }
        }
        return end;
    }
}
