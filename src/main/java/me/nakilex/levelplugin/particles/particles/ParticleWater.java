package me.nakilex.levelplugin.particles.particles;

import me.nakilex.levelplugin.particles.particles.parents.LiquidParticle;
import me.nakilex.levelplugin.particles.particles.parents.Particle;
import me.nakilex.levelplugin.particles.util.LiquidParticleState;

import java.util.Locale;

public class ParticleWater extends Particle implements LiquidParticle {

    private LiquidParticleState state = LiquidParticleState.DRIPPING;

    public ParticleWater(double offsetX, double offsetY, double offsetZ, int count) {
        super("dripping_water", offsetX, offsetY, offsetZ, count);
    }

    public ParticleWater(double offsetX, double offsetY, double offsetZ) {
        this(offsetX, offsetY, offsetZ, 1);
    }

    public ParticleWater(int count) {
        this(0, 0, 0, count);
    }

    public ParticleWater() {
        this(0, 0, 0, 1);
    }

    @Override
    public ParticleWater inherit(Particle particle) {
        super.inherit(particle);

        if (particle instanceof LiquidParticle) {
            setLiquidState(((LiquidParticle) particle).getLiquidState());
        }

        return this;
    }

    @Override
    public ParticleWater clone() {
        return new ParticleWater().inherit(this);
    }

    @Override
    public ParticleWater setLiquidState(LiquidParticleState state) {
        if (state == LiquidParticleState.LANDING) throw new IllegalArgumentException("The \"LANDING\" state doesn't exist for this particle!");

        setParticleKey(state.name().toLowerCase(Locale.ROOT) + "_water");
        this.state = state;

        return this;
    }

    @Override
    public LiquidParticleState getLiquidState() {
        return state;
    }
}
