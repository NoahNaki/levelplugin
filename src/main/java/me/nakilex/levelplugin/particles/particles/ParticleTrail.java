package me.nakilex.levelplugin.particles.particles;

import me.nakilex.levelplugin.particles.particles.parents.ColorableParticle;
import me.nakilex.levelplugin.particles.particles.parents.Particle;
import me.nakilex.levelplugin.particles.particles.parents.TravellingParticle;
import me.nakilex.levelplugin.particles.util.Color;
import me.nakilex.levelplugin.particles.util.LVMath;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;

public class ParticleTrail extends TravellingParticle implements ColorableParticle {

    protected Color color;
    private int arrivalTime = 20;

    public ParticleTrail(Color color, Location toGo, double offsetX, double offsetY, double offsetZ, int count) {
        super("", false, 1, null, toGo, offsetX, offsetY, offsetZ, count);

        setParticleKey("trail");
        setColor(color);
    }

    public ParticleTrail(Color color, Vector velocity, double offsetX, double offsetY, double offsetZ, int count) {
        super("", false, 1, velocity, null, offsetX, offsetY, offsetZ, count);

        setParticleKey("trail");
        setColor(color);
    }

    public ParticleTrail(Location toGo, double offsetX, double offsetY, double offsetZ, int count) {
        this(null, toGo, offsetX, offsetY, offsetZ, count);
    }

    public ParticleTrail(Vector velocity, double offsetX, double offsetY, double offsetZ, int count) {
        this(null, velocity, offsetX, offsetY, offsetZ, count);
    }

    public ParticleTrail(Color color, double offsetX, double offsetY, double offsetZ, int count) {
        this(color, (Location) null, offsetX, offsetY, offsetZ, count);
    }

    public ParticleTrail(Color color, Location toGo, double offsetX, double offsetY, double offsetZ) {
        this(color, toGo, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleTrail(Color color, Vector velocity, double offsetX, double offsetY, double offsetZ) {
        this(color, velocity, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleTrail(Location toGo, double offsetX, double offsetY, double offsetZ) {
        this(null, toGo, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleTrail(Vector velocity, double offsetX, double offsetY, double offsetZ) {
        this(null, velocity, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleTrail(Color color, double offsetX, double offsetY, double offsetZ) {
        this(color, (Location) null, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleTrail(double offsetX, double offsetY, double offsetZ, int count) {
        this(null, (Location) null, offsetX, offsetY, offsetZ, count);
    }

    public ParticleTrail(Color color, Location toGo) {
        this(color, toGo, 0, 0, 0, 1);
    }

    public ParticleTrail(Color color, Vector velocity) {
        this(color, velocity, 0, 0, 0, 1);
    }

    public ParticleTrail(Location toGo) {
        this(null, toGo, 0, 0, 0, 1);
    }

    public ParticleTrail(Vector velocity) {
        this(null, velocity, 0, 0, 0, 1);
    }

    public ParticleTrail(Color color) {
        this(color, (Location) null, 0, 0, 0, 1);
    }

    public ParticleTrail(double offsetX, double offsetY, double offsetZ) {
        this(null, (Location) null, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleTrail(int count) {
        this(null, (Location) null, 0, 0, 0, count);
    }

    public ParticleTrail() {
        this(null, (Location) null, 0, 0, 0, 1);
    }

    @Override
    public ParticleTrail inherit(Particle particle) {
        super.inherit(particle);

        if (particle instanceof ColorableParticle) {
            setColor(((ColorableParticle) particle).getColor());
        }

        if (particle instanceof ParticleTrail) {
            setArrivalTime(((ParticleTrail) particle).getArrivalTime());
        }

        if (particle instanceof ParticleVibration) {
            setArrivalTime(((ParticleVibration) particle).getArrivalTime());
        }

        return this;
    }

    @Override
    public ParticleTrail clone() {
        return new ParticleTrail().inherit(this);
    }

    @Override
    protected Vector getXYZ(Location location) {
        return LVMath.toVector(xyzHelper, location).add(generateFakeOffset());
    }

    @Override
    protected Vector getOffsets(Location location) {
        return offsetHelper.zero();
    }

    @Override
    protected int getPacketCount() {
        return 0;
    }

    @Override
    protected Object getData(Location location) {
        Location target = location.clone();
        if (toGo != null) {
            target.setX(toGo.getX()).setY(toGo.getY()).setZ(toGo.getZ());
        } else if (velocity != null) {
            target.add(velocity);
        }
        org.bukkit.Color trailColor = (color == null) ? org.bukkit.Color.fromRGB(rng.nextInt(0xffffff)) : color.toBukkitColor();
        return new org.bukkit.Particle.TargetColor(target, trailColor);
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
     * @param arrivalTime the amount of ticks it takes this particle to go from its origin to its destination, default 20 ticks or 1 second.
     */
    public ParticleTrail setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;

        return this;
    }

    @Nullable
    public Color getColor() {
        return color;
    }

    /**
     * @return the amount of ticks it takes this particle to go from its origin to its destination, default 20 ticks or 1 second.
     */
    public int getArrivalTime() {
        return arrivalTime;
    }
}
