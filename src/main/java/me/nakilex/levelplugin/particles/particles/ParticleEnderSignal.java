package hm.zelha.particlesfx.particles;

import hm.zelha.particlesfx.particles.parents.Particle;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * while this effect is really cool, it summons around 30 particles per effect.
 * <br><br>
 * since Effect.ENDER_SIGNAL is Type.VISUAL, the radius, speed, and offsets are unused internally, and the default radius is quite small.
 * <br><br>
 * Type.VISUAL effects are also locked to specific coordinates of the block they're played on because they use block-based locations.
 */
public class ParticleEnderSignal extends Particle {

    /**@see ParticleEnderSignal*/
    public ParticleEnderSignal(int count) {
        super("", 0, 0, 0, count);
        setParticleKey("effect");
    }

    /**@see ParticleEnderSignal*/
    public ParticleEnderSignal() {
        this(1);
    }

    @Override
    public ParticleEnderSignal inherit(Particle particle) {
        super.inherit(particle);

        return this;
    }

    @Override
    public ParticleEnderSignal clone() {
        return new ParticleEnderSignal().inherit(this);
    }

    @Override
    protected boolean displaySpecial(Player player, Location location) {
        player.playEffect(location, Effect.ENDER_SIGNAL, 0);
        return true;
    }
}
