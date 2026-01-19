package me.nakilex.levelplugin.particles.particles;

import me.nakilex.levelplugin.particles.particles.parents.LiquidParticle;
import me.nakilex.levelplugin.particles.particles.parents.Particle;
import me.nakilex.levelplugin.particles.util.LiquidParticleState;

import java.util.Locale;

public class ParticleLava extends Particle implements LiquidParticle {

    private LiquidParticleState state = LiquidParticleState.DRIPPING;

    public ParticleLava(double offsetX, double offsetY, double offsetZ, int count) {
        super("dripping_lava", offsetX, offsetY, offsetZ, count);
    }

    public ParticleLava(double offsetX, double offsetY, double offsetZ) {
        this(offsetX, offsetY, offsetZ, 1);
    }

    public ParticleLava(int count) {
        this(0, 0, 0, count);
    }

    public ParticleLava() {
        this(0, 0, 0, 1);
    }

    @Override
    public ParticleLava inherit(Particle particle) {
        super.inherit(particle);

        if (particle instanceof LiquidParticle) {
            setLiquidState(((LiquidParticle) particle).getLiquidState());
        }

        return this;
    }

    @Override
    public ParticleLava clone() {
        return new ParticleLava().inherit(this);
    }

    @Override
    public ParticleLava setLiquidState(LiquidParticleState state) {
        setParticleKey(state.name().toLowerCase(Locale.ROOT) + "_lava");
        this.state = state;

        return this;
    }

    @Override
    public LiquidParticleState getLiquidState() {
        return state;
    }
}
