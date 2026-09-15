package me.nakilex.levelplugin.animatedlb;

import me.nakilex.levelplugin.dialogdemo.market.DialogPixels;
import me.nakilex.levelplugin.playerhead.PlayerHeadRenderer;
import me.nakilex.levelplugin.utils.ChatFormatter;
import me.nakilex.levelplugin.utils.TooltipUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AnimatedLeaderboard {
    private static final String ENTITY_TAG = "levelplugin_animatedlb";
    private static final byte VISIBLE_OPACITY = (byte) -1;
    private static final byte INVISIBLE_OPACITY = (byte) -127;
    private static final double FOOTER_GAP = 0.18;

    /** Metadata key tying a footer's click hitbox back to the board it should advance. */
    public static final String NEXT_PAGE_META = "lp_animatedlb_next_page";

    private final JavaPlugin plugin;
    private final LeaderboardDataProvider dataProvider;
    private final Location origin;
    private final float scale;
    private final int cycleDuration;
    private final int rowCount;
    private final double animationSpeed;
    private final List<BoardType> boardTypes;
    private final List<TextDisplay> allDisplays = new ArrayList<>();
    private final List<RowDisplay> rows = new ArrayList<>();
    private final List<TextDisplay> progressSegments = new ArrayList<>();
    private TextDisplay title;
    private TextDisplay footer;
    private Interaction footerInteraction;
    private BoardType boardType;
    private int progressTick = 0;
    private boolean transitioning = false;
    private BukkitTask tickTask;

    public AnimatedLeaderboard(JavaPlugin plugin, LeaderboardDataProvider dataProvider, Location origin, float scale, int cycleDuration, int rowCount, double animationSpeed) {
        this(plugin, dataProvider, origin, scale, cycleDuration, rowCount, animationSpeed, List.of(BoardType.STRONGHOLD_STAGE, BoardType.POWER));
    }

    public AnimatedLeaderboard(JavaPlugin plugin, LeaderboardDataProvider dataProvider, Location origin, float scale,
                               int cycleDuration, int rowCount, double animationSpeed, List<BoardType> boardTypes) {
        this.plugin = plugin;
        this.dataProvider = dataProvider;
        this.origin = origin;
        this.scale = scale;
        this.cycleDuration = Math.max(20, cycleDuration);
        this.rowCount = rowCount;
        this.animationSpeed = Math.max(0.1, animationSpeed);
        this.boardTypes = boardTypes == null || boardTypes.isEmpty() ? List.of(BoardType.STRONGHOLD_STAGE, BoardType.POWER) : List.copyOf(boardTypes);
        this.boardType = this.boardTypes.get(0);
    }

    public List<BoardType> getBoardTypes() {
        return boardTypes;
    }

    public void spawn() {
        remove();
        removeOrphanedDisplays();
        title = spawnText(0, 2.2, "");
        if (cyclesBoardTypes()) {
            for (int i = 0; i < 4; i++) {
                progressSegments.add(spawnText((i - 1.5) * 0.52, 1.55, ""));
            }
        }
        for (int i = 0; i < rowCount; i++) {
            double y = 1.2 - (i * 0.24);
            Vector leftBase = new Vector(localX(-0.95), y, localZ(-0.95));
            Vector rightBase = new Vector(localX(0.95), y, localZ(0.95));
            RowDisplay row = new RowDisplay(spawnText(-0.95, y, ""), spawnText(0.95, y, ""), leftBase, rightBase, i);
            row.setOpacity(VISIBLE_OPACITY);
            rows.add(row);
        }
        if (cyclesBoardTypes()) {
            spawnFooter();
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
        if (footerInteraction != null && footerInteraction.isValid()) footerInteraction.remove();
        footerInteraction = null;
        footer = null;
        transitioning = false;
        progressTick = 0;
    }

    public void next() {
        if (cyclesBoardTypes()) transitionTo(nextBoardType());
    }

    private void startProgressTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (transitioning) return;
            progressTick++;
            if (cyclesBoardTypes()) renderProgress();
            if (progressTick < cycleDuration) return;
            if (cyclesBoardTypes()) {
                transitionTo(nextBoardType());
            } else {
                applyBoard(boardType);
                progressTick = 0;
            }
        }, 1L, 1L);
    }

    private void transitionTo(BoardType next) {
        if (transitioning || next == boardType) return;
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
            // Keep every page structurally identical: unused ranks still render their number,
            // fallback head, NONE label and a zero value, just like the original leaderboards.
            LeaderboardEntry e = i < entries.size()
                    ? entries.get(i)
                    : new LeaderboardEntry(new UUID(0, 0), "NONE", 0, 0);
            Component head = PlayerHeadRenderer.getHead(plugin, Bukkit.getOfflinePlayer(e.playerId()));
            Component left = padded(type.color() + "#" + (i + 1), RANK_WIDTH)
                    .append(head)
                    .append(Component.text(" "))
                    .append(padded(ChatColor.WHITE + e.name(), NAME_WIDTH));
            rows.get(i).setText(left, type.color() + type.format(e));
        }
        updateFooter();
    }

    /** Click-to-advance hint below the rows: a left-click glyph, and a dot per page marking the active one. */
    private void spawnFooter() {
        double footerY = 1.2 - (rowCount * 0.24) - FOOTER_GAP;
        footer = spawnText(0, footerY, "");
        Location loc = origin.clone().add(localX(0), footerY, localZ(0));
        footerInteraction = origin.getWorld().spawn(loc, Interaction.class, entity -> {
            entity.setInteractionWidth(1.6f);
            entity.setInteractionHeight(0.3f);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setPersistent(false);
            entity.setMetadata(NEXT_PAGE_META, new FixedMetadataValue(plugin, this));
        });
    }

    private void updateFooter() {
        if (footer == null) return;
        footer.setText(buildFooterText());
    }

    private String buildFooterText() {
        StringBuilder dots = new StringBuilder();
        int current = boardTypes.indexOf(boardType);
        for (int i = 0; i < boardTypes.size(); i++) {
            dots.append(i == current ? ChatColor.WHITE : ChatColor.DARK_GRAY).append('●');
            if (i < boardTypes.size() - 1) dots.append(' ');
        }
        return ChatColor.WHITE + TooltipUtil.GLYPH_LEFT_CLICK_RAW + " " + ChatColor.GRAY + "Next Page " + dots;
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
        // These displays are runtime state. Persisting them leaves an untracked copy in the world
        // after every restart, so subsequent spawns render multiple boards on top of each other.
        td.setPersistent(false);
        td.addScoreboardTag(ENTITY_TAG);
        td.setBillboard(Display.Billboard.FIXED);
        td.setText(text);
        td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        td.setSeeThrough(true);
        td.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale), new AxisAngle4f()));
        allDisplays.add(td);
        return td;
    }

    /**
     * Removes tagged displays plus untagged displays left by builds from before ENTITY_TAG existed.
     * The legacy sweep is deliberately confined to the board's small configured footprint.
     */
    private void removeOrphanedDisplays() {
        origin.getWorld().getNearbyEntities(origin, 4.0, 5.0, 4.0).stream()
                .filter(TextDisplay.class::isInstance)
                .map(TextDisplay.class::cast)
                .filter(display -> display.getScoreboardTags().contains(ENTITY_TAG) || looksLikeLegacyBoardDisplay(display))
                .forEach(TextDisplay::remove);
    }

    private boolean looksLikeLegacyBoardDisplay(TextDisplay display) {
        return display.getBillboard() == Display.Billboard.FIXED
                && display.isSeeThrough()
                && display.getBackgroundColor() != null
                && display.getBackgroundColor().getAlpha() == 0;
    }

    /**
     * A TextDisplay centres its text on the entity, so rows only share a left edge if they share a
     * total advance. Padding the two variable-width parts to a fixed pixel width does that: the rank
     * ("#1" vs "#10") and the name, which is why a short name now eats into the gap before the score
     * column instead of shifting its whole row right.
     */
    private static final int RANK_WIDTH = 20;
    private static final int NAME_WIDTH = 100;

    private static Component padded(String legacyText, int targetWidth) {
        Component text = LegacyComponentSerializer.legacySection().deserialize(legacyText);
        int used = ChatFormatter.pixelLength(legacyText);
        return used >= targetWidth ? text : text.append(DialogPixels.shift(targetWidth - used));
    }

    private boolean cyclesBoardTypes() { return boardTypes.size() > 1; }

    private BoardType nextBoardType() {
        int currentIndex = boardTypes.indexOf(boardType);
        return boardTypes.get((currentIndex + 1) % boardTypes.size());
    }

    private Vector getSlideVector(double distance) {
        double radians = Math.toRadians(origin.getYaw());
        return new Vector(Math.cos(radians) * distance, 0, Math.sin(radians) * distance);
    }

    private int getRowDurationTicks() { return Math.max(4, (int) Math.round(10D / animationSpeed)); }
    private double getSlideDistance() { return 0.9D; }
    private double localX(double x) { return Math.cos(Math.toRadians(origin.getYaw())) * x; }
    private double localZ(double x) { return Math.sin(Math.toRadians(origin.getYaw())) * x; }
    private double ease(double t) { return (3 * t * t) - (2 * t * t * t); }
    private double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
