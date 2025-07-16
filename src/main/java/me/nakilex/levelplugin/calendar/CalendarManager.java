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

    public String getFormattedDate() {
        return "Year " + year + " - " + month + "/" + day;
    }
}
