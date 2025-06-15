package me.nakilex.levelplugin.effectdemo;

import de.slikey.effectlib.Effect;
import de.slikey.effectlib.effect.*;
import me.nakilex.levelplugin.Main;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * Collection of simple EffectLib examples used in the demo GUI.
 */
public enum DemoEffects {
    HELIX(Material.BLAZE_ROD, "Helix") {
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
    SPHERE(Material.NETHER_STAR, "Sphere") {
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
    TORNADO(Material.FIRE_CHARGE, "Tornado") {
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
    ATOM(Material.ENDER_EYE, "Atom") {
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
    CONE(Material.SNOWBALL, "Cone") {
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
    CYLINDER(Material.IRON_BARS, "Cylinder") {
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
    DNA(Material.BONE, "DNA") {
        @Override
        public void play(Player player) {
            DnaEffect effect = new DnaEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particleHelix = Particle.VILLAGER_HAPPY;
            effect.iterations = 40;
            start(effect);
        }
    },
    DONUT(Material.CAKE, "Donut") {
        @Override
        public void play(Player player) {
            DonutEffect effect = new DonutEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particle = Particle.FIREWORKS_SPARK;
            effect.iterations = 40;
            start(effect);
        }
    },
    FOUNTAIN(Material.WATER_BUCKET, "Fountain") {
        @Override
        public void play(Player player) {
            FountainEffect effect = new FountainEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particle = Particle.WATER_SPLASH;
            effect.iterations = 40;
            start(effect);
        }
    },
    HEART(Material.APPLE, "Heart") {
        @Override
        public void play(Player player) {
            HeartEffect effect = new HeartEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation().add(0,1,0));
            effect.iterations = 40;
            start(effect);
        }
    },
    BIG_BANG(Material.TNT, "Big Bang") {
        @Override
        public void play(Player player) {
            BigBangEffect effect = new BigBangEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particle = Particle.EXPLOSION_NORMAL;
            effect.iterations = 1;
            start(effect);
        }
    },
    VORTEX(Material.ENDER_PEARL, "Vortex") {
        @Override
        public void play(Player player) {
            VortexEffect effect = new VortexEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particle = Particle.PORTAL;
            effect.iterations = 40;
            start(effect);
        }
    },
    WAVE(Material.PRISMARINE_SHARD, "Wave") {
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
    STAR(Material.AMETHYST_SHARD, "Star") {
        @Override
        public void play(Player player) {
            StarEffect effect = new StarEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation().add(0,1,0));
            effect.particle = Particle.END_ROD;
            effect.iterations = 40;
            start(effect);
        }
    },
    ANIMATED_BALL(Material.SLIME_BALL, "Animated Ball") {
        @Override
        public void play(Player player) {
            AnimatedBallEffect effect = new AnimatedBallEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation().add(0,1,0));
            effect.particle = Particle.SPELL_WITCH;
            effect.size = 1.2f;
            effect.iterations = 40;
            start(effect);
        }
    },
    BLEED(Material.RED_DYE, "Bleed") {
        @Override
        public void play(Player player) {
            BleedEffect effect = new BleedEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.material = Material.REDSTONE_BLOCK;
            effect.height = 1.0;
            effect.iterations = 20;
            start(effect);
        }
    },
    CLOUD(Material.WHITE_WOOL, "Cloud") {
        @Override
        public void play(Player player) {
            CloudEffect effect = new CloudEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation().add(0,1,0));
            effect.cloudParticle = Particle.CLOUD;
            effect.mainParticle = Particle.END_ROD;
            effect.iterations = 40;
            start(effect);
        }
    },
    CUBE(Material.EMERALD_BLOCK, "Cube") {
        @Override
        public void play(Player player) {
            CubeEffect effect = new CubeEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particle = Particle.TOTEM;
            effect.edgeLength = 2f;
            effect.iterations = 20;
            start(effect);
        }
    },
    CUBOID(Material.GOLD_BLOCK, "Cuboid") {
        @Override
        public void play(Player player) {
            CuboidEffect effect = new CuboidEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.xLength = 2;
            effect.yLength = 3;
            effect.zLength = 1.5;
            effect.particles = 50;
            effect.iterations = 20;
            start(effect);
        }
    },
    DRAGON(Material.DRAGON_EGG, "Dragon") {
        @Override
        public void play(Player player) {
            DragonEffect effect = new DragonEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particles = 5;
            effect.length = 3f;
            effect.iterations = 40;
            start(effect);
        }
    },
    EARTH(Material.GRASS_BLOCK, "Earth") {
        @Override
        public void play(Player player) {
            EarthEffect effect = new EarthEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particleLand = Particle.VILLAGER_HAPPY;
            effect.particleOcean = Particle.DRIP_WATER;
            effect.iterations = 40;
            start(effect);
        }
    },
    FLAME(Material.BLAZE_POWDER, "Flame") {
        @Override
        public void play(Player player) {
            FlameEffect effect = new FlameEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particles = 30;
            effect.iterations = 20;
            start(effect);
        }
    },
    HILL(Material.DIRT, "Hill") {
        @Override
        public void play(Player player) {
            HillEffect effect = new HillEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.edgeLength = 2;
            effect.height = 2;
            effect.particles = 50;
            effect.iterations = 20;
            start(effect);
        }
    },
    LINE(Material.STICK, "Line") {
        @Override
        public void play(Player player) {
            LineEffect effect = new LineEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particle = Particle.CRIT_MAGIC;
            effect.length = 3;
            effect.particles = 30;
            effect.iterations = 10;
            start(effect);
        }
    },
    MUSIC(Material.NOTE_BLOCK, "Music") {
        @Override
        public void play(Player player) {
            MusicEffect effect = new MusicEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.radius = 1.5f;
            effect.radialsPerStep = Math.PI / 8;
            effect.iterations = 40;
            start(effect);
        }
    },
    PYRAMID(Material.SANDSTONE, "Pyramid") {
        @Override
        public void play(Player player) {
            PyramidEffect effect = new PyramidEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.radius = 2;
            effect.particles = 50;
            effect.iterations = 20;
            start(effect);
        }
    },
    SKYROCKET(Material.FIREWORK_ROCKET, "Sky Rocket") {
        @Override
        public void play(Player player) {
            SkyRocketEffect effect = new SkyRocketEffect(Main.getInstance().getEffectManager());
            effect.setLocation(player.getLocation());
            effect.particle = Particle.FLAME;
            effect.power = 1.5f;
            effect.iterations = 20;
            start(effect);
        }
    };

    private final Material icon;
    private final String label;

    DemoEffects(Material icon, String label) {
        this.icon = icon;
        this.label = label;
    }

    public Material getIcon() {
        return icon;
    }

    public String getLabel() {
        return label;
    }

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
        DemoEffects[] values = values();
        return (slot >= 0 && slot < values.length) ? values[slot] : null;
    }
}
