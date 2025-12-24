package me.nakilex.levelplugin.fishing.api;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class FishingContext {
    private final UUID playerId;
    private final World world;
    private final Location location;
    private final FishingMechanism mechanism;
    private final ItemStack rodSnapshot;
    private final Integer hookEntityId;
    private final long seed;
    private final long startTimestamp;
    private final Biome biome;
    private final boolean raining;
    private final boolean thundering;
    private final long worldTime;
    private final int liquidDepth;
    private final Material liquidMaterial;

    public FishingContext(UUID playerId,
                          World world,
                          Location location,
                          FishingMechanism mechanism,
                          ItemStack rodSnapshot,
                          Integer hookEntityId,
                          long seed,
                          long startTimestamp,
                          Biome biome,
                          boolean raining,
                          boolean thundering,
                          long worldTime,
                          int liquidDepth,
                          Material liquidMaterial) {
        this.playerId = playerId;
        this.world = world;
        this.location = location;
        this.mechanism = mechanism;
        this.rodSnapshot = rodSnapshot;
        this.hookEntityId = hookEntityId;
        this.seed = seed;
        this.startTimestamp = startTimestamp;
        this.biome = biome;
        this.raining = raining;
        this.thundering = thundering;
        this.worldTime = worldTime;
        this.liquidDepth = liquidDepth;
        this.liquidMaterial = liquidMaterial;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerId);
    }

    public World getWorld() {
        return world;
    }

    public Location getLocation() {
        return location.clone();
    }

    public FishingMechanism getMechanism() {
        return mechanism;
    }

    public ItemStack getRodSnapshot() {
        return rodSnapshot == null ? null : rodSnapshot.clone();
    }

    public Integer getHookEntityId() {
        return hookEntityId;
    }

    public long getSeed() {
        return seed;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public Biome getBiome() {
        return biome;
    }

    public boolean isRaining() {
        return raining;
    }

    public boolean isThundering() {
        return thundering;
    }

    public long getWorldTime() {
        return worldTime;
    }

    public int getLiquidDepth() {
        return liquidDepth;
    }

    public Material getLiquidMaterial() {
        return liquidMaterial;
    }
}
