package me.nakilex.levelplugin.animatedlb;

import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class AnimatedLeaderboard {
    private static final byte VISIBLE_OPACITY = (byte) -1;
    private static final byte INVISIBLE_OPACITY = (byte) -127;

    private final JavaPlugin plugin;
    private final LeaderboardDataProvider dataProvider;
    private final Location origin;
    private final float scale;
    private final int cycleDuration;
    private final int rowCount;
    private final double animationSpeed;
    private final List<TextDisplay> allDisplays = new ArrayList<>();
    private final List<RowDisplay> rows = new ArrayList<>();
    private final List<TextDisplay> progressSegments = new ArrayList<>();
    private TextDisplay title;
    private TextDisplay subtitle;
    private BoardType boardType = BoardType.KILLS;
    private int progressTick = 0;
    private boolean transitioning = false;
    private BukkitTask tickTask;

    public AnimatedLeaderboard(JavaPlugin plugin, LeaderboardDataProvider dataProvider, Location origin, float scale, int cycleDuration, int rowCount, double animationSpeed) {
        this.plugin = plugin;
        this.dataProvider = dataProvider;
        this.origin = origin;
        this.scale = scale;
        this.cycleDuration = Math.max(20, cycleDuration);
        this.rowCount = rowCount;
        this.animationSpeed = Math.max(0.1, animationSpeed);
    }

    public void spawn() {
        remove();
        title = spawnText(0, 2.2, "");
        subtitle = spawnText(0, 1.9, ChatColor.GRAY + "LAST 30 DAYS");
        for (int i = 0; i < 4; i++) {
            progressSegments.add(spawnText((i - 1.5) * 0.52, 1.55, ""));
        }
        for (int i = 0; i < rowCount; i++) {
            double y = 1.2 - (i * 0.24);
            Vector leftBase = new Vector(localX(-0.95), y, localZ(-0.95));
            Vector rightBase = new Vector(localX(0.95), y, localZ(0.95));
            RowDisplay row = new RowDisplay(spawnText(-0.95, y, ""), spawnText(0.95, y, ""), leftBase, rightBase, i);
            row.setOpacity(VISIBLE_OPACITY);
            rows.add(row);
        }
        applyBoard(boardType);
        startProgressTask();
    }

    public void remove() {
        if (tickTask != null) tickTask.cancel();
        allDisplays.forEach(d -> { if (d != null && d.isValid()) d.remove(); });
        allDisplays.clear();
        rows.clear();
        progressSegments.clear();
        transitioning = false;
        progressTick = 0;
    }

    public void next() { transitionTo(boardType.next()); }

    private void startProgressTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (transitioning) return;
            progressTick++;
            renderProgress();
            if (progressTick >= cycleDuration) {
                transitionTo(boardType.next());
            }
        }, 1L, 1L);
    }

    private void transitionTo(BoardType next) {
        if (transitioning) return;
        transitioning = true;
        progressTick = 0;
        animateTitleBounce(next, () -> animateRowsOut(() -> {
            boardType = next;
            applyBoard(boardType);
            teleportRowsToEntrySide();
            animateRowsIn(() -> animateTitleTyping(boardType, () -> {
                transitioning = false;
                progressTick = 0;
                renderProgress();
            }));
        }));
    }

    private void applyBoard(BoardType type) {
        title.setText(type.color() + type.icon() + " " + type.title());
        List<LeaderboardEntry> entries = dataProvider.getEntries(type, rowCount);
        for (int i = 0; i < rowCount; i++) {
            LeaderboardEntry e = i < entries.size() ? entries.get(i) : new LeaderboardEntry("NONE", 0);
            rows.get(i).setText(ChatColor.WHITE + "#" + (i + 1) + " " + e.name(), type.color() + type.format(e.value()));
        }
    }

    private void animateRowsOut(Runnable after) {
        runRowAnimation(true, after);
    }

    private void teleportRowsToEntrySide() {
        Vector offset = getSlideVector(-getSlideDistance());
        for (RowDisplay row : rows) {
            row.setOpacity(INVISIBLE_OPACITY);
            row.teleportWithOffset(origin, offset);
        }
    }

    private void animateRowsIn(Runnable after) {
        runRowAnimation(false, after);
    }

    private void runRowAnimation(boolean out, Runnable after) {
        final int rowDuration = getRowDurationTicks();
        final int rowDelayTicks = 1;
        final int totalDuration = rowDuration + ((rows.size() - 1) * rowDelayTicks);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                for (RowDisplay row : rows) {
                    int rowStartTick = row.index() * rowDelayTicks;
                    int rowLocalTick = tick - rowStartTick;

                    if (rowLocalTick < 0) {
                        if (out) {
                            row.teleportToBase(origin);
                            row.setOpacity(VISIBLE_OPACITY);
                        } else {
                            row.teleportWithOffset(origin, getSlideVector(-getSlideDistance()));
                            row.setOpacity(INVISIBLE_OPACITY);
                        }
                        continue;
                    }

                    double rowT = clamp(rowLocalTick / (double) rowDuration, 0.0, 1.0);
                    double eased = ease(rowT);
                    double distance = out
                            ? getSlideDistance() * eased
                            : -getSlideDistance() + (getSlideDistance() * eased);
                    byte opacity = out
                            ? (byte) (-1 - (126 * rowT))
                            : (byte) (-127 + (126 * rowT));

                    row.teleportWithOffset(origin, getSlideVector(distance));
                    row.setOpacity(opacity);
                }

                if (tick++ >= totalDuration) {
                    cancel();
                    if (out) {
                        rows.forEach(r -> r.setOpacity(INVISIBLE_OPACITY));
                    } else {
                        rows.forEach(r -> {
                            r.teleportToBase(origin);
                            r.setOpacity(VISIBLE_OPACITY);
                        });
                    }
                    after.run();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void animateTitleBounce(BoardType next, Runnable after) {
        title.setText(next.color() + next.icon() + " " + next.title());
        title.setInterpolationDuration(4);
        title.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale * 1.2f), new AxisAngle4f()));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            title.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale), new AxisAngle4f()));
            after.run();
        }, 5L);
    }

    private void animateTitleTyping(BoardType type, Runnable after) {
        String full = type.color() + type.icon() + " " + type.title();
        title.setText("");
        new BukkitRunnable() {
            int idx = 0;
            @Override
            public void run() {
                if (idx >= full.length()) {
                    cancel();
                    title.setText(full);
                    after.run();
                    return;
                }
                idx++;
                title.setText(full.substring(0, idx));
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void renderProgress() {
        int total = 44;
        int filled = (int) ((progressTick / (double) cycleDuration) * total);
        int per = total / 4;
        for (int i = 0; i < 4; i++) {
            int segFill = Math.max(0, Math.min(per, filled - (i * per)));
            int empty = per - segFill;
            progressSegments.get(i).setText(ChatColor.WHITE + "§m" + " ".repeat(segFill) + ChatColor.DARK_GRAY + "§m" + " ".repeat(empty));
        }
    }

    private TextDisplay spawnText(double x, double y, String text) {
        Location loc = origin.clone().add(localX(x), y, localZ(x));
        TextDisplay td = (TextDisplay) origin.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        td.setBillboard(Display.Billboard.FIXED);
        td.setText(text);
        td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        td.setSeeThrough(true);
        td.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale), new AxisAngle4f()));
        allDisplays.add(td);
        return td;
    }

    private Vector getSlideVector(double distance) {
        return switch (normalizeYaw(origin.getYaw())) {
            case 180 -> new Vector(-distance, 0, 0);
            case 90 -> new Vector(0, 0, distance);
            case 270 -> new Vector(0, 0, -distance);
            default -> new Vector(distance, 0, 0);
        };
    }

    private int getRowDurationTicks() { return Math.max(4, (int) Math.round(10D / animationSpeed)); }
    private double getSlideDistance() { return 0.9D; }
    private double localX(double x) { int yaw = normalizeYaw(origin.getYaw()); return yaw == 180 ? -x : (yaw == 0 ? x : 0); }
    private double localZ(double x) { int yaw = normalizeYaw(origin.getYaw()); return yaw == 90 ? -x : yaw == 270 ? x : 0; }
    private int normalizeYaw(float yaw) { int y = ((Math.round(yaw / 90f) * 90) % 360 + 360) % 360; return switch (y) { case 90, 180, 270 -> y; default -> 0; }; }
    private double ease(double t) { return (3 * t * t) - (2 * t * t * t); }
    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
