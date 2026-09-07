package me.nakilex.prisonenchants.mine;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class MineRegionResolver {
    private final JavaPlugin plugin;
    private Pattern allowedRegionPattern;

    public MineRegionResolver(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String expression = plugin.getConfig().getString(
                "mine-region-pattern", "(?i)^mine(?:[a-z]|p\\d+)$");
        try {
            allowedRegionPattern = Pattern.compile(expression);
        } catch (PatternSyntaxException ex) {
            plugin.getLogger().warning("Invalid mine-region-pattern; using safe default.");
            allowedRegionPattern = Pattern.compile("(?i)^mine(?:[a-z]|p\\d+)$");
        }
    }

    public Optional<MineArea> resolve(Location location) {
        if (location == null || location.getWorld() == null) return Optional.empty();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        ApplicableRegionSet regions = container.createQuery().getApplicableRegions(BukkitAdapter.adapt(location));
        return regions.getRegions().stream()
                .filter(region -> allowedRegionPattern.matcher(region.getId()).matches())
                .min(Comparator.comparingLong(this::volume))
                .map(region -> new MineArea(location.getWorld(), region));
    }

    private long volume(ProtectedRegion region) {
        long x = (long) region.getMaximumPoint().x() - region.getMinimumPoint().x() + 1;
        long y = (long) region.getMaximumPoint().y() - region.getMinimumPoint().y() + 1;
        long z = (long) region.getMaximumPoint().z() - region.getMinimumPoint().z() + 1;
        return x * y * z;
    }
}
