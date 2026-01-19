package me.nakilex.levelplugin.particles;

import com.github.fierioziy.particlenativeapi.api.ParticleNativeAPI;

import java.util.Objects;

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

    public boolean isReady() {
        return api != null;
    }

    public void reset() {
        api = null;
    }
}
