package me.nakilex.levelplugin.effectdemo;

import de.slikey.effectlib.effect.AtomEffect;
import de.slikey.effectlib.effect.HelixEffect;
import de.slikey.effectlib.effect.SphereEffect;
import de.slikey.effectlib.effect.TornadoEffect;
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
            effect.start();
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
            effect.start();
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
            effect.start();
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
            effect.start();
        }
    };

    public abstract void play(Player player);

    /**
     * Get effect by slot index for GUI convenience.
     */
    public static DemoEffects bySlot(int slot) {
        return switch (slot) {
            case 10 -> HELIX;
            case 12 -> SPHERE;
            case 14 -> TORNADO;
            case 16 -> ATOM;
            default -> null;
        };
    }
}
