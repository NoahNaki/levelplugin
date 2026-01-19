package me.nakilex.levelplugin.particles.particles;

import me.nakilex.levelplugin.particles.particles.parents.ColorableParticle;
import me.nakilex.levelplugin.particles.particles.parents.Particle;
import me.nakilex.levelplugin.particles.particles.parents.SizeableParticle;
import me.nakilex.levelplugin.particles.util.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

import javax.annotation.Nullable;

public class ParticleDustColored extends Particle implements SizeableParticle, ColorableParticle {

    protected final Color colorHelper = new Color(rng.nextInt(0xffffff));
    protected Color color;
    protected double size;

    public ParticleDustColored(@Nullable Color color, double size, double offsetX, double offsetY, double offsetZ, int count) {
        super("", offsetX, offsetY, offsetZ, count);

        setParticleKey("dust");
        if (color != null) {
            colorHelper.setRGB(color.getRGB());
        }
        setColor(color);
        setSize(size);
    }

    public ParticleDustColored(double size, double offsetX, double offsetY, double offsetZ, int count) {
        this(null, size, offsetX, offsetY, offsetZ, count);
    }

    public ParticleDustColored(@Nullable Color color, double offsetX, double offsetY, double offsetZ, int count) {
        this(color, 1, offsetX, offsetY, offsetZ, count);
    }

    public ParticleDustColored(double offsetX, double offsetY, double offsetZ, int count) {
        this(null, 1, offsetX, offsetY, offsetZ, count);
    }

    public ParticleDustColored(@Nullable Color color, double offsetX, double offsetY, double offsetZ) {
        this(color, 1, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleDustColored(double size, double offsetX, double offsetY, double offsetZ) {
        this(null, size, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleDustColored(double offsetX, double offsetY, double offsetZ) {
        this(null, 1, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleDustColored(@Nullable Color color, double size, int count) {
        this(color, size, 0, 0, 0, count);
    }

    public ParticleDustColored(@Nullable Color color, double size) {
        this(color, size, 0, 0, 0, 1);
    }

    public ParticleDustColored(@Nullable Color color) {
        this(color, 1, 0, 0, 0, 1);
    }

    public ParticleDustColored(int count) {
        this(null, 1, 0, 0, 0, count);
    }

    public ParticleDustColored(double size) {
        this(null, size, 0, 0, 0, 1);
    }

    public ParticleDustColored() {
        this(null, 1, 0, 0, 0, 1);
    }

    @Override
    public ParticleDustColored inherit(Particle particle) {
        super.inherit(particle);

        if (particle instanceof ColorableParticle) {
            setColor(((ColorableParticle) particle).getColor());
        }

        if (particle instanceof SizeableParticle) {
            setSize(((SizeableParticle) particle).getSize());
        }

        return this;
    }

    @Override
    public ParticleDustColored clone() {
        return new ParticleDustColored().inherit(this);
    }

    @Override
    protected void updateData(Location location) {
        org.bukkit.Color bukkitColor = (color == null) ? org.bukkit.Color.fromRGB(rng.nextInt(0xffffff)) : color.toBukkitColor();
        if (color != null && !colorHelper.equals(color)) {
            colorHelper.setRGB(color.getRGB());
        }
        data = new Particle.DustOptions(bukkitColor, clampSize(size));
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

    /** only changes between 0 and 4. */
    @Override
    public void setSize(double size) {
        this.size = size;
    }

    @Nullable
    public Color getColor() {
        return color;
    }

    /** only changes between 0 and 4. */
    @Override
    public double getSize() {
        return size;
    }

    protected float clampSize(double size) {
        return (float) Math.max(0.01, Math.min(4.0, size));
    }
}
