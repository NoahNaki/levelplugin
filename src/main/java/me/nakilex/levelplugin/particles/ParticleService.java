package me.nakilex.levelplugin.particles;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;
import com.github.fierioziy.particlenativeapi.api.particle.type.ParticleType;
import com.github.fierioziy.particlenativeapi.api.packet.ParticlePacket;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.Objects;
import java.util.Optional;

public final class ParticleService {
    private static final ParticleService INSTANCE = new ParticleService();

    private ParticleNativeAPI api;

    private ParticleService() {
    }

    public static ParticleService getInstance() {
        return INSTANCE;
    }

    public void initialize(ParticleNativeAPI api) {
        this.api = Objects.requireNonNull(api, "ParticleNativeAPI must not be null");
    }

    public ParticleNativeAPI getApi() {
        return Objects.requireNonNull(api, "ParticleNativeAPI has not been initialized");
    }

    public Optional<ParticleType> resolveParticleType(String input) {
        ParticleNativeAPI resolvedApi = getApi();
        String key = input == null ? "" : input.trim().toUpperCase().replace('-', '_');
        if (key.isEmpty()) {
            return Optional.empty();
        }
        Optional<ParticleType> from13 = findOnList(resolvedApi.LIST_1_13, key);
        if (from13.isPresent()) {
            return from13;
        }
        Optional<ParticleType> from19 = findOnList(resolvedApi.LIST_1_19_PART, key);
        if (from19.isPresent()) {
            return from19;
        }
        return findOnList(resolvedApi.LIST_1_8, key);
    }

    public void sendToPlayer(Player player, ParticleType type, Location location, int count) {
        ParticlePacket packet = type.packet(false, location, count);
        packet.sendTo(player);
    }

    public void sendRing(Player player, ParticleType type, Location center, double radius, int points) {
        double step = (Math.PI * 2.0) / points;
        for (int i = 0; i < points; i++) {
            double angle = step * i;
            double x = center.getX() + (Math.cos(angle) * radius);
            double z = center.getZ() + (Math.sin(angle) * radius);
            type.packet(false, x, center.getY(), z, 1).sendTo(player);
        }
    }

    public void sendArc(Player player, ParticleType type, Location center, double radius, double degrees, int points) {
        double radians = Math.toRadians(degrees);
        double step = radians / Math.max(points - 1, 1);
        double start = -radians / 2.0;
        for (int i = 0; i < points; i++) {
            double angle = start + (step * i);
            double x = center.getX() + (Math.cos(angle) * radius);
            double z = center.getZ() + (Math.sin(angle) * radius);
            type.packet(false, x, center.getY(), z, 1).sendTo(player);
        }
    }

    public boolean isReady() {
        return api != null;
    }

    public void reset() {
        api = null;
    }

    private Optional<ParticleType> findOnList(Object list, String key) {
        try {
            Field field = list.getClass().getField(key);
            Object value = field.get(list);
            if (value instanceof ParticleType particleType && particleType.isPresent()) {
                return Optional.of(particleType);
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // Particle not found in this list.
        }
        return Optional.empty();
    }
}
