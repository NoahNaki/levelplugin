package me.nakilex.levelplugin.fishing.core.game;

import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fishing.api.FishingContext;
import me.nakilex.levelplugin.fishing.api.game.FishingGame;
import me.nakilex.levelplugin.fishing.core.feedback.FeedbackService;
import me.nakilex.levelplugin.fishing.core.feedback.FishingTheme;
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
    private final FeedbackService feedbackService;
    private final FishingTheme theme;
    private BossBar bossBar;
    private BukkitTask task;
    private boolean completed;
    private int ticks;
    private double windowStart;
    private double windowEnd;

    public TimingBarGame(Main plugin,
                         FishingContext context,
                         GameDefinition definition,
                         FeedbackService feedbackService,
                         FishingTheme theme,
                         Consumer<Boolean> onComplete) {
        this.plugin = plugin;
        this.context = context;
        this.definition = definition;
        this.feedbackService = feedbackService;
        this.theme = theme;
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
        String title = theme != null ? feedbackService.formatBossBarTitle(theme.bossBarTitle())
                : ChatColor.AQUA + "Reel in the catch!";
        BarColor color = theme != null ? theme.barColor() : BarColor.BLUE;
        bossBar = Bukkit.createBossBar(title, color, BarStyle.SOLID);
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
            updateActionBar(player, progress);
            if (ticks % 10 == 0) {
                feedbackService.playLineParticles(context);
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

    private void updateActionBar(Player player, double progress) {
        String message;
        if (progress >= windowStart && progress <= windowEnd) {
            message = "&aReel now!";
        } else if (progress > windowEnd) {
            message = "&7Get ready...";
        } else {
            message = "&cToo late!";
        }
        feedbackService.showActionBar(player, message, 200L);
    }
}
