package hm.zelha.particlesfx.particles;

import hm.zelha.particlesfx.particles.parents.ColorableParticle;
import hm.zelha.particlesfx.particles.parents.Particle;
import hm.zelha.particlesfx.particles.parents.SizeableParticle;
import hm.zelha.particlesfx.util.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import javax.annotation.Nullable;

public class ParticleDustMulticolored extends ParticleDustColored implements SizeableParticle, ColorableParticle {

    protected final Color colorHelper2 = new Color(rng.nextInt(0xffffff));
    private Color transition = null;

    public ParticleDustMulticolored(@Nullable Color color, double size, double offsetX, double offsetY, double offsetZ, int count) {
        super(color, size, offsetX, offsetY, offsetZ, count);

        setParticleKey("dust_color_transition");
    }

    public ParticleDustMulticolored(double size, double offsetX, double offsetY, double offsetZ, int count) {
        this(null, size, offsetX, offsetY, offsetZ, count);
    }

    public ParticleDustMulticolored(@Nullable Color color, double offsetX, double offsetY, double offsetZ, int count) {
        this(color, 1, offsetX, offsetY, offsetZ, count);
    }

    public ParticleDustMulticolored(double offsetX, double offsetY, double offsetZ, int count) {
        this(null, 1, offsetX, offsetY, offsetZ, count);
    }

    public ParticleDustMulticolored(@Nullable Color color, double offsetX, double offsetY, double offsetZ) {
        this(color, 1, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleDustMulticolored(double size, double offsetX, double offsetY, double offsetZ) {
        this(null, size, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleDustMulticolored(double offsetX, double offsetY, double offsetZ) {
        this(null, 1, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleDustMulticolored(@Nullable Color color, double size, int count) {
        this(color, size, 0, 0, 0, count);
    }

    public ParticleDustMulticolored(@Nullable Color color, double size) {
        this(color, size, 0, 0, 0, 1);
    }

    public ParticleDustMulticolored(@Nullable Color color) {
        this(color, 1, 0, 0, 0, 1);
    }

    public ParticleDustMulticolored(int count) {
        this(null, 1, 0, 0, 0, count);
    }

    public ParticleDustMulticolored(double size) {
        this(null, size, 0, 0, 0, 1);
    }

    public ParticleDustMulticolored() {
        this(null, 1, 0, 0, 0, 1);
    }

    @Override
    public ParticleDustMulticolored inherit(Particle particle) {
        super.inherit(particle);

        if (particle instanceof ParticleDustMulticolored) {
            this.transition = ((ParticleDustMulticolored) particle).transition;
        }

        return this;
    }

    @Override
    public ParticleDustMulticolored clone() {
        return new ParticleDustMulticolored().inherit(this);
    }

    @Override
    protected void updateData(Location location) {
        org.bukkit.Color from = (color == null) ? org.bukkit.Color.fromRGB(rng.nextInt(0xffffff)) : color.toBukkitColor();
        org.bukkit.Color to = (transition == null) ? org.bukkit.Color.fromRGB(rng.nextInt(0xffffff)) : transition.toBukkitColor();

        if (color != null && !colorHelper.equals(color)) {
            colorHelper.setRGB(color.getRGB());
        }
        if (transition != null && !colorHelper2.equals(transition)) {
            colorHelper2.setRGB(transition.getRGB());
        }

        data = new Particle.DustTransition(from, to, clampSize(size));
    }

    /**
     * @param transition the color this particle will transition to as it fades.
     */
    public ParticleDustMulticolored setTransitionColor(@Nullable Color transition) {
        this.transition = transition;

        return this;
    }

    /**
     * @return the color this particle will transition to as it fades.
     */
    public Color getTransitionColor() {
        return transition;
    }
}
