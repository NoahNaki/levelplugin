package me.nakilex.levelplugin.particles.particles.parents;

public interface SizeableParticle extends IParticle {
    SizeableParticle inherit(Particle particle);

    SizeableParticle clone();

    void setSize(double size);

    double getSize();
}