package me.nakilex.levelplugin.particles.particles;

import me.nakilex.levelplugin.particles.particles.parents.LiquidParticle;
import me.nakilex.levelplugin.particles.particles.parents.Particle;
import me.nakilex.levelplugin.particles.util.LiquidParticleState;

import java.util.Locale;

public class ParticleHoney extends Particle implements LiquidParticle {

    private LiquidParticleState state = LiquidParticleState.DRIPPING;

    public ParticleHoney(double offsetX, double offsetY, double offsetZ, int count) {
        super("dripping_honey", offsetX, offsetY, offsetZ, count);
    }

    public ParticleHoney(double offsetX, double offsetY, double offsetZ) {
        this(offsetX, offsetY, offsetZ, 1);
    }

    public ParticleHoney(int count) {
        this(0, 0, 0, count);
    }

    public ParticleHoney() {
        this(0, 0, 0, 1);
    }

    @Override
    public ParticleHoney inherit(Particle particle) {
        super.inherit(particle);

        if (particle instanceof LiquidParticle) {
            setLiquidState(((LiquidParticle) particle).getLiquidState());
        }

        return this;
    }

    @Override
    public ParticleHoney clone() {
        return new ParticleHoney().inherit(this);
    }

    @Override
    public ParticleHoney setLiquidState(LiquidParticleState state) {
        setParticleKey(state.name().toLowerCase(Locale.ROOT) + "_honey");
        this.state = state;

        return this;
    }

    @Override
    public LiquidParticleState getLiquidState() {
        return state;
    }
}
