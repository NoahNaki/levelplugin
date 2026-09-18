package me.nakilex.playerspoofer;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Maintains a baseline population while deliberately letting the live desired total wander
 * inside a small band. Independent organic churn rotates target-managed players even when
 * the population is already correct, so TAB does not look frozen.
 */
final class PopulationController {
    private final PlayerSpooferPlugin plugin;
    private final FakePlayerManager manager;
    private final CitizensSkinResolver skinResolver;
    private int targetTotal;
    private int liveTargetTotal;
    private int variationOffset;
    private long nextActionAtMillis;
    private long nextVariationAtMillis;
    private long nextChurnAtMillis;
    private boolean addInFlight;

    PopulationController(PlayerSpooferPlugin plugin, FakePlayerManager manager, CitizensSkinResolver skinResolver) {
        this.plugin = plugin;
        this.manager = manager;
        this.skinResolver = skinResolver;
        this.targetTotal = Math.max(0, plugin.getConfig().getInt("population.target-total", 0));
        this.liveTargetTotal = targetTotal;
    }

    void start() {
        long now = System.currentTimeMillis();
        long startupSeconds = Math.max(1L, plugin.getConfig().getLong("population.startup-delay-seconds", 8L));
        nextActionAtMillis = now + startupSeconds * 1000L;
        scheduleNextVariation(now, true);
        scheduleNextChurn(now, true);
        Bukkit.getScheduler().runTaskTimer(plugin.host(), this::tick, 20L, 20L);
    }

    int targetTotal() { return targetTotal; }
    int liveTargetTotal() { return liveTargetTotal; }
    int variationOffset() { return variationOffset; }
    boolean enabled() { return targetTotal > 0; }
    boolean addInFlight() { return addInFlight; }

    long secondsUntilNextAction() {
        return Math.max(0L, (nextActionAtMillis - System.currentTimeMillis() + 999L) / 1000L);
    }

    long secondsUntilNextVariation() {
        return Math.max(0L, (nextVariationAtMillis - System.currentTimeMillis() + 999L) / 1000L);
    }

    long secondsUntilNextChurn() {
        return Math.max(0L, (nextChurnAtMillis - System.currentTimeMillis() + 999L) / 1000L);
    }

    void setTarget(int target) {
        targetTotal = Math.max(0, target);
        plugin.getConfig().set("population.target-total", targetTotal);
        plugin.saveConfig();
        variationOffset = 0;
        liveTargetTotal = targetTotal;
        long now = System.currentTimeMillis();
        scheduleNextVariation(now, true);
        scheduleNextChurn(now, true);
        if (targetTotal == 0) {
            addInFlight = false;
            manager.clearTargetManaged();
            return;
        }
        reconcileSoon();
    }

    void reconcileSoon() {
        if (!enabled()) return;
        nextActionAtMillis = Math.min(nextActionAtMillis, System.currentTimeMillis() + 1200L);
    }

    private void tick() {
        if (!enabled() || addInFlight) return;
        long now = System.currentTimeMillis();

        updateVariationIfDue(now);

        if (now >= nextChurnAtMillis) {
            if (performOrganicChurn(now)) return;
            scheduleNextChurn(now, false);
        }

        if (now < nextActionAtMillis) return;

        // Session turnover has priority even when the live population target is currently perfect.
        boolean hasExpired = manager.all().stream().anyMatch(fake -> fake.sessionExpired(now));
        if (hasExpired) {
            FakePlayer removed = manager.removeOneTargetManaged(true);
            if (removed != null) {
                scheduleNext(false);
                return;
            }
        }

        int real = Bukkit.getOnlinePlayers().size();
        int visibleFake = manager.visibleSize();
        int total = real + visibleFake;
        int desired = Math.max(real, liveTargetTotal);

        if (total < desired) {
            Location location = manager.defaultLocation();
            if (location == null) {
                scheduleRetry();
                return;
            }
            List<String> candidates = plugin.usernamePool();
            if (candidates.isEmpty()) {
                scheduleRetry();
                return;
            }

            addInFlight = true;
            skinResolver.addVerifiedTargetOne(candidates, location, success -> {
                addInFlight = false;
                if (!enabled()) {
                    if (success) manager.removeOneTargetManaged(false);
                    return;
                }
                if (success) scheduleNext(true);
                else scheduleRetry();
            });
            return;
        }

        if (total > desired) {
            FakePlayer removed = manager.removeOneTargetManaged(false);
            if (removed != null) {
                scheduleNext(false);
                return;
            }
        }

        nextActionAtMillis = now + 3000L;
    }

    private void updateVariationIfDue(long now) {
        if (!plugin.getConfig().getBoolean("population.variation.enabled", true)) {
            variationOffset = 0;
            liveTargetTotal = targetTotal;
            return;
        }
        if (now < nextVariationAtMillis) return;

        int maxDeviationPlayers = Math.max(1, plugin.getConfig().getInt("population.variation.max-deviation-players", 6));
        double percent = Math.max(0.0D, plugin.getConfig().getDouble("population.variation.max-deviation-percent", 0.12D));
        int percentDeviation = Math.max(1, (int) Math.round(targetTotal * percent));
        int bound = Math.min(maxDeviationPlayers, percentDeviation);
        int maxStep = Math.max(1, plugin.getConfig().getInt("population.variation.max-step", 3));

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int step;
        do {
            step = random.nextInt(-maxStep, maxStep + 1);
        } while (step == 0 && random.nextInt(100) < 75);

        variationOffset = clamp(variationOffset + step, -bound, bound);
        // Occasionally choose a fresh point in the band so the population does not settle into a tiny oscillation.
        if (random.nextInt(100) < 22) variationOffset = random.nextInt(-bound, bound + 1);
        liveTargetTotal = clamp(targetTotal + variationOffset, 1, 100);
        variationOffset = liveTargetTotal - targetTotal;
        scheduleNextVariation(now, false);
        nextActionAtMillis = Math.min(nextActionAtMillis, now + 1200L);
    }

    private boolean performOrganicChurn(long now) {
        if (!plugin.getConfig().getBoolean("population.churn.enabled", true)) return false;
        int currentTotal = Bukkit.getOnlinePlayers().size() + manager.visibleSize();
        int desired = Math.max(Bukkit.getOnlinePlayers().size(), liveTargetTotal);
        int minimumManaged = Math.max(1, plugin.getConfig().getInt("population.churn.minimum-managed-players", 4));
        if (manager.targetManagedSize() < minimumManaged || currentTotal < desired) return false;

        int chance = clamp(plugin.getConfig().getInt("population.churn.chance-percent", 82), 0, 100);
        if (ThreadLocalRandom.current().nextInt(100) >= chance) return false;

        FakePlayer removed = manager.removeOneTargetManaged(false);
        scheduleNextChurn(now, false);
        if (removed == null) return false;

        // The empty slot remains visible for a few seconds before normal target reconciliation adds somebody else.
        long minGap = Math.max(2L, plugin.getConfig().getLong("population.churn.replacement-gap-seconds-min", 5L));
        long maxGap = Math.max(minGap, plugin.getConfig().getLong("population.churn.replacement-gap-seconds-max", 14L));
        nextActionAtMillis = now + ThreadLocalRandom.current().nextLong(minGap, maxGap + 1L) * 1000L;
        return true;
    }

    private void scheduleNext(boolean join) {
        long min = Math.max(1L, plugin.getConfig().getLong(
                join ? "population.join-delay-seconds-min" : "population.leave-delay-seconds-min",
                join ? 3L : 4L));
        long max = Math.max(min, plugin.getConfig().getLong(
                join ? "population.join-delay-seconds-max" : "population.leave-delay-seconds-max",
                join ? 10L : 12L));
        long delay = ThreadLocalRandom.current().nextLong(min, max + 1L);
        nextActionAtMillis = System.currentTimeMillis() + delay * 1000L;
    }

    private void scheduleNextVariation(long now, boolean startup) {
        long min = Math.max(10L, plugin.getConfig().getLong("population.variation.interval-seconds-min", 35L));
        long max = Math.max(min, plugin.getConfig().getLong("population.variation.interval-seconds-max", 95L));
        if (startup) min = Math.min(min, 25L);
        nextVariationAtMillis = now + ThreadLocalRandom.current().nextLong(min, max + 1L) * 1000L;
    }

    private void scheduleNextChurn(long now, boolean startup) {
        long min = Math.max(15L, plugin.getConfig().getLong("population.churn.interval-seconds-min", 35L));
        long max = Math.max(min, plugin.getConfig().getLong("population.churn.interval-seconds-max", 100L));
        if (startup) min = Math.max(15L, Math.min(min, 30L));
        nextChurnAtMillis = now + ThreadLocalRandom.current().nextLong(min, max + 1L) * 1000L;
    }

    private void scheduleRetry() {
        long retry = Math.max(5L, plugin.getConfig().getLong("population.failed-add-retry-seconds", 15L));
        nextActionAtMillis = System.currentTimeMillis() + retry * 1000L;
        skinResolver.prewarmSkinDonors();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
