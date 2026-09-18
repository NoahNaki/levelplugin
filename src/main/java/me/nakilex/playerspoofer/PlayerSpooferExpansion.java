package me.nakilex.playerspoofer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

final class PlayerSpooferExpansion extends PlaceholderExpansion {
    private final PlayerSpooferPlugin plugin;
    private final FakePlayerManager manager;
    private final CitizensSkinResolver skins;
    private final PopulationController population;

    PlayerSpooferExpansion(PlayerSpooferPlugin plugin, FakePlayerManager manager,
                           CitizensSkinResolver skins, PopulationController population) {
        this.plugin = plugin;
        this.manager = manager;
        this.skins = skins;
        this.population = population;
    }

    @Override public String getIdentifier() { return "playerspoofer"; }
    @Override public String getAuthor() { return "Nakilex"; }
    @Override public String getVersion() { return plugin.getDescriptionVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String key = params == null ? "" : params.toLowerCase(java.util.Locale.ROOT);
        int real = Bukkit.getOnlinePlayers().size();
        int fake = manager.visibleSize();
        int[] bars = manager.pingBarCounts();
        return switch (key) {
            case "fake" -> Integer.toString(fake);
            case "configured" -> Integer.toString(manager.size());
            case "real" -> Integer.toString(real);
            case "total" -> Integer.toString(real + fake);
            case "target" -> Integer.toString(population.targetTotal());
            case "target_live" -> Integer.toString(population.liveTargetTotal());
            case "target_offset" -> Integer.toString(population.variationOffset());
            case "managed" -> Integer.toString(manager.targetManagedSize());
            case "manual" -> Integer.toString(manager.manualSize());
            case "world" -> Integer.toString(manager.worldVisibleSize());
            case "skins_cached" -> Integer.toString(skins.eligibleCachedSkinCount());
            case "skins_pending" -> Integer.toString(skins.pendingSkinCount());
            case "ping_5bar" -> Integer.toString(bars[4]);
            case "ping_4bar" -> Integer.toString(bars[3]);
            case "ping_3bar" -> Integer.toString(bars[2]);
            case "ping_2bar" -> Integer.toString(bars[1]);
            case "ping_1bar" -> Integer.toString(bars[0]);
            default -> null;
        };
    }
}
