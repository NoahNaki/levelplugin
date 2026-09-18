package me.nakilex.playerspoofer;

import net.citizensnpcs.npc.skin.profile.ProfileFetchResult;
import net.citizensnpcs.npc.skin.profile.ProfileFetcher;
import net.citizensnpcs.util.SkinProperty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Citizens/Mojang profile bridge. It verifies usernames and builds an independent pool of
 * signed skin texture properties. Skin textures are unique across active spoofed players.
 */
final class CitizensSkinResolver {
    private final PlayerSpooferPlugin plugin;
    private final FakePlayerManager manager;
    private final List<SkinData> skinDonors = new CopyOnWriteArrayList<>();
    private final Set<String> cachedSkinTextures = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<String> donorPending = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final Set<String> waitingForSkin = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private boolean available;

    CitizensSkinResolver(PlayerSpooferPlugin plugin, FakePlayerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    void initialize() {
        available = Bukkit.getPluginManager().isPluginEnabled("Citizens");
        if (!available) {
            plugin.getLogger().warning("Citizens is not enabled. Real-account verification and random skins are unavailable.");
            return;
        }

        plugin.getLogger().info("Citizens detected: real-account verification + unique random skin pool enabled.");
        List<String> duplicateAssignments = manager.clearDuplicateSkinAssignments();
        if (!duplicateAssignments.isEmpty()) {
            waitingForSkin.addAll(duplicateAssignments);
            plugin.getLogger().info("Cleared " + duplicateAssignments.size()
                    + " duplicate persisted skin assignment(s); unique replacements are being resolved.");
        }
        reconcileSkinPolicy();
        prewarmSkinDonors();
        Bukkit.getScheduler().runTaskLater(plugin.host(), this::validateExistingPlayers, 40L);
    }

    boolean isAvailable() { return available; }
    int cachedSkinCount() { return skinDonors.size(); }
    int eligibleCachedSkinCount() { return (int) skinDonors.stream().filter(s -> plugin.isSkinDonorAllowed(s.sourceName())).count(); }
    int pendingSkinCount() { return waitingForSkin.size(); }

    void applyRandomSkinsToMissing() {
        if (!available) return;
        for (FakePlayer fake : manager.all()) {
            if (!fake.hasSkin()) applyRandomSkin(fake.name());
        }
    }

    void reconcileSkinPolicy() {
        if (!available) return;
        for (FakePlayer fake : manager.all()) {
            if (!fake.hasSkin()) continue;
            String source = fake.skinSourceName();
            if (source != null && !plugin.isSkinDonorAllowed(source)) {
                manager.clearSkin(fake.name());
                waitingForSkin.add(fake.name());
            }
        }
        prewarmSkinDonors();
        applyRandomSkinsToMissing();
    }

    void applyRandomSkin(String fakePlayerName) {
        if (!available || fakePlayerName == null || !manager.contains(fakePlayerName)) return;
        SkinData skin = randomUnusedSkin(fakePlayerName);
        if (skin == null) {
            waitingForSkin.add(fakePlayerName);
            prewarmSkinDonors();
            return;
        }
        if (manager.updateSkin(fakePlayerName, skin.texture(), skin.signature(), skin.sourceName())) {
            waitingForSkin.remove(fakePlayerName);
        } else {
            waitingForSkin.add(fakePlayerName);
            prewarmSkinDonors();
        }
    }

    void addVerifiedSingle(CommandSender sender, String playerName, Location location) {
        if (!available) {
            sender.sendMessage(ChatColor.RED + "Citizens is required to verify that the username is a real Java account.");
            return;
        }
        sender.sendMessage(ChatColor.YELLOW + "Checking Mojang profile for " + playerName + "...");
        fetchProfile(playerName, 0, lookup -> runMain(() -> {
            if (lookup.result() == ProfileFetchResult.SUCCESS) {
                if (lookup.skin() != null) cacheSkin(lookup.skin());
                if (plugin.isNameCollisionProtected(playerName)) {
                    sender.sendMessage(ChatColor.RED + playerName + " is reserved because the real account has joined this server before or is currently connecting.");
                    return;
                }
                if (manager.contains(playerName)) {
                    sender.sendMessage(ChatColor.RED + playerName + " is already spoofed.");
                    return;
                }
                SkinData skin = randomUnusedSkin(playerName);
                if (skin == null) {
                    prewarmSkinDonors();
                    sender.sendMessage(ChatColor.YELLOW + playerName + " is a real account, but no unused allowed skin is cached yet. "
                            + "The skin pool is warming up; try again in a few seconds.");
                    return;
                }
                try {
                    FakePlayer fake = manager.add(playerName, location, BotOrigin.MANUAL_SINGLE, true,
                            skin.texture(), skin.signature(), skin.sourceName());
                    if (fake == null) {
                        sender.sendMessage(ChatColor.RED + playerName + " is already spoofed.");
                        return;
                    }
                    sender.sendMessage(ChatColor.GREEN + "Added verified player " + fake.name()
                            + " to TAB and the world with a unique random skin.");
                } catch (IllegalArgumentException exception) {
                    sender.sendMessage(ChatColor.RED + exception.getMessage());
                }
            } else if (lookup.result() == ProfileFetchResult.NOT_FOUND) {
                sender.sendMessage(ChatColor.RED + playerName + " is not a currently resolvable Minecraft: Java Edition username.");
            } else {
                sender.sendMessage(ChatColor.RED + "Could not verify " + playerName + " right now (" + lookup.result() + "). Try again shortly.");
            }
        }));
    }

    void addVerifiedBulk(CommandSender sender, List<String> candidates, int quantity, Location location) {
        if (!available) {
            sender.sendMessage(ChatColor.RED + "Citizens is required for bulk username verification.");
            return;
        }

        List<String> availableNames = filteredUnusedCandidates(candidates);
        Collections.shuffle(availableNames);
        if (availableNames.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No unused username candidates are available under the current whitelist/blacklist rules.");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Verifying username candidates through Mojang/Citizens. Adding up to "
                + quantity + " real accounts...");
        new BulkAddJob(sender, availableNames, quantity, location).next();
    }

    /** Silent single-player add used by target-population mode. */
    void addVerifiedTargetOne(List<String> candidates, Location location, Consumer<Boolean> completion) {
        if (!available || location == null) {
            completion.accept(false);
            return;
        }
        List<String> availableNames = filteredUnusedCandidates(candidates);
        Collections.shuffle(availableNames);
        if (availableNames.isEmpty()) {
            completion.accept(false);
            return;
        }
        new TargetAddJob(availableNames, location, completion).next();
    }

    /** Only authoritative NOT_FOUND results remove persisted spoofed usernames. */
    void validateExistingPlayers() {
        if (!available) return;
        List<String> names = manager.all().stream().map(FakePlayer::name).toList();
        if (names.isEmpty()) return;
        new ExistingValidationJob(names).next();
    }

    void prewarmSkinDonors() {
        if (!available) return;
        List<String> configured = plugin.getConfig().getStringList("skin-donor-pool");
        if (configured.isEmpty()) configured = UsernamePool.DEFAULT_SKIN_DONORS;
        int allowedDonorCount = (int) configured.stream().filter(plugin::isSkinDonorAllowed).count();
        int desired = Math.min(allowedDonorCount, Math.max(
                Math.max(4, plugin.getConfig().getInt("skin-cache-target", 64)), manager.size() + 12));
        if (eligibleCachedSkinCount() >= desired) {
            drainWaitingForSkins();
            return;
        }

        List<String> donors = configured.stream()
                .map(String::trim)
                .filter(manager::isValidName)
                .filter(plugin::isSkinDonorAllowed)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collections.shuffle(donors);

        int budget = Math.min(donors.size(), Math.max(16, desired * 2));
        for (int i = 0; i < budget; i++) {
            String name = donors.get(i);
            long delay = (long) i * Math.max(2L, plugin.getConfig().getLong("skin-prewarm-spacing-ticks", 6L));
            Bukkit.getScheduler().runTaskLater(plugin.host(), () -> fetchSkinDonor(name, 0), delay);
        }
    }

    private List<String> filteredUnusedCandidates(List<String> candidates) {
        return candidates.stream()
                .map(String::trim)
                .filter(manager::isValidName)
                .filter(plugin::isAutoPlayerAllowed)
                .filter(name -> !manager.contains(name))
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private void fetchSkinDonor(String playerName, int attempt) {
        if (!available || !plugin.isSkinDonorAllowed(playerName)) return;
        int configuredTarget = Math.max(4, plugin.getConfig().getInt("skin-cache-target", 64));
        long allowedDonors = plugin.getConfig().getStringList("skin-donor-pool").stream().filter(plugin::isSkinDonorAllowed).count();
        int dynamicTarget = (int) Math.min(allowedDonors, Math.max(configuredTarget, manager.size() + 12L));
        if (eligibleCachedSkinCount() >= dynamicTarget) return;
        String key = playerName.toLowerCase(Locale.ROOT);
        if (!donorPending.add(key)) return;

        fetchProfile(playerName, attempt, lookup -> runMain(() -> {
            donorPending.remove(key);
            if (lookup.result() == ProfileFetchResult.SUCCESS && lookup.skin() != null
                    && plugin.isSkinDonorAllowed(lookup.skin().sourceName())) {
                cacheSkin(lookup.skin());
                drainWaitingForSkins();
            }
        }));
    }

    private void cacheSkin(SkinData skin) {
        if (skin == null || skin.texture() == null || skin.texture().isBlank()) return;
        if (cachedSkinTextures.add(skin.texture())) skinDonors.add(skin);
    }

    private SkinData randomUnusedSkin(String fakePlayerName) {
        if (skinDonors.isEmpty()) return null;
        Set<String> used = manager.usedSkinTexturesExcept(fakePlayerName);
        List<SkinData> usable = skinDonors.stream()
                .filter(skin -> plugin.isSkinDonorAllowed(skin.sourceName()))
                .filter(skin -> !used.contains(skin.texture()))
                .toList();
        if (usable.isEmpty()) return null;
        return usable.get(ThreadLocalRandom.current().nextInt(usable.size()));
    }

    private void drainWaitingForSkins() {
        if (skinDonors.isEmpty() || waitingForSkin.isEmpty()) return;
        List<String> waiting = new ArrayList<>(waitingForSkin);
        waitingForSkin.clear();
        for (String fakeName : waiting) {
            if (manager.contains(fakeName)) applyRandomSkin(fakeName);
        }
    }

    private void fetchProfile(String playerName, int attempt, Consumer<Lookup> callback) {
        try {
            ProfileFetcher.fetch(playerName, request -> {
                ProfileFetchResult result = request.getResult();
                if (result == ProfileFetchResult.SUCCESS && request.getProfile() != null) {
                    SkinData skin = null;
                    try {
                        SkinProperty property = SkinProperty.fromMojangProfile(request.getProfile());
                        if (property != null && property.value != null && !property.value.isBlank()) {
                            skin = new SkinData(playerName, property.value, property.signature);
                        }
                    } catch (Throwable throwable) {
                        plugin.getLogger().fine("Profile exists but skin extraction failed for " + playerName + ": " + throwable.getMessage());
                    }
                    callback.accept(new Lookup(ProfileFetchResult.SUCCESS, skin));
                    return;
                }

                if (result == ProfileFetchResult.TOO_MANY_REQUESTS && attempt < 3) {
                    long delay = Math.max(40L, plugin.getConfig().getLong("profile-retry-delay-ticks", 100L)) * (attempt + 1L);
                    Bukkit.getScheduler().runTaskLater(plugin.host(),
                            () -> fetchProfile(playerName, attempt + 1, callback), delay);
                    return;
                }
                callback.accept(new Lookup(result, null));
            });
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Citizens profile lookup failed for " + playerName + ": " + throwable.getMessage());
            callback.accept(new Lookup(ProfileFetchResult.FAILED, null));
        }
    }

    private void runMain(Runnable runnable) {
        Bukkit.getScheduler().runTaskLater(plugin.host(), runnable, 0L);
    }

    private final class BulkAddJob {
        private final CommandSender sender;
        private final List<String> candidates;
        private final int target;
        private final Location location;
        private int cursor;
        private int checked;
        private int added;
        private int notFound;
        private int noUniqueSkin;

        private BulkAddJob(CommandSender sender, List<String> candidates, int target, Location location) {
            this.sender = sender;
            this.candidates = candidates;
            this.target = target;
            this.location = location;
        }

        private void next() {
            if (added >= target || cursor >= candidates.size()) {
                finish();
                return;
            }
            String candidate = candidates.get(cursor++);
            if (manager.contains(candidate) || !plugin.isAutoPlayerAllowed(candidate)) {
                scheduleNext();
                return;
            }

            checked++;
            fetchProfile(candidate, 0, lookup -> runMain(() -> {
                if (lookup.result() == ProfileFetchResult.SUCCESS) {
                    if (!plugin.isAutoPlayerAllowed(candidate)) {
                        scheduleNext();
                        return;
                    }
                    if (lookup.skin() != null) cacheSkin(lookup.skin());
                    if (!manager.contains(candidate)) {
                        SkinData skin = randomUnusedSkin(candidate);
                        if (skin == null) {
                            noUniqueSkin++;
                            prewarmSkinDonors();
                        } else {
                            try {
                                FakePlayer fake = manager.add(candidate, location, BotOrigin.MANUAL_MULTI, false,
                                        skin.texture(), skin.signature(), skin.sourceName());
                                if (fake != null) added++;
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                } else if (lookup.result() == ProfileFetchResult.NOT_FOUND) {
                    notFound++;
                }
                scheduleNext();
            }));
        }

        private void scheduleNext() {
            Bukkit.getScheduler().runTaskLater(plugin.host(), this::next,
                    Math.max(1L, plugin.getConfig().getLong("bulk-verification-spacing-ticks", 2L)));
        }

        private void finish() {
            sender.sendMessage(ChatColor.GREEN + "Bulk add finished: " + added + " verified real player(s) added to TAB."
                    + ChatColor.YELLOW + " Checked " + checked + " candidate(s); " + notFound + " no longer resolve as current usernames.");
            if (added < target) {
                sender.sendMessage(ChatColor.YELLOW + "Requested " + target + ", but only " + added
                        + " currently resolvable unused accounts with unique allowed skins were available."
                        + (noUniqueSkin > 0 ? " " + noUniqueSkin + " verified account(s) were skipped because no unused skin was ready yet." : ""));
            }
        }
    }

    private final class TargetAddJob {
        private final List<String> candidates;
        private final Location location;
        private final Consumer<Boolean> completion;
        private int cursor;

        private TargetAddJob(List<String> candidates, Location location, Consumer<Boolean> completion) {
            this.candidates = candidates;
            this.location = location;
            this.completion = completion;
        }

        private void next() {
            if (cursor >= candidates.size()) {
                completion.accept(false);
                return;
            }
            String candidate = candidates.get(cursor++);
            if (manager.contains(candidate) || !plugin.isAutoPlayerAllowed(candidate)) {
                scheduleNext();
                return;
            }
            fetchProfile(candidate, 0, lookup -> runMain(() -> {
                if (lookup.result() == ProfileFetchResult.SUCCESS && !manager.contains(candidate)
                        && plugin.isAutoPlayerAllowed(candidate)) {
                    if (lookup.skin() != null) cacheSkin(lookup.skin());
                    SkinData skin = randomUnusedSkin(candidate);
                    if (skin == null) {
                        prewarmSkinDonors();
                        completion.accept(false);
                        return;
                    }
                    try {
                        FakePlayer fake = manager.add(candidate, location, BotOrigin.TARGET, false,
                                skin.texture(), skin.signature(), skin.sourceName());
                        completion.accept(fake != null);
                        return;
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                scheduleNext();
            }));
        }

        private void scheduleNext() {
            Bukkit.getScheduler().runTaskLater(plugin.host(), this::next,
                    Math.max(1L, plugin.getConfig().getLong("bulk-verification-spacing-ticks", 2L)));
        }
    }

    private final class ExistingValidationJob {
        private final List<String> names;
        private int cursor;
        private int removed;

        private ExistingValidationJob(List<String> names) {
            this.names = names;
        }

        private void next() {
            if (cursor >= names.size()) {
                if (removed > 0) plugin.getLogger().info("Removed " + removed + " persisted spoofed username(s) that no longer resolve to Java accounts.");
                return;
            }
            String name = names.get(cursor++);
            if (!manager.contains(name)) {
                scheduleNext();
                return;
            }
            fetchProfile(name, 0, lookup -> runMain(() -> {
                if (lookup.result() == ProfileFetchResult.NOT_FOUND) {
                    manager.remove(name);
                    removed++;
                } else if (lookup.result() == ProfileFetchResult.SUCCESS && lookup.skin() != null) {
                    cacheSkin(lookup.skin());
                }
                scheduleNext();
            }));
        }

        private void scheduleNext() {
            Bukkit.getScheduler().runTaskLater(plugin.host(), this::next,
                    Math.max(2L, plugin.getConfig().getLong("existing-validation-spacing-ticks", 4L)));
        }
    }

    private record SkinData(String sourceName, String texture, String signature) {}
    private record Lookup(ProfileFetchResult result, SkinData skin) {}
}
