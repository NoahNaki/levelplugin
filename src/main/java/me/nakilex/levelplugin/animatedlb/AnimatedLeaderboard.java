package me.nakilex.levelplugin.animatedlb;

import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class AnimatedLeaderboard {
    private final JavaPlugin plugin;
    private final LeaderboardDataProvider dataProvider;
    private final Location origin;
    private final float scale;
    private final int cycleDuration;
    private final int rowCount;
    private final double animationSpeed;
    private final List<TextDisplay> allDisplays = new ArrayList<>();
    private final List<RowDisplay> rows = new ArrayList<>();
    private TextDisplay title;
    private TextDisplay subtitle;
    private final List<TextDisplay> progressSegments = new ArrayList<>();
    private BoardType boardType = BoardType.KILLS;
    private int progressTick = 0;
    private BukkitTask tickTask;

    public AnimatedLeaderboard(JavaPlugin plugin, LeaderboardDataProvider dataProvider, Location origin, float scale, int cycleDuration, int rowCount, double animationSpeed) {
        this.plugin = plugin; this.dataProvider = dataProvider; this.origin = origin; this.scale = scale;
        this.cycleDuration = Math.max(20, cycleDuration); this.rowCount = rowCount; this.animationSpeed = Math.max(0.1, animationSpeed);
    }

    public void spawn() {
        remove();
        title = spawnText(0, 2.2, "");
        subtitle = spawnText(0, 1.9, ChatColor.GRAY + "LAST 30 DAYS");
        for (int i = 0; i < 4; i++) progressSegments.add(spawnText((i - 1.5) * 0.52, 1.55, ""));
        for (int i = 0; i < rowCount; i++) {
            double y = 1.2 - (i * 0.24);
            rows.add(new RowDisplay(spawnText(-0.95, y, ""), spawnText(0.95, y, "")));
        }
        applyBoard(boardType);
        startProgressTask();
    }

    public void remove() { if (tickTask != null) tickTask.cancel(); allDisplays.forEach(d -> { if (d != null && d.isValid()) d.remove();}); allDisplays.clear(); rows.clear(); progressSegments.clear(); }
    public void next() { transitionTo(boardType.next()); }

    private void startProgressTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            progressTick++; renderProgress();
            if (progressTick >= cycleDuration) { progressTick = 0; transitionTo(boardType.next()); }
        }, 1L, 1L);
    }

    private void transitionTo(BoardType next) {
        boardType = next;
        bounceTitle();
        slideRows(false, 0.9);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            applyBoard(boardType);
            slideRows(true, -0.9);
            typeTitle();
        }, Math.max(4L, Math.round(10 / animationSpeed)));
    }

    private void applyBoard(BoardType type) {
        title.setText(type.color() + type.icon() + " " + type.title());
        List<LeaderboardEntry> entries = dataProvider.getEntries(type, rowCount);
        for (int i = 0; i < rowCount; i++) {
            LeaderboardEntry e = i < entries.size() ? entries.get(i) : new LeaderboardEntry("NONE", 0);
            RowDisplay row = rows.get(i);
            row.left().setText(ChatColor.WHITE + "#" + (i + 1) + " " + e.name());
            row.right().setText(type.color() + type.format(e.value()));
        }
    }

    private void renderProgress() {
        int total = 44; int filled = (int) ((progressTick / (double) cycleDuration) * total);
        int per = total / 4;
        for (int i = 0; i < 4; i++) {
            int segFill = Math.max(0, Math.min(per, filled - (i * per)));
            int empty = per - segFill;
            progressSegments.get(i).setText(ChatColor.WHITE + "§m" + " ".repeat(segFill) + ChatColor.DARK_GRAY + "§m" + " ".repeat(empty));
        }
    }

    private void bounceTitle() {
        title.setInterpolationDuration(4);
        title.setTransformation(new Transformation(new Vector3f(), new org.joml.AxisAngle4f(), new Vector3f(scale * 1.2f), new org.joml.AxisAngle4f()));
        Bukkit.getScheduler().runTaskLater(plugin, () -> title.setTransformation(new Transformation(new Vector3f(), new org.joml.AxisAngle4f(), new Vector3f(scale), new org.joml.AxisAngle4f())), 5L);
    }

    private void slideRows(boolean in, double fromOffset) {
        for (RowDisplay row : rows) {
            setOpacity(row, in ? 0 : 255);
            Location left = row.left().getLocation(); Location right = row.right().getLocation();
            double eased = ease(1.0);
            double offset = fromOffset * (1.0 - eased);
            row.left().teleport(left.add(localX(offset), 0, localZ(offset)));
            row.right().teleport(right.add(localX(offset), 0, localZ(offset)));
            setOpacity(row, in ? 255 : 0);
        }
    }

    private void typeTitle() {
        String full = boardType.color() + boardType.icon() + " " + boardType.title();
        title.setText("");
        for (int i = 1; i <= full.length(); i++) {
            int idx = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> title.setText(full.substring(0, Math.min(idx, full.length()))), i);
        }
    }

    private TextDisplay spawnText(double x, double y, String text) {
        Location loc = origin.clone().add(localX(x), y, localZ(x));
        TextDisplay td = (TextDisplay) origin.getWorld().spawnEntity(loc, EntityType.TEXT_DISPLAY);
        td.setBillboard(Display.Billboard.FIXED);
        td.setText(text); td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0)); td.setSeeThrough(true);
        td.setTransformation(new Transformation(new Vector3f(), new org.joml.AxisAngle4f(), new Vector3f(scale), new org.joml.AxisAngle4f()));
        allDisplays.add(td);
        return td;
    }

    private double localX(double x) { int yaw = normalizeYaw(origin.getYaw()); return yaw == 90 ? 0 : yaw == 270 ? 0 : yaw == 180 ? -x : x; }
    private double localZ(double x) { int yaw = normalizeYaw(origin.getYaw()); return yaw == 90 ? -x : yaw == 270 ? x : 0; }
    private int normalizeYaw(float yaw) { int y = ((Math.round(yaw / 90f) * 90) % 360 + 360) % 360; return switch (y) {case 90,180,270 -> y; default -> 0;}; }
    private double ease(double t) { return (3 * t * t) - (2 * t * t * t); }
    private void setOpacity(RowDisplay row, int opacity) { row.left().setTextOpacity((byte) opacity); row.right().setTextOpacity((byte) opacity); }
}
