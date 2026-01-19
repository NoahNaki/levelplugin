package me.nakilex.levelplugin.particles.particles.parents;

import me.nakilex.levelplugin.particles.util.LiquidParticleState;

public interface LiquidParticle extends IParticle {
    LiquidParticle inherit(Particle particle);

    LiquidParticle clone();

    /**
     * @param state The type of liquid particle this object represents, keep in mind some LiquidParticles don't support all states.
     * @return this object
     */
    LiquidParticle setLiquidState(LiquidParticleState state);

    /**
     * @return The type of liquid particle this object represents, keep in mind some LiquidParticles don't support all states.
     */
    LiquidParticleState getLiquidState();
}
