package me.nakilex.levelplugin.calendar;

import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class CalendarManager {
    private final Main plugin;
    private final int[] monthLengths = {31,28,31,30,31,30,31,31,30,31,30,31};
    private int year = 0;
    private int month = 1; // 1-12
    private int day = 1;   // 1-based
    private long lastDayCount = -1;
    private static final String[] SEASONS = {"Winter", "Spring", "Summer", "Autumn"};
    private static final String[] PHASES = {"Early", "Mid", "Late"};
    private static final int[] MONTH_TO_SEASON = {0,0,0,1,1,1,2,2,2,3,3,3};
    private static final int[] MONTH_TO_PHASE = {1,2,0,1,2,0,1,2,0,1,2,0};

    public CalendarManager(Main plugin) {
        this.plugin = plugin;
        new BukkitRunnable() {
            @Override
            public void run() {
                checkDay();
            }
        }.runTaskTimer(plugin, 20L, 200L); // check every 10s
    }

    private void checkDay() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
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
        plugin.getScoreboardManager().updateAll();
    }

    /** Legacy numeric date for debugging */
    public String getFormattedDate() {
        return "Year " + year + " - " + month + "/" + day;
    }

    /** Returns date like "Early Spring 18th" for scoreboard display. */
    public String getSeasonDate() {
        int season = MONTH_TO_SEASON[month - 1];
        int phase = MONTH_TO_PHASE[month - 1];
        return PHASES[phase] + " " + SEASONS[season] + " " + ordinal(day);
    }

    /** Returns 12h clock time such as "9:00am" based on world time. */
    public String getTimeString() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) return "0:00am";
        long t = world.getTime();
        int hours24 = (int)((t + 6000) % 24000) / 1000;
        int minutes = (int)(60 * (t % 1000) / 1000);
        String ampm = hours24 >= 12 ? "pm" : "am";
        int hour12 = hours24 % 12;
        if (hour12 == 0) hour12 = 12;
        return String.format("%d:%02d%s", hour12, minutes, ampm);
    }

    /** Return a simple weather icon representing time of day or storm. */
    public String getWeatherIcon() {
        World world = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (world == null) return "";
        if (world.hasStorm()) return "\u2602"; // umbrella
        long t = world.getTime();
        if (t >= 13000 && t < 23000) return "\u263E"; // moon
        return "\u2600"; // sun
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
