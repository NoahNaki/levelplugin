package hm.zelha.particlesfx.particles.parents;

import hm.zelha.particlesfx.util.LVMath;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public abstract class Particle implements IParticle {

    protected final ThreadLocalRandom rng = ThreadLocalRandom.current();
    protected final Vector fakeOffsetHelper = new Vector();
    protected final Vector xyzHelper = new Vector();
    protected final Vector offsetHelper = new Vector();
    private static final Map<String, org.bukkit.Particle> PARTICLES_BY_KEY = new HashMap<>();
    protected org.bukkit.Particle particle;
    protected Object data;
    protected double offsetX;
    protected double offsetY;
    protected double offsetZ;
    protected double speed = 0;
    protected int count;
    protected int radius = 0;
    private final List<Player> players = new ArrayList<>();
    private final List<Player> listHelper = new ArrayList<>();

    static {
        for (org.bukkit.Particle particle : org.bukkit.Particle.values()) {
            if (particle.getKey() != null) {
                PARTICLES_BY_KEY.put(particle.getKey().getKey().toLowerCase(Locale.ROOT), particle);
            }
            PARTICLES_BY_KEY.put(particle.name().toLowerCase(Locale.ROOT), particle);
        }
    }

    protected Particle(String particleID, double offsetX, double offsetY, double offsetZ, int count) {
        setParticleKey(particleID);

        setOffset(offsetX, offsetY, offsetZ);
        setCount(count);
    }

    public void display(Location location) {
        players.clear();
        players.addAll(Bukkit.getOnlinePlayers());
        display(location, players);
    }

    public void displayForPlayers(Location location, Player... players) {
        listHelper.clear();

        for (Player player : players) {
            listHelper.add(player);
        }

        display(location, listHelper);
    }

    public void displayForPlayers(Location location, List<UUID> players) {
        listHelper.clear();

        for (int i = 0; i < players.size(); i++) {
            Player p = Bukkit.getPlayer(players.get(i));

            if (p == null) continue;

            listHelper.add(p);
        }

        display(location, listHelper);
    }

    public Particle inherit(Particle particle) {
        offsetX = particle.offsetX;
        offsetY = particle.offsetY;
        offsetZ = particle.offsetZ;
        speed = particle.speed;
        count = particle.count;
        radius = particle.radius;
        this.particle = particle.particle;
        this.data = particle.data;

        return this;
    }

    public abstract Particle clone();

    protected void display(Location location, List<Player> players) {
        Validate.notNull(location, "Location cannot be null!");
        Validate.notNull(location.getWorld(), "World cannot be null!");
        updateData(location);

        if (particle == null) {
            return;
        }

        for (int i = 0; i < ((getPacketCount() != count) ? count : 1); i++) {
            for (int k = 0; k < players.size(); k++) {
                Player player = players.get(k);

                if (player == null) continue;
                if (!location.getWorld().equals(player.getWorld())) continue;

                if (radius != 0) {
                    double distance = location.distanceSquared(player.getLocation());

                    if (distance > Math.pow(radius, 2)) continue;
                }

                Vector xyz = getXYZ(location);
                Vector offsets = getOffsets(location);
                Location spawnLocation = new Location(location.getWorld(), xyz.getX(), xyz.getY(), xyz.getZ());
                if (displaySpecial(player, spawnLocation)) {
                    continue;
                }
                Object payload = getData(spawnLocation);
                if (payload == null && particle.getDataType() != Void.class) {
                    continue;
                }
                player.spawnParticle(
                        particle,
                        spawnLocation,
                        getPacketCount(),
                        offsets.getX(),
                        offsets.getY(),
                        offsets.getZ(),
                        getPacketSpeed(),
                        payload
                );
            }
        }
    }

    /** @return a vector meant to be added to a location to mimic particle offset */
    protected Vector generateFakeOffset() {
        fakeOffsetHelper.setX(rng.nextGaussian() * offsetX);
        fakeOffsetHelper.setY(rng.nextGaussian() * offsetY);
        fakeOffsetHelper.setZ(rng.nextGaussian() * offsetZ);

        return fakeOffsetHelper;
    }

    /**
     * Meant to be overridden by child classes to modify the X/Y/Z values of the packet sent to the player.
     *
     * @param location the location passed into the display method
     * @return the xyzHelper with the modified X/Y/Z values
     */
    protected Vector getXYZ(Location location) {
        return LVMath.toVector(xyzHelper, location);
    }

    /**
     * Meant to be overridden by child classes to modify the offset values of the packet sent to the player.
     *
     * @param location the location passed into the display method
     * @return the offsetHelper with the modified offset values
     */
    protected Vector getOffsets(Location location) {
        return offsetHelper.setX(offsetX).setY(offsetY).setZ(offsetZ);
    }

    /**
     * Meant to be overridden by child classes to modify the speed value of the packet sent to the player.
     *
     * @return the modified speed value
     */
    protected float getPacketSpeed() {
        return (float) speed;
    }

    /**
     * Meant to be overridden by child classes to modify the count value of the packet sent to the player.
     *
     * @return the modified count value
     */
    protected int getPacketCount() {
        return count;
    }

    /**
     * Meant to be overridden by child classes when a different packet is needed to display the particle.
     *
     * @param location the location passed into the display method
     * @return the different packet
     */
    protected boolean displaySpecial(Player player, Location location) {
        return false;
    }

    protected void updateData(Location location) {
    }

    protected Object getData(Location location) {
        return data;
    }

    protected void setParticleKey(String key) {
        this.particle = resolveParticle(key);
    }

    protected void setParticleKey(String key, Object data) {
        this.particle = resolveParticle(key);
        this.data = data;
    }

    private static org.bukkit.Particle resolveParticle(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String cleaned = key.toLowerCase(Locale.ROOT);
        if (cleaned.startsWith("minecraft:")) {
            cleaned = cleaned.substring("minecraft:".length());
        }
        return PARTICLES_BY_KEY.get(cleaned);
    }

    public void setOffset(double x, double y, double z) {
        setOffsetX(x);
        setOffsetY(y);
        setOffsetZ(z);
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = Math.abs(offsetX);
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = Math.abs(offsetY);
    }

    public void setOffsetZ(double offsetZ) {
        this.offsetZ = Math.abs(offsetZ);
    }

    public Particle setSpeed(double speed) {
        this.speed = speed;

        return this;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Particle setRadius(int radius) {
        this.radius = radius;

        return this;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public double getSpeed() {
        return speed;
    }

    public int getCount() {
        return count;
    }

    public int getRadius() {
        return radius;
    }
}
