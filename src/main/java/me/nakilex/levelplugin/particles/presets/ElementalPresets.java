package me.nakilex.levelplugin.particles.presets;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.nakilex.levelplugin.particles.ParticleCenter;
import me.nakilex.levelplugin.particles.ParticlePlane;
import me.nakilex.levelplugin.particles.ParticlePreset;
import me.nakilex.levelplugin.particles.ParticlePresetSettings;
import me.nakilex.levelplugin.particles.ParticleRotationAxis;
import me.nakilex.levelplugin.particles.patterns.HelixPattern;
import me.nakilex.levelplugin.particles.patterns.LemniscatePattern;
import me.nakilex.levelplugin.particles.patterns.LissajousPattern;
import me.nakilex.levelplugin.particles.patterns.RosePattern;
import me.nakilex.levelplugin.particles.patterns.SpiralPattern;
import me.nakilex.levelplugin.particles.patterns.StarPattern;
import me.nakilex.levelplugin.particles.patterns.TrochoidPattern;
import org.bukkit.Particle;

public final class ElementalPresets {
    private ElementalPresets() {}

    public static final ParticlePreset EMBER = new ParticlePreset(
            "EMBER",
            List.of(
                    new RosePattern(Particle.FLAME, null, 1.4, 5.0, 6.0, ParticlePlane.Y, 0, null),
                    new HelixPattern(Particle.SMOKE_NORMAL, null, 0.8, 1.4, 2.0, -4.0, ParticlePlane.Y, 0,
                            null)
            ),
            new ParticlePresetSettings(36, 24, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset SOUL_MIST = new ParticlePreset(
            "SOUL_MIST",
            List.of(
                    new LissajousPattern(Particle.SOUL, null, 1.2, 1.0, 3.0, 2.0, 90.0, 3.5,
                            ParticlePlane.Y, 0, null),
                    new SpiralPattern(Particle.PORTAL, null, 1.2, 1.6, 5.0, 1.4, false,
                            ParticlePlane.Y, 0, null)
            ),
            new ParticlePresetSettings(28, 30, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset FROST_SHARD = new ParticlePreset(
            "FROST_SHARD",
            List.of(
                    new LemniscatePattern(Particle.END_ROD, null, 1.2, 2.0, ParticlePlane.Y, 15.0,
                            ParticleRotationAxis.X),
                    new TrochoidPattern(Particle.SNOWFLAKE, null, 1.6, 0.6, 0.9, -3.0, ParticlePlane.Y,
                            0, null)
            ),
            new ParticlePresetSettings(30, 20, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset ARCANE_SPARK = new ParticlePreset(
            "ARCANE_SPARK",
            List.of(
                    new TrochoidPattern(Particle.ENCHANT, null, 1.8, 0.7, 1.1, 4.0, ParticlePlane.Y, 0,
                            null),
                    new StarPattern(Particle.END_ROD, null, 1.0, 0.45, -6.0, ParticlePlane.Z, 0, null)
            ),
            new ParticlePresetSettings(32, 22, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset VOID_GLIMMER = new ParticlePreset(
            "VOID_GLIMMER",
            List.of(
                    new SpiralPattern(Particle.PORTAL, null, 1.6, 0.8, 2.5, 1.8, true,
                            ParticlePlane.Y, 0, null),
                    new RosePattern(Particle.DRAGON_BREATH, null, 0.9, 7.0, -4.0, ParticlePlane.Z, 10.0,
                            ParticleRotationAxis.Y)
            ),
            new ParticlePresetSettings(34, 24, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset HEALING_AURA = new ParticlePreset(
            "HEALING_AURA",
            List.of(
                    new HelixPattern(Particle.HAPPY_VILLAGER, null, 1.0, 1.6, 1.6, 2.0, ParticlePlane.Y, 0,
                            null),
                    new LissajousPattern(Particle.HEART, null, 0.9, 0.6, 2.0, 3.0, 45.0, -3.0,
                            ParticlePlane.X, 0, null)
            ),
            new ParticlePresetSettings(26, 26, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset CELESTIAL = new ParticlePreset(
            "CELESTIAL",
            List.of(
                    new LissajousPattern(Particle.FIREWORK, null, 1.1, 1.1, 5.0, 4.0, 20.0, 2.5,
                            ParticlePlane.Y, 0, null),
                    new RosePattern(Particle.END_ROD, null, 1.3, 4.0, -3.5, ParticlePlane.Y, 0, null)
            ),
            new ParticlePresetSettings(30, 20, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset RADIANT_SIGIL = new ParticlePreset(
            "RADIANT_SIGIL",
            List.of(
                    new StarPattern(Particle.CRIT, null, 1.3, 0.55, 5.0, ParticlePlane.Y, 0, null),
                    new LemniscatePattern(Particle.ENCHANT, null, 1.0, -2.5, ParticlePlane.Z, 0, null)
            ),
            new ParticlePresetSettings(28, 20, ParticleCenter.SELF, 6.0)
    );

    private static final Map<String, ParticlePreset> PRESETS = Map.ofEntries(
            Map.entry(EMBER.name(), EMBER),
            Map.entry(SOUL_MIST.name(), SOUL_MIST),
            Map.entry(FROST_SHARD.name(), FROST_SHARD),
            Map.entry(ARCANE_SPARK.name(), ARCANE_SPARK),
            Map.entry(VOID_GLIMMER.name(), VOID_GLIMMER),
            Map.entry(HEALING_AURA.name(), HEALING_AURA),
            Map.entry(CELESTIAL.name(), CELESTIAL),
            Map.entry(RADIANT_SIGIL.name(), RADIANT_SIGIL)
    );

    public static ParticlePreset getPreset(String name) {
        if (name == null) {
            return null;
        }
        return PRESETS.get(name.toUpperCase(Locale.ROOT));
    }

    public static List<String> getPresetNames() {
        return PRESETS.keySet().stream().sorted().toList();
    }
}
