package me.nakilex.levelplugin.cutscene.effects;

import org.bukkit.Particle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Immutable description of the audiovisual effects attached to a cutscene
 * frame. The structure is intentionally generic so the same definition can be
 * reused by different frame types or shared via bundles in the YAML schema.
 */
public final class EffectSettings {
    private static final EffectSettings EMPTY = new EffectSettings(null, null, null,
            List.of(), List.of(), List.of());

    private final String title;
    private final String subtitle;
    private final String actionBar;
    private final List<String> commands;
    private final List<SoundCue> sounds;
    private final List<ParticleCue> particles;
    private final List<String> bundles;

    private EffectSettings(String title, String subtitle, String actionBar,
                           List<String> commands, List<SoundCue> sounds,
                           List<ParticleCue> particles) {
        this(title, subtitle, actionBar, commands, sounds, particles, List.of());
    }

    private EffectSettings(String title, String subtitle, String actionBar,
                           List<String> commands, List<SoundCue> sounds,
                           List<ParticleCue> particles, List<String> bundles) {
        this.title = title;
        this.subtitle = subtitle;
        this.actionBar = actionBar;
        this.commands = commands;
        this.sounds = sounds;
        this.particles = particles;
        this.bundles = bundles;
    }

    public static EffectSettings empty() {
        return EMPTY;
    }

    public String title() {
        return title;
    }

    public String subtitle() {
        return subtitle;
    }

    public String actionBar() {
        return actionBar;
    }

    public List<String> commands() {
        return commands;
    }

    public List<SoundCue> sounds() {
        return sounds;
    }

    public List<ParticleCue> particles() {
        return particles;
    }

    public List<String> bundleRefs() {
        return bundles;
    }

    public boolean isEmpty() {
        return title == null && subtitle == null && actionBar == null
                && commands.isEmpty() && sounds.isEmpty() && particles.isEmpty()
                && bundles.isEmpty();
    }

    public EffectSettings merge(EffectSettings other) {
        if (other == null || other.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return other;
        }
        List<String> mergedCommands = new ArrayList<>(commands);
        mergedCommands.addAll(other.commands);
        List<SoundCue> mergedSounds = new ArrayList<>(sounds);
        mergedSounds.addAll(other.sounds);
        List<ParticleCue> mergedParticles = new ArrayList<>(particles);
        mergedParticles.addAll(other.particles);
        List<String> mergedBundles = new ArrayList<>(bundles);
        mergedBundles.addAll(other.bundles);
        return new EffectSettings(other.title != null ? other.title : title,
                other.subtitle != null ? other.subtitle : subtitle,
                other.actionBar != null ? other.actionBar : actionBar,
                Collections.unmodifiableList(mergedCommands),
                Collections.unmodifiableList(mergedSounds),
                Collections.unmodifiableList(mergedParticles),
                Collections.unmodifiableList(mergedBundles));
    }

    public Map<String, Object> toMap() {
        if (isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (title != null) map.put("title", title);
        if (subtitle != null) map.put("subtitle", subtitle);
        if (actionBar != null) map.put("actionBar", actionBar);
        if (!commands.isEmpty()) map.put("commands", commands);
        if (!sounds.isEmpty()) {
            List<Map<String, Object>> soundList = new ArrayList<>();
            for (SoundCue cue : sounds) {
                soundList.add(cue.toMap());
            }
            map.put("sounds", soundList);
        }
        if (!particles.isEmpty()) {
            List<Map<String, Object>> particleList = new ArrayList<>();
            for (ParticleCue cue : particles) {
                particleList.add(cue.toMap());
            }
            map.put("particles", particleList);
        }
        if (!bundles.isEmpty()) {
            map.put("bundles", bundles);
        }
        return map;
    }

    public static EffectSettings fromLegacy(String title, String subtitle,
                                            String actionBar, String sound,
                                            String command) {
        Builder builder = builder();
        builder.title = title;
        builder.subtitle = subtitle;
        builder.actionBar = actionBar;
        if (sound != null) {
            builder.sounds.add(new SoundCue(sound, 1f, 1f));
        }
        if (command != null && !command.isEmpty()) {
            builder.commands.add(command);
        }
        return builder.build();
    }

    public static EffectSettings fromMap(Map<?, ?> raw,
                                         Function<String, EffectSettings> bundleResolver) {
        if (raw == null || raw.isEmpty()) {
            return empty();
        }
        Builder builder = builder();
        Object title = raw.get("title");
        Object subtitle = raw.get("subtitle");
        Object actionBar = raw.get("actionBar");
        if (title instanceof String t) builder.title = t;
        if (subtitle instanceof String s) builder.subtitle = s;
        if (actionBar instanceof String a) builder.actionBar = a;

        Object commands = raw.get("commands");
        if (commands instanceof List<?> list) {
            for (Object obj : list) {
                if (obj != null) builder.commands.add(obj.toString());
            }
        } else if (raw.get("command") instanceof String single) {
            builder.commands.add(single);
        }

        Object sounds = raw.get("sounds");
        if (sounds instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> map) {
                    SoundCue cue = SoundCue.fromMap(map);
                    if (cue != null) builder.sounds.add(cue);
                } else if (obj != null) {
                    builder.sounds.add(new SoundCue(obj.toString(), 1f, 1f));
                }
            }
        } else if (raw.get("sound") instanceof String singleSound) {
            builder.sounds.add(new SoundCue(singleSound, 1f, 1f));
        }

        Object particles = raw.get("particles");
        if (particles instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> map) {
                    ParticleCue cue = ParticleCue.fromMap(map);
                    if (cue != null) builder.particles.add(cue);
                }
            }
        }

        Object bundles = raw.get("bundles");
        if (bundles instanceof List<?> list) {
            for (Object obj : list) {
                if (obj != null) {
                    String name = obj.toString();
                    builder.bundleRefs.add(name);
                    if (bundleResolver != null) {
                        EffectSettings merged = bundleResolver.apply(name);
                        if (merged != null) {
                            builder.merge(merged);
                        }
                    }
                }
            }
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String title;
        private String subtitle;
        private String actionBar;
        private final List<String> commands = new ArrayList<>();
        private final List<SoundCue> sounds = new ArrayList<>();
        private final List<ParticleCue> particles = new ArrayList<>();
        private final List<String> bundleRefs = new ArrayList<>();

        private Builder() {}

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder subtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder actionBar(String actionBar) {
            this.actionBar = actionBar;
            return this;
        }

        public Builder command(String command) {
            if (command != null && !command.isEmpty()) {
                this.commands.add(command);
            }
            return this;
        }

        public Builder addSound(String sound, float volume, float pitch) {
            if (sound != null) {
                this.sounds.add(new SoundCue(sound, volume, pitch));
            }
            return this;
        }

        public Builder addParticle(Particle particle, int count,
                                   double offsetX, double offsetY, double offsetZ) {
            if (particle != null) {
                this.particles.add(new ParticleCue(particle, count, offsetX, offsetY, offsetZ));
            }
            return this;
        }

        public Builder referenceBundle(String name) {
            if (name != null && !name.isEmpty()) {
                this.bundleRefs.add(name);
            }
            return this;
        }

        public Builder merge(EffectSettings other) {
            if (other != null && !other.isEmpty()) {
                this.commands.addAll(other.commands);
                this.sounds.addAll(other.sounds);
                this.particles.addAll(other.particles);
                this.bundleRefs.addAll(other.bundles);
            }
            return this;
        }

        public EffectSettings build() {
            return new EffectSettings(title, subtitle, actionBar,
                    Collections.unmodifiableList(new ArrayList<>(commands)),
                    Collections.unmodifiableList(new ArrayList<>(sounds)),
                    Collections.unmodifiableList(new ArrayList<>(particles)),
                    Collections.unmodifiableList(new ArrayList<>(bundleRefs)));
        }
    }

    public record SoundCue(String sound, float volume, float pitch) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("sound", sound);
            map.put("volume", volume);
            map.put("pitch", pitch);
            return map;
        }

        public static SoundCue fromMap(Map<?, ?> map) {
            if (map == null) {
                return null;
            }
            Object sound = map.get("sound");
            if (!(sound instanceof String name) || name.isEmpty()) {
                return null;
            }
            float volume = toFloat(map.get("volume"), 1f);
            float pitch = toFloat(map.get("pitch"), 1f);
            return new SoundCue(name, volume, pitch);
        }

        private static float toFloat(Object value, float fallback) {
            if (value instanceof Number number) {
                return number.floatValue();
            }
            if (value instanceof String str) {
                try {
                    return Float.parseFloat(str);
                } catch (NumberFormatException ignored) {}
            }
            return fallback;
        }
    }

    public record ParticleCue(Particle particle, int count,
                              double offsetX, double offsetY, double offsetZ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("type", particle.name());
            map.put("count", count);
            map.put("offset", List.of(offsetX, offsetY, offsetZ));
            return map;
        }

        public static ParticleCue fromMap(Map<?, ?> map) {
            if (map == null) {
                return null;
            }
            Object type = map.get("type");
            if (!(type instanceof String name)) {
                return null;
            }
            Particle particle;
            try {
                particle = Particle.valueOf(name.toUpperCase());
            } catch (IllegalArgumentException ex) {
                return null;
            }
            int count = toInt(map.get("count"), 1);
            double offsetX = 0, offsetY = 0, offsetZ = 0;
            Object offset = map.get("offset");
            if (offset instanceof List<?> list && list.size() >= 3) {
                offsetX = toDouble(list.get(0), 0);
                offsetY = toDouble(list.get(1), 0);
                offsetZ = toDouble(list.get(2), 0);
            }
            return new ParticleCue(particle, count, offsetX, offsetY, offsetZ);
        }

        private static int toInt(Object value, int fallback) {
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String str) {
                try {
                    return Integer.parseInt(str);
                } catch (NumberFormatException ignored) {}
            }
            return fallback;
        }

        private static double toDouble(Object value, double fallback) {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value instanceof String str) {
                try {
                    return Double.parseDouble(str);
                } catch (NumberFormatException ignored) {}
            }
            return fallback;
        }
    }
}
