package me.nakilex.levelplugin.effectdemo;

import de.slikey.effectlib.effect.AtomEffect;
import de.slikey.effectlib.Effect;
import de.slikey.effectlib.effect.*;
import me.nakilex.levelplugin.Main;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * Collection of very simple particle effects used by the /fxdemo GUI.
 */
public enum DemoEffects {
    HELIX {
        @Override
        public void play(Player player) {
            HelixEffect effect = new HelixEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.iterations = 40;
            effect.radius = 1.2f;
            effect.particle = Particle.FLAME;
            start(effect);
        }
    },
    SPHERE {
        @Override
        public void play(Player player) {
            SphereEffect effect = new SphereEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation().add(0,1,0));
            effect.particle = Particle.CRIT;
            effect.radius = 1.5f;
            effect.iterations = 10;
            start(effect);
        }
    },
    TORNADO {
        @Override
        public void play(Player player) {
            TornadoEffect effect = new TornadoEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.tornadoParticle = Particle.CLOUD;
            effect.circleParticles = 8;
            effect.iterations = 40;
            start(effect);
        }
    },
    ATOM {
        @Override
        public void play(Player player) {
            AtomEffect effect = new AtomEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particleOrbital = Particle.END_ROD;
            effect.particleNucleus = Particle.FLASH;
            effect.radius = 1.0f;
            effect.iterations = 40;
            start(effect);
        }
    },
    CONE {
        @Override
        public void play(Player player) {
            ConeEffect effect = new ConeEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.angularVelocity = Math.PI / 8;
            effect.lengthGrow = 0.1f;
            effect.particle = Particle.FLAME;
            effect.iterations = 40;
            start(effect);
        }
    },
    CYLINDER {
        @Override
        public void play(Player player) {
            CylinderEffect effect = new CylinderEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.radius = 1.5f;
            effect.height = 3f;
            effect.particle = Particle.SPELL_WITCH;
            effect.iterations = 40;
            start(effect);
        }
    },
    DNA {
        @Override
        public void play(Player player) {
            DnaEffect effect = new DnaEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particleHelix = Particle.VILLAGER_HAPPY;
            effect.iterations = 40;
            start(effect);
        }
    },
    DONUT {
        @Override
        public void play(Player player) {
            DonutEffect effect = new DonutEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particle = Particle.FIREWORKS_SPARK;
            effect.iterations = 40;
            start(effect);
        }
    },
    FOUNTAIN {
        @Override
        public void play(Player player) {
            FountainEffect effect = new FountainEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particle = Particle.WATER_SPLASH;
            effect.iterations = 40;
            start(effect);
        }
    },
    HEART {
        @Override
        public void play(Player player) {
            HeartEffect effect = new HeartEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation().add(0,1,0));
            effect.iterations = 40;
            start(effect);
        }
    },
    BIG_BANG {
        @Override
        public void play(Player player) {
            BigBangEffect effect = new BigBangEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particle = Particle.EXPLOSION_NORMAL;
            effect.iterations = 1;
            start(effect);
        }
    },
    VORTEX {
        @Override
        public void play(Player player) {
            VortexEffect effect = new VortexEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particle = Particle.PORTAL;
            effect.iterations = 40;
            start(effect);
        }
    },
    WAVE {
        @Override
        public void play(Player player) {
            WaveEffect effect = new WaveEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particle = Particle.SPLASH;
            effect.period = 2;
            effect.iterations = 40;
            start(effect);
        }
    },
    STAR {
        @Override
        public void play(Player player) {
            StarEffect effect = new StarEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation().add(0,1,0));
            effect.particle = Particle.END_ROD;
            effect.iterations = 40;
            start(effect);
        }
    };

    private static Effect active;

    private static void start(Effect effect) {
        if (active != null && !active.isDone()) {
            active.cancel();
        }
        active = effect;
        effect.start();
    }

    public abstract void play(Player player);

    /**
     * Get effect by slot index for GUI convenience.
     */
    public static DemoEffects bySlot(int slot) {
        return switch (slot) {
            case 10 -> HELIX;
            case 11 -> SPHERE;
            case 12 -> TORNADO;
            case 13 -> ATOM;
            case 14 -> CONE;
            case 15 -> CYLINDER;
            case 16 -> DNA;
            case 19 -> DONUT;
            case 20 -> FOUNTAIN;
            case 21 -> HEART;
            case 22 -> BIG_BANG;
            case 23 -> VORTEX;
            case 24 -> WAVE;
            case 25 -> STAR;
            default -> null;
        };
    }
}
