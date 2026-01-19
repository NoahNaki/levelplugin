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
import me.nakilex.levelplugin.particles.patterns.BulletSpherePattern;
import me.nakilex.levelplugin.particles.patterns.PointPattern;
import me.nakilex.levelplugin.particles.patterns.RosePattern;
import me.nakilex.levelplugin.particles.patterns.SpiralPattern;
import me.nakilex.levelplugin.particles.patterns.StarPattern;
import me.nakilex.levelplugin.particles.patterns.TrochoidPattern;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

public final class ElementalPresets {
    private ElementalPresets() {}

    public static final ParticlePreset EMBER = new ParticlePreset(
            "EMBER",
            List.of(
                    new HelixPattern(Particle.SMOKE_NORMAL, null, 0.6, 2.4, 1.4, -6.0, ParticlePlane.Y, 0,
                            null),
                    new SpiralPattern(Particle.FLAME, null, 1.0, 2.0, 4.0, 1.3, true, ParticlePlane.Y, 0,
                            null)
            ),
            new ParticlePresetSettings(36, 24, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset SOUL_MIST = new ParticlePreset(
            "SOUL_MIST",
            List.of(
                    new RosePattern(Particle.ENCHANT, null, 1.5, 6.0, 2.5, ParticlePlane.Y, 0, null),
                    new LissajousPattern(Particle.END_ROD, null, 1.1, 1.1, 4.0, 3.0, 30.0, 2.0,
                            ParticlePlane.Y, 0, null)
            ),
            new ParticlePresetSettings(28, 30, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset FROST_SHARD = new ParticlePreset(
            "FROST_SHARD",
            List.of(
                    new StarPattern(Particle.SNOWFLAKE, null, 1.3, 0.45, 3.0, ParticlePlane.X, 20.0,
                            ParticleRotationAxis.Z),
                    new TrochoidPattern(Particle.END_ROD, null, 1.4, 0.5, 0.9, -4.0, ParticlePlane.Y, 0,
                            null)
            ),
            new ParticlePresetSettings(30, 20, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset ARCANE_SPARK = new ParticlePreset(
            "ARCANE_SPARK",
            List.of(
                    new HelixPattern(Particle.END_ROD, null, 0.35, 2.4, 1.1, 0.0, ParticlePlane.LOOK, 0,
                            null),
                    new SpiralPattern(Particle.CRIT, null, 0.25, 2.0, 2.5, 0.6, false, ParticlePlane.LOOK, 0,
                            null)
            ),
            new ParticlePresetSettings(32, 22, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset VOID_GLIMMER = new ParticlePreset(
            "VOID_GLIMMER",
            List.of(
                    new SpiralPattern(Particle.PORTAL, null, 1.6, 1.0, 2.5, 1.8, true,
                            ParticlePlane.Y, 0, null),
                    new LemniscatePattern(Particle.DRAGON_BREATH, null, 1.0, -3.0, ParticlePlane.Z, 10.0,
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
                    new LissajousPattern(Particle.FIREWORK, null, 1.2, 1.0, 5.0, 4.0, 10.0, 2.0,
                            ParticlePlane.Y, 0, null),
                    new RosePattern(Particle.END_ROD, null, 1.4, 5.0, -2.5, ParticlePlane.Y, 0, null),
                    new PointPattern(Particle.END_ROD, null, new Vector(0, 0.1, 0))
            ),
            new ParticlePresetSettings(30, 20, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset RADIANT_SIGIL = new ParticlePreset(
            "RADIANT_SIGIL",
            List.of(
                    new StarPattern(Particle.ENCHANT, null, 1.4, 0.55, 4.0, ParticlePlane.Y, 0, null),
                    new RosePattern(Particle.CRIT, null, 1.1, 8.0, -3.0, ParticlePlane.Y, 0, null)
            ),
            new ParticlePresetSettings(28, 20, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset METEOR_FALL = new ParticlePreset(
            "METEOR_FALL",
            List.of(
                    new HelixPattern(Particle.SMOKE_NORMAL, null, 0.5, 2.8, 1.2, -5.0, ParticlePlane.Y, 0,
                            null),
                    new SpiralPattern(Particle.LAVA, null, 0.9, 2.2, 5.0, 1.1, true, ParticlePlane.Y, 0,
                            null)
            ),
            new ParticlePresetSettings(32, 18, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset PIERCING_LANCE = new ParticlePreset(
            "PIERCING_LANCE",
            List.of(
                    new SpiralPattern(Particle.END_ROD, null, 0.2, 2.6, 2.0, 0.6, false, ParticlePlane.LOOK, 0,
                            null),
                    new PointPattern(Particle.CRIT, null, new Vector(0, 0, 2.4))
            ),
            new ParticlePresetSettings(22, 16, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset ARCANE_CIRCLE = new ParticlePreset(
            "ARCANE_CIRCLE",
            List.of(
                    new RosePattern(Particle.ENCHANT, null, 1.6, 7.0, 3.0, ParticlePlane.Y, 0, null),
                    new LissajousPattern(Particle.END_ROD, null, 1.2, 1.2, 4.0, 5.0, 15.0, -2.0,
                            ParticlePlane.Y, 0, null)
            ),
            new ParticlePresetSettings(34, 24, ParticleCenter.SELF, 6.0)
    );

    public static final ParticlePreset BULLETSPHERE = new ParticlePreset(
            "BULLETSPHERE",
            List.of(
                    new BulletSpherePattern(Particle.END_ROD, null, Particle.CRIT, null,
                            5.0, 0.45, 160, 6)
            ),
            new ParticlePresetSettings(20, 60, ParticleCenter.LOOK, 8.0)
    );

    private static final Map<String, ParticlePreset> PRESETS = Map.ofEntries(
            Map.entry(EMBER.name(), EMBER),
            Map.entry(SOUL_MIST.name(), SOUL_MIST),
            Map.entry(FROST_SHARD.name(), FROST_SHARD),
            Map.entry(ARCANE_SPARK.name(), ARCANE_SPARK),
            Map.entry(VOID_GLIMMER.name(), VOID_GLIMMER),
            Map.entry(HEALING_AURA.name(), HEALING_AURA),
            Map.entry(CELESTIAL.name(), CELESTIAL),
            Map.entry(RADIANT_SIGIL.name(), RADIANT_SIGIL),
            Map.entry(METEOR_FALL.name(), METEOR_FALL),
            Map.entry(PIERCING_LANCE.name(), PIERCING_LANCE),
            Map.entry(ARCANE_CIRCLE.name(), ARCANE_CIRCLE),
            Map.entry(BULLETSPHERE.name(), BULLETSPHERE)
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
