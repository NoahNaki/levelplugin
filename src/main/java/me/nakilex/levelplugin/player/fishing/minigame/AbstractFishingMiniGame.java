package me.nakilex.levelplugin.player.fishing.minigame;

import me.nakilex.levelplugin.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

/** Shared lifecycle, timeout, boss-bar UI, and cleanup for fishing mini-games. */
public abstract class AbstractFishingMiniGame implements FishingMiniGame {
    protected final Main plugin;
    protected final Player player;
    protected final long durationMs;
    private final Consumer<Boolean> completion;
    private final String instruction;
    protected BossBar bar;
    protected long endsAtMs;
    private BukkitTask task;
    private boolean finished;

    protected AbstractFishingMiniGame(Main plugin, Player player, long durationMs,
                                      String instruction, Consumer<Boolean> completion) {
        this.plugin = plugin;
        this.player = player;
        this.durationMs = durationMs;
        this.instruction = instruction;
        this.completion = completion;
    }

    @Override
    public final void start() {
        endsAtMs = System.currentTimeMillis() + durationMs;
        bar = Bukkit.createBossBar(instruction, BarColor.BLUE, BarStyle.SOLID);
        bar.addPlayer(player);
        bar.setVisible(true);
        onStart();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::runTick, 0L, 1L);
    }

    private void runTick() {
        if (finished) return;
        if (!player.isOnline() || System.currentTimeMillis() >= endsAtMs) {
            finish(timeoutSuccess());
            return;
        }
        tick();
    }

    protected abstract void tick();
    protected void onStart() { }
    protected boolean timeoutSuccess() { return false; }

    protected final void finish(boolean success) {
        if (finished) return;
        finished = true;
        if (task != null) task.cancel();
        if (bar != null) bar.removeAll();
        player.sendActionBar(Component.empty());
        if (player.isOnline()) {
            player.getWorld().playSound(player.getLocation(),
                    success ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.BLOCK_NOTE_BLOCK_BASS,
                    0.9f, success ? 1.2f : 0.8f);
        }
        completion.accept(success);
    }

    @Override public void cancel() { finish(false); }
    @Override public boolean isFinished() { return finished; }
    @Override public void handleClick() { }
    @Override public void handleSneak(boolean sneaking) { }
    @Override public void handleMovement(Movement movement) { }

    protected final void updateBar(String title, double progress) {
        bar.setTitle(title);
        bar.setProgress(clamp(progress));
    }

    protected final void actionBar(String message) {
        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    protected final String meter(double value, int width) {
        int filled = (int) Math.round(clamp(value) * width);
        return ChatColor.GREEN + "■".repeat(filled) + ChatColor.DARK_GRAY + "■".repeat(width - filled);
    }

    protected final String pointer(double position, double zoneStart, double zoneEnd, int width) {
        int pointer = Math.min(width - 1, (int) Math.floor(clamp(position) * width));
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < width; i++) {
            double cell = (i + 0.5) / width;
            out.append(i == pointer ? ChatColor.YELLOW + "◆" :
                    cell >= zoneStart && cell <= zoneEnd ? ChatColor.GREEN + "■" : ChatColor.DARK_GRAY + "■");
        }
        return out.toString();
    }

    protected final long remainingMs() { return Math.max(0L, endsAtMs - System.currentTimeMillis()); }
    protected static double clamp(double value) { return Math.max(0.0, Math.min(1.0, value)); }
}
