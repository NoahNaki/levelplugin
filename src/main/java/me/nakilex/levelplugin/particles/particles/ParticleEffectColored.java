package me.nakilex.levelplugin.particles.particles;

import me.nakilex.levelplugin.particles.particles.parents.ColorableParticle;
import me.nakilex.levelplugin.particles.particles.parents.Particle;
import me.nakilex.levelplugin.particles.util.Color;
import org.bukkit.Location;

import javax.annotation.Nullable;

public class ParticleEffectColored extends Particle implements ColorableParticle {

    protected Color color;
    private int transparency = 255;

    public ParticleEffectColored(@Nullable Color color, int transparency, double offsetX, double offsetY, double offsetZ, int count) {
        super("", offsetX, offsetY, offsetZ, count);

        setParticleKey("entity_effect");
        setTransparency(transparency);
        setColor(color);
    }

    public ParticleEffectColored(int transparency, double offsetX, double offsetY, double offsetZ, int count) {
        this(null, transparency, offsetX, offsetY, offsetZ, count);
    }

    public ParticleEffectColored(@Nullable Color color, double offsetX, double offsetY, double offsetZ) {
        this(color, 255, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleEffectColored(int transparency, double offsetX, double offsetY, double offsetZ) {
        this(null, transparency, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleEffectColored(double offsetX, double offsetY, double offsetZ, int count) {
        this(null, 255, offsetX, offsetY, offsetZ, count);
    }

    public ParticleEffectColored(double offsetX, double offsetY, double offsetZ) {
        this(null, 255, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleEffectColored(@Nullable Color color) {
        this(color, 255, 0, 0, 0, 1);
    }

    public ParticleEffectColored(int count) {
        this(null, 255, 0, 0, 0, count);
    }

    public ParticleEffectColored() {
        this(null, 255, 0, 0, 0, 1);
    }

    @Override
    public ParticleEffectColored inherit(Particle particle) {
        super.inherit(particle);

        if (particle instanceof ColorableParticle) {
            color = ((ColorableParticle) particle).getColor();
        }

        if (particle instanceof ParticleEffectColored) {
            transparency = ((ParticleEffectColored) particle).transparency;
        }

        return this;
    }

    @Override
    public ParticleEffectColored clone() {
        return new ParticleEffectColored().inherit(this);
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

    /**
     * @param transparency the transparency of the particle, from 0 to 255
     * @return this object
     */
    public ParticleEffectColored setTransparency(int transparency) {
        this.transparency = Math.min(Math.max(0, transparency), 255);

        return this;
    }

    @Nullable
    public Color getColor() {
        return color;
    }

    /**
     * @return the transparency of the particle, from 0 to 255
     */
    public int getTransparency() {
        return transparency;
    }
}
