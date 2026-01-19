package me.nakilex.levelplugin.particles.particles;

import me.nakilex.levelplugin.particles.particles.parents.Particle;
import org.bukkit.Location;

public class ParticleShriek extends Particle {
    private int delay;

    public ParticleShriek(double offsetX, double offsetY, double offsetZ, int count) {
        super("", offsetX, offsetY, offsetZ, count);

        setParticleKey("shriek");
        setDelay(0);
    }

    public ParticleShriek(double offsetX, double offsetY, double offsetZ) {
        this(offsetX, offsetY, offsetZ, 1);
    }

    public ParticleShriek(int count) {
        this(0, 0, 0, count);
    }

    public ParticleShriek() {
        this(0, 0, 0, 1);
    }

    @Override
    public ParticleShriek inherit(Particle particle) {
        super.inherit(particle);

        if (particle instanceof ParticleShriek) {
            this.delay = ((ParticleShriek) particle).delay;
        }

        return this;
    }

    @Override
    public ParticleShriek clone() {
        return new ParticleShriek().inherit(this);
    }

    /**
     * @param delay amount of ticks before this particle displays
     */
    public ParticleShriek setDelay(int delay) {
        this.delay = Math.max(0, delay);

        return this;
    }

    /**
     * @return amount of ticks before this particle displays
     */
    public int getDelay() {
        return delay;
    }

    @Override
    protected Object getData(Location location) {
        return delay;
    }
}
