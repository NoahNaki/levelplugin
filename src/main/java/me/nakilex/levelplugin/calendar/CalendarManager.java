package me.nakilex.levelplugin.calendar;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import java.util.Random;

public class CalendarManager implements Listener {
    private final Main plugin;
    private World world;
    private final Random random = new Random();
    private String baseWeatherGlyph = GLYPH_HOT;
    private String overrideWeatherGlyph = null;
    private WeatherType currentWeather = WeatherType.CLEAR;
    private final int[] monthLengths = {31,28,31,30,31,30,31,31,30,31,30,31};
    private int year = 0;
    private int month = 1; // 1-12
    private int day = 1;   // 1-based
    private long lastDayCount = -1;
    private static final String[] SEASONS = {"Winter", "Spring", "Summer", "Autumn"};
    private static final String[] PHASES = {"Early", "Mid", "Late"};
    private static final int[] MONTH_TO_SEASON = {0,0,0,1,1,1,2,2,2,3,3,3};
    private static final int[] MONTH_TO_PHASE = {1,2,0,1,2,0,1,2,0,1,2,0};

    // Tokens for glyphs defined in the resource pack
    private static final String GLYPH_WINTER = "<glyph:winter>";
    private static final String GLYPH_THUNDER = "<glyph:thunder>";
    private static final String GLYPH_SUMMER = "<glyph:summer>";
    private static final String GLYPH_SPRING = "<glyph:spring>";
    private static final String GLYPH_SNOW = "<glyph:snow>";
    private static final String GLYPH_RAIN = "<glyph:rain>";
    private static final String GLYPH_HOT = "<glyph:hot>";
    private static final String GLYPH_FALL = "<glyph:fall>";
    private static final String GLYPH_COLD = "<glyph:cold>";
    private static final String GLYPH_CLOUD = "<glyph:cloud>";

    public CalendarManager(Main plugin) {
        this.plugin = plugin;
        this.world = Bukkit.getWorld("world");
        if (this.world == null && !Bukkit.getWorlds().isEmpty()) {
            this.world = Bukkit.getWorlds().get(0);
        }
        if (this.world != null) {
            this.world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        chooseDailyWeather();
        new BukkitRunnable() {
            @Override
            public void run() {
                checkDay();
            }
        }.runTaskTimer(plugin, 20L, 200L); // check every 10s
    }

    private void checkDay() {
        if (world == null) return;
        long dayCount = world.getFullTime() / 24000L;
        if (dayCount != lastDayCount) {
            lastDayCount = dayCount;
            advanceDay();
        }
    }

    private void advanceDay() {
        day++;
        if (day > monthLengths[month - 1]) {
            day = 1;
            month++;
            if (month > 12) {
                month = 1;
                year++;
            }
        }
        chooseDailyWeather();
        plugin.getScoreboardManager().updateAll();
        plugin.getEnvironmentManager().grantDailyPayout("town");
    }

    /** Legacy numeric date for debugging */
    public String getFormattedDate() {
        return "Year " + year + " - " + month + "/" + day;
    }

    /**
     * Returns date like "ꑆ Early Spring 18th" with the custom season glyph.
     */
    public String getSeasonDate() {
        int season = MONTH_TO_SEASON[month - 1];
        int phase = MONTH_TO_PHASE[month - 1];
        String glyph = getSeasonGlyph(season);
        return glyph + " " + PHASES[phase] + " " + SEASONS[season] + " " + ordinal(day);
    }

    /** Glyph representing the current season. */
    public String getSeasonGlyph() {
        return getSeasonGlyph(MONTH_TO_SEASON[month - 1]);
    }

    private String getSeasonGlyph(int seasonIndex) {
        switch (seasonIndex) {
            case 0: return GLYPH_WINTER;
            case 1: return GLYPH_SPRING;
            case 2: return GLYPH_SUMMER;
            case 3: return GLYPH_FALL;
            default: return GLYPH_CLOUD;
        }
    }

    /** Returns 12h clock time such as "9:00am" based on world time. */
    public String getTimeString() {
        if (world == null) return "0:00am";
        long t = world.getTime();
        int hours24 = (int)((t + 6000) % 24000) / 1000;
        int minutes = (int)(60 * (t % 1000) / 1000);
        String ampm = hours24 >= 12 ? "pm" : "am";
        int hour12 = hours24 % 12;
        if (hour12 == 0) hour12 = 12;
        return String.format("%d:%02d%s", hour12, minutes, ampm);
    }

    /**
     * Return a glyph representing current weather and time of day.
     */
    public String getWeatherGlyph() {
        if (overrideWeatherGlyph != null) return overrideWeatherGlyph;
        return baseWeatherGlyph;
    }

    private void chooseDailyWeather() {
        int pick = random.nextInt(4);
        switch (pick) {
            case 0:
                currentWeather = WeatherType.CLEAR;
                break;
            case 1:
                currentWeather = WeatherType.RAIN;
                break;
            case 2:
                currentWeather = WeatherType.SNOW;
                break;
            default:
                currentWeather = WeatherType.STORM;
                break;
        }
        applyWeather(currentWeather);
    }

    private void applyWeather(WeatherType type) {
        if (world == null) return;
        switch (type) {
            case CLEAR:
                world.setStorm(false);
                world.setThundering(false);
                world.setWeatherDuration(20 * 60 * 10);
                baseWeatherGlyph = GLYPH_HOT;
                break;
            case RAIN:
                world.setStorm(true);
                world.setThundering(false);
                world.setWeatherDuration(20 * 60 * 10);
                baseWeatherGlyph = GLYPH_RAIN;
                break;
            case SNOW:
                world.setStorm(true);
                world.setThundering(false);
                world.setWeatherDuration(20 * 60 * 10);
                baseWeatherGlyph = GLYPH_SNOW;
                break;
            case STORM:
                world.setStorm(true);
                world.setThundering(true);
                world.setWeatherDuration(20 * 60 * 10);
                baseWeatherGlyph = GLYPH_THUNDER;
                break;
        }
        updateOverride();
    }

    private void updateOverride() {
        if (world == null) {
            overrideWeatherGlyph = GLYPH_CLOUD;
            return;
        }
        if (world.isThundering()) {
            overrideWeatherGlyph = GLYPH_THUNDER;
            return;
        }
        if (world.hasStorm()) {
            org.bukkit.block.Biome biome = world.getBiome(world.getSpawnLocation());
            if (biome.name().contains("SNOW")) {
                overrideWeatherGlyph = GLYPH_SNOW;
            } else {
                overrideWeatherGlyph = GLYPH_RAIN;
            }
            return;
        }
        overrideWeatherGlyph = null;
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!event.getWorld().equals(world)) return;
        updateOverride();
        plugin.getScoreboardManager().updateAll();
    }

    @EventHandler
    public void onThunderChange(ThunderChangeEvent event) {
        if (!event.getWorld().equals(world)) return;
        updateOverride();
        plugin.getScoreboardManager().updateAll();
    }

    private static String ordinal(int n) {
        if (n % 100 >= 11 && n % 100 <= 13) return n + "th";
        switch (n % 10) {
            case 1: return n + "st";
            case 2: return n + "nd";
            case 3: return n + "rd";
            default: return n + "th";
        }
    }
}
