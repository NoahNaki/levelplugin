package me.nakilex.levelplugin.particles.particles;

import me.nakilex.levelplugin.particles.particles.parents.Particle;
import me.nakilex.levelplugin.particles.particles.parents.TravellingParticle;
import me.nakilex.levelplugin.particles.util.LVMath;
import org.bukkit.Location;
import org.bukkit.Vibration;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class ParticleVibration extends TravellingParticle {

    private Entity entity = null;
    private int arrivalTime = 20;

    public ParticleVibration(Location toGo, double offsetX, double offsetY, double offsetZ, int count) {
        super("", false, 0, null, toGo, offsetX, offsetY, offsetZ, count);
        setParticleKey("vibration");
    }

    public ParticleVibration(Vector velocity, double offsetX, double offsetY, double offsetZ, int count) {
        super("", false, 0, velocity, null, offsetX, offsetY, offsetZ, count);
        setParticleKey("vibration");
    }

    public ParticleVibration(Entity entity, double offsetX, double offsetY, double offsetZ, int count) {
        super("", false, 0, null, null, offsetX, offsetY, offsetZ, count);

        this.entity = entity;
        setParticleKey("vibration");
    }

    public ParticleVibration(Location toGo, double offsetX, double offsetY, double offsetZ) {
        this(toGo, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleVibration(Vector velocity, double offsetX, double offsetY, double offsetZ) {
        this(velocity, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleVibration(Entity entity, double offsetX, double offsetY, double offsetZ) {
        this(entity, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleVibration(double offsetX, double offsetY, double offsetZ, int count) {
        this((Location) null, offsetX, offsetY, offsetZ, count);
    }

    public ParticleVibration(double offsetX, double offsetY, double offsetZ) {
        this((Location) null, offsetX, offsetY, offsetZ, 1);
    }

    public ParticleVibration(int count) {
        this((Location) null, 0, 0, 0, count);
    }

    public ParticleVibration(Location toGo) {
        this(toGo, 0, 0, 0, 1);
    }

    public ParticleVibration(Vector velocity) {
        this(velocity, 0, 0, 0, 1);
    }

    public ParticleVibration(Entity entity) {
        this(entity, 0, 0, 0, 1);
    }

    public ParticleVibration() {
        this((Location) null, 0, 0, 0, 1);
    }

    @Override
    public ParticleVibration inherit(Particle particle) {
        super.inherit(particle);

        if (particle instanceof ParticleVibration) {
            this.entity = ((ParticleVibration) particle).entity;
            this.arrivalTime = ((ParticleVibration) particle).arrivalTime;
        }

        if (particle instanceof ParticleTrail) {
            this.arrivalTime = ((ParticleTrail) particle).getArrivalTime();
        }

        return this;
    }

    @Override
    public ParticleVibration clone() {
        return new ParticleVibration().inherit(this);
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
        Vibration.Destination destination;
        if (entity != null) {
            destination = new Vibration.Destination.EntityDestination(entity);
        } else if (toGo != null) {
            destination = new Vibration.Destination.BlockDestination(toGo);
        } else if (velocity != null) {
            Location target = location.clone().add(velocity);
            destination = new Vibration.Destination.BlockDestination(target);
        } else {
            destination = new Vibration.Destination.BlockDestination(location);
        }
        return new Vibration(destination, arrivalTime);
    }

    /**
     * This particle can track entities client-side, which can make for some really neat effects.
     *
     * @param entity entity for this particle to track
     */
    public ParticleVibration setEntity(Entity entity) {
        this.entity = entity;

        return this;
    }

    /**
     * @param arrivalTime the amount of ticks it takes this particle to go from its origin to its destination, default 20 ticks or 1 second.
     */
    public ParticleVibration setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;

        return this;
    }

    /**
     * This particle can track entities client-side, which can make for some really neat effects.
     *
     * @return entity for this particle to track
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * @return the amount of ticks it takes this particle to go from its origin to its destination, default 20 ticks or 1 second.
     */
    public int getArrivalTime() {
        return arrivalTime;
    }
}
