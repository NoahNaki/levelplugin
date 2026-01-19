package hm.zelha.particlesfx.particles;

import hm.zelha.particlesfx.particles.parents.ColorableParticle;
import hm.zelha.particlesfx.particles.parents.Particle;
import hm.zelha.particlesfx.util.Color;
import org.bukkit.Location;

import javax.annotation.Nullable;

public class ParticleTintedLeaves extends Particle implements ColorableParticle {

    protected Color color;

    public ParticleTintedLeaves(@Nullable Color color, double offsetX, double offsetY, double offsetZ, int count) {
        super("", offsetX, offsetY, offsetZ, count);

        setParticleKey("tinted_leaves");
        setColor(color);
    }

    public ParticleTintedLeaves(@Nullable Color color, double offsetX, double offsetY, double offsetZ) {
        this(color, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleTintedLeaves(@Nullable Color color, int count) {
        this(color, 0, 0, 0, count);
    }

    public ParticleTintedLeaves(double offsetX, double offsetY, double offsetZ, int count) {
        this(null, offsetX, offsetY, offsetZ, count);
    }

    public ParticleTintedLeaves(double offsetX, double offsetY, double offsetZ) {
        this(null, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleTintedLeaves(@Nullable Color color) {
        this(color, 0, 0, 0, 1);
    }

    public ParticleTintedLeaves(int count) {
        this(null, 0, 0, 0, count);
    }

    public ParticleTintedLeaves() {
        this(null, 0, 0, 0, 1);
    }

    @Override
    public ParticleTintedLeaves inherit(Particle particle) {
        super.inherit(particle);

        if (particle instanceof ColorableParticle) {
            color = ((ColorableParticle) particle).getColor();
        }

        return this;
    }

    @Override
    public ParticleTintedLeaves clone() {
        return new ParticleTintedLeaves().inherit(this);
    }

    @Override
    protected void updateData(Location location) {
        if (color == null) {
            data = org.bukkit.Color.fromRGB(rng.nextInt(0xffffff));
            return;
        }
        data = color.toBukkitColor();
    }

    public void setColor(@Nullable Color color) {
        this.color = color;
    }

    public void setColor(int red, int green, int blue) {
        if (color != null && !color.isLocked()) {
            color.setRed(red);
            color.setGreen(green);
            color.setBlue(blue);
        } else {
            this.color = new Color(red, green, blue);
        }
    }

    @Nullable
    public Color getColor() {
        return color;
    }
}
