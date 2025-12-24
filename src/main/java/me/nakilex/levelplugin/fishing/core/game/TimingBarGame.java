package me.nakilex.levelplugin.fishing.core.game;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.game.FishingGame;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Random;
import java.util.function.Consumer;

public class TimingBarGame implements FishingGame {
    private final Main plugin;
    private final FishingContext context;
    private final GameDefinition definition;
    private final Consumer<Boolean> onComplete;
    private BossBar bossBar;
    private BukkitTask task;
    private boolean completed;
    private int ticks;
    private double windowStart;
    private double windowEnd;

    public TimingBarGame(Main plugin, FishingContext context, GameDefinition definition, Consumer<Boolean> onComplete) {
        this.plugin = plugin;
        this.context = context;
        this.definition = definition;
        this.onComplete = onComplete;
    }

    @Override
    public void start() {
        Player player = context.getPlayer();
        if (player == null) {
            finish(false);
            return;
        }
        setupWindow();
        bossBar = Bukkit.createBossBar(ChatColor.AQUA + "Reel in the catch!", BarColor.BLUE, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);
        bossBar.setVisible(true);
        ticks = 0;
        int duration = Math.max(1, definition.durationTicks());
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (completed) {
                return;
            }
            Player current = context.getPlayer();
            if (current == null || !current.isOnline()) {
                finish(false);
                return;
            }
            ticks++;
            double progress = Math.max(0.0, 1.0 - (ticks / (double) duration));
            if (bossBar != null) {
                bossBar.setProgress(progress);
            }
            if (ticks >= duration) {
                finish(false);
            }
        }, 0L, 1L);
    }

    @Override
    public void handlePlayerAction() {
        if (completed) {
            return;
        }
        int duration = Math.max(1, definition.durationTicks());
        double progress = Math.max(0.0, 1.0 - (ticks / (double) duration));
        boolean success = progress >= windowStart && progress <= windowEnd;
        finish(success);
    }

    @Override
    public void cancel() {
        finish(false);
    }

    private void setupWindow() {
        double min = Math.max(0.0, Math.min(1.0, definition.windowMin()));
        double max = Math.max(min, Math.min(1.0, definition.windowMax()));
        Random random = new Random(context.getSeed());
        double size = min == max ? min : min + (random.nextDouble() * (max - min));
        size = Math.max(0.01, Math.min(1.0, size));
        double start = random.nextDouble() * (1.0 - size);
        windowStart = start;
        windowEnd = start + size;
    }

    private void finish(boolean success) {
        if (completed) {
            return;
        }
        completed = true;
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
            bossBar = null;
        }
        onComplete.accept(success);
    }
}
