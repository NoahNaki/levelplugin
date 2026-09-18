package me.nakilex.playerspoofer;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientChatMessage;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PlayerSpoofer, merged into LevelPlugin as a module rather than a separate plugin jar.
 *
 * <p>This was a {@code JavaPlugin} of its own. It now runs inside LevelPlugin and keeps the same
 * surface the rest of the package calls ({@code getConfig()}, {@code getLogger()},
 * {@code getDataFolder()}), so the other classes are unchanged apart from handing the real plugin
 * instance to the scheduler via {@link #host()}. Its config and saved state live in
 * {@code plugins/LevelPlugin/playerspoofer/} instead of {@code plugins/PlayerSpoofer/}.</p>
 */
public final class PlayerSpooferPlugin implements Listener {
    private static final String VERSION = "1.7.1";

    private final JavaPlugin host;
    private final File dataFolder;
    private final Logger logger;
    private FileConfiguration config;

    private RealPlayerRegistry realPlayerRegistry;
    private FakePlayerManager fakePlayerManager;
    private CitizensSkinResolver skinResolver;
    private PopulationController populationController;
    private ChatDirector chatDirector;
    private PacketListenerCommon packetChatListener;
    private FakeChatAppearance appearance;

    public PlayerSpooferPlugin(JavaPlugin host) {
        this.host = host;
        this.dataFolder = new File(host.getDataFolder(), "playerspoofer");
        this.logger = host.getLogger();
    }

    /** Shared rank/face styling for fake players, so chat and the tab list stay consistent. */
    FakeChatAppearance appearance() {
        if (appearance == null) {
            appearance = new FakeChatAppearance(this);
        }
        return appearance;
    }

    /** The real plugin instance, for APIs that require one (scheduler, event registration). */
    public JavaPlugin host() {
        return host;
    }

    File getDataFolder() {
        return dataFolder;
    }

    Logger getLogger() {
        return logger;
    }

    FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile());
    }

    void saveConfig() {
        try {
            getConfig().save(configFile());
        } catch (IOException ex) {
            logger.warning("Could not save PlayerSpoofer config: " + ex.getMessage());
        }
    }

    /**
     * Writes the bundled default config on first run. Also adopts a config left behind by the
     * standalone PlayerSpoofer plugin, so an existing install keeps its tuning and its bots
     * instead of silently starting from defaults.
     */
    void saveDefaultConfig() {
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.warning("Could not create " + dataFolder + "; PlayerSpoofer will use defaults.");
        }
        if (configFile().exists()) {
            return;
        }
        File legacyFolder = new File(host.getDataFolder().getParentFile(), "PlayerSpoofer");
        File legacyConfig = new File(legacyFolder, "config.yml");
        if (legacyConfig.isFile()) {
            copyLegacyData(legacyFolder);
            return;
        }
        try (InputStream defaults = host.getResource("playerspoofer/config.yml")) {
            if (defaults == null) {
                logger.warning("Bundled PlayerSpoofer config.yml is missing from the jar.");
                return;
            }
            Files.copy(defaults, configFile().toPath());
        } catch (IOException ex) {
            logger.warning("Could not write the default PlayerSpoofer config: " + ex.getMessage());
        }
    }

    /** Copies config and saved state across from a previous standalone PlayerSpoofer install. */
    private void copyLegacyData(File legacyFolder) {
        File[] files = legacyFolder.listFiles();
        if (files == null) {
            return;
        }
        int copied = 0;
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            try {
                Files.copy(file.toPath(), new File(dataFolder, file.getName()).toPath());
                copied++;
            } catch (IOException ex) {
                logger.warning("Could not carry over " + file.getName()
                        + " from the old PlayerSpoofer folder: " + ex.getMessage());
            }
        }
        logger.info("Adopted " + copied + " file(s) from the standalone PlayerSpoofer install at "
                + legacyFolder + ".");
    }

    private File configFile() {
        return new File(dataFolder, "config.yml");
    }

    public void enable() {
        saveDefaultConfig();
        int configVersion = getConfig().getInt("config-version", 0);
        boolean migrateLatencyProfiles = configVersion < 5;

        if (configVersion < 3) {
            getConfig().set("username-pool", UsernamePool.DEFAULT_NAMES);
            getConfig().set("skin-donor-pool", UsernamePool.DEFAULT_SKIN_DONORS);
        }
        if (configVersion < 4) {
            getConfig().set("skin-cache-target", Math.max(32, getConfig().getInt("skin-cache-target", 16)));
        }
        if (configVersion < 5) {
            getConfig().set("config-version", 5);
            getConfig().set("skin-cache-target", Math.max(64, getConfig().getInt("skin-cache-target", 32)));
            getConfig().set("population.target-total", 0);
            getConfig().set("population.startup-delay-seconds", 8);
            getConfig().set("population.join-delay-seconds-min", 4);
            getConfig().set("population.join-delay-seconds-max", 15);
            getConfig().set("population.leave-delay-seconds-min", 5);
            getConfig().set("population.leave-delay-seconds-max", 20);
            getConfig().set("population.failed-add-retry-seconds", 15);
            getConfig().set("population.session-minutes-min", 20);
            getConfig().set("population.session-minutes-max", 240);
            getConfig().set("selection.player-blacklist", List.of());
            getConfig().set("selection.player-whitelist", List.of());
            getConfig().set("selection.skin-blacklist", List.of());
            getConfig().set("selection.skin-whitelist", List.of());
        }
        if (configVersion < 6) {
            installChatDefaults();
            getConfig().set("config-version", 6);
        }
        if (configVersion < 7) {
            installCollisionAndPopulationDefaults();
            getConfig().set("config-version", 7);
        }
        if (configVersion < 8) {
            installChatContextAndDisplayDefaults();
            getConfig().set("config-version", 8);
        }
        if (configVersion < 9) {
            installChatCaptureDefaults();
            getConfig().set("config-version", 9);
        }
        if (configVersion < 10) {
            installChatRealismDefaults();
            getConfig().set("config-version", 10);
        }
        if (configVersion < 11) {
            installChatSocialDefaults();
            getConfig().set("config-version", 11);
        }
        if (getConfig().getStringList("username-pool").isEmpty()) getConfig().set("username-pool", UsernamePool.DEFAULT_NAMES);
        if (getConfig().getStringList("skin-donor-pool").isEmpty()) getConfig().set("skin-donor-pool", UsernamePool.DEFAULT_SKIN_DONORS);
        saveConfig();

        realPlayerRegistry = new RealPlayerRegistry(this);
        realPlayerRegistry.initialize();

        fakePlayerManager = new FakePlayerManager(this);
        fakePlayerManager.load();
        removePersistedAutomaticHistoryConflicts();
        if (migrateLatencyProfiles) fakePlayerManager.rerollLatencyProfiles();

        skinResolver = new CitizensSkinResolver(this, fakePlayerManager);
        skinResolver.initialize();
        skinResolver.applyRandomSkinsToMissing();

        populationController = new PopulationController(this, fakePlayerManager, skinResolver);
        populationController.start();

        chatDirector = new ChatDirector(this, fakePlayerManager);
        chatDirector.start();
        registerPacketChatCapture();

        Bukkit.getPluginManager().registerEvents(this, host);
        for (Player player : Bukkit.getOnlinePlayers()) {
            Bukkit.getScheduler().runTaskLater(host, () -> fakePlayerManager.showAll(player), 10L);
        }

        long pingJitterTicks = Math.max(100L, getConfig().getLong("ping-jitter-interval-ticks", 600L));
        Bukkit.getScheduler().runTaskTimer(host, fakePlayerManager::jitterLatencies, pingJitterTicks, pingJitterTicks);

        registerPlaceholderExpansion();
        getLogger().info("PlayerSpoofer " + VERSION + " enabled with " + fakePlayerManager.size()
                + " fake players. Target population=" + populationController.targetTotal() + ".");
    }

    public void disable() {
        unregisterPacketChatCapture();
        if (chatDirector != null) chatDirector.shutdown();
        if (fakePlayerManager == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) fakePlayerManager.hideAll(player);
        fakePlayerManager.save();
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        if (fakePlayerManager == null) return;
        int spoofed = event.getNumPlayers() + fakePlayerManager.visibleSize();
        event.setMaxPlayers(Math.max(event.getMaxPlayers(), spoofed));
        event.setNumPlayers(spoofed);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if (realPlayerRegistry == null || fakePlayerManager == null) return;
        String realName = event.getName();
        realPlayerRegistry.reservePreLogin(realName, event.getUniqueId());

        // Bukkit/PacketEvents operations stay on the primary thread. The reservation above is
        // already visible to target selection, so the same name cannot be selected in this gap.
        Bukkit.getScheduler().runTask(host, () -> {
            FakePlayer removed = fakePlayerManager.remove(realName);
            if (removed != null) {
                getLogger().info("Collision guard retired spoofed " + removed.name()
                        + " because the real account began joining.");
                if (populationController != null) populationController.reconcileSoon();
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Second synchronous collision check in case a later login stage bypassed/delayed the
        // async retirement task. A genuine join always wins over a spoofed identity.
        FakePlayer removed = fakePlayerManager.remove(player.getName());
        if (removed != null) {
            getLogger().info("Collision guard retired spoofed " + removed.name()
                    + " on real PlayerJoinEvent.");
        }
        realPlayerRegistry.recordSuccessfulJoin(player);

        Bukkit.getScheduler().runTaskLater(host, () -> fakePlayerManager.showAll(player), 10L);
        Bukkit.getScheduler().runTaskLater(host, populationController::reconcileSoon, 1L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(host, () -> fakePlayerManager.showAll(player), 2L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || event.getFrom().getWorld() != event.getTo().getWorld()) return;
        int fromChunkX = floorToChunk(event.getFrom().getX());
        int fromChunkZ = floorToChunk(event.getFrom().getZ());
        int toChunkX = floorToChunk(event.getTo().getX());
        int toChunkZ = floorToChunk(event.getTo().getZ());
        if (fromChunkX != toChunkX || fromChunkZ != toChunkZ) {
            Player player = event.getPlayer();
            Bukkit.getScheduler().runTaskLater(host, () -> fakePlayerManager.refreshEntities(player), 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onAsyncChat(AsyncChatEvent event) {
        if (chatDirector == null) return;
        if (event.isCancelled() && !getConfig().getBoolean("chat.capture.cancelled-events", true)) return;

        // PacketEvents is the authoritative capture path on 1.21.11. Avoid deserializing the
        // Adventure Component here: Paper 1.21.11's Adventure ABI differs from several older
        // compile-time variants and can throw IncompatibleClassChangeError. The packet listener
        // has already captured this exact message; the legacy listener remains a fallback.
    }

    /**
     * Compatibility capture for chat stacks that still consume/fire Bukkit's legacy chat event.
     * Paper 1.21.11 still exposes AsyncPlayerChatEvent (deprecated), and a surprising number of
     * formatter plugins remain wired to it rather than Paper's Adventure AsyncChatEvent.
     * ChatDirector de-duplicates the two event streams when both are emitted for one message.
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onLegacyAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (chatDirector == null) return;
        boolean cancelled = event.isCancelled();
        if (cancelled && !getConfig().getBoolean("chat.capture.cancelled-events", true)) return;
        dispatchCapturedChat(event.getPlayer(), event.getMessage(), cancelled, "legacy");
    }

    private void dispatchCapturedChat(Player player, String message, boolean cancelled, String source) {
        // Never touch the original chat event or its renderer. Move only our bookkeeping/AI
        // selection onto the primary thread when the source event arrived asynchronously.
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(host, () -> {
                if (chatDirector != null) chatDirector.onPlayerChat(player, message, cancelled, source);
            });
        } else if (chatDirector != null) {
            chatDirector.onPlayerChat(player, message, cancelled, source);
        }
    }


    private void registerPacketChatCapture() {
        try {
            packetChatListener = PacketEvents.getAPI().getEventManager().registerListener(new PacketListener() {
                @Override
                public void onPacketReceive(PacketReceiveEvent event) {
                    if (event.getPacketType() != PacketType.Play.Client.CHAT_MESSAGE) return;
                    Object rawPlayer = event.getPlayer();
                    if (!(rawPlayer instanceof Player player)) return;

                    String message;
                    try {
                        message = new WrapperPlayClientChatMessage(event).getMessage();
                    } catch (Throwable throwable) {
                        getLogger().warning("Failed to decode incoming chat packet from " + player.getName() + ": " + throwable.getClass().getSimpleName());
                        return;
                    }

                    // PacketEvents runs on the networking thread. Never perform Bukkit/chat-director
                    // state changes there; dispatch onto the server thread. ChatDirector de-duplicates
                    // this against Paper/legacy chat events if those also fire.
                    dispatchCapturedChat(player, message, event.isCancelled(), "packet");
                }
            }, PacketListenerPriority.MONITOR);
            getLogger().info("PacketEvents raw CHAT_MESSAGE capture enabled.");
        } catch (Throwable throwable) {
            packetChatListener = null;
            getLogger().log(java.util.logging.Level.WARNING,
                    "Could not register PacketEvents raw chat capture; Paper/Bukkit chat listeners remain active.", throwable);
        }
    }

    private void unregisterPacketChatCapture() {
        if (packetChatListener == null) return;
        try {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetChatListener);
        } catch (Throwable ignored) {
        } finally {
            packetChatListener = null;
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (populationController != null) Bukkit.getScheduler().runTaskLater(host, populationController::reconcileSoon, 1L);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playerspoofer.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "move" -> handleMove(sender, args);
            case "list" -> handleList(sender);
            case "stats" -> handleStats(sender);
            case "target" -> handleTarget(sender, args);
            case "chat" -> handleChat(sender, args);
            case "blacklist" -> handleSelectionList(sender, args, true);
            case "whitelist" -> handleSelectionList(sender, args, false);
            case "clear" -> {
                int cleared = fakePlayerManager.clear();
                sender.sendMessage(ChatColor.GREEN + "Cleared " + cleared + " fake player(s)."
                        + (populationController.enabled() ? ChatColor.YELLOW + " Target mode is still enabled and will repopulate gradually." : ""));
                populationController.reconcileSoon();
            }
            case "refresh" -> {
                for (Player player : Bukkit.getOnlinePlayers()) fakePlayerManager.showAll(player);
                skinResolver.reconcileSkinPolicy();
                skinResolver.applyRandomSkinsToMissing();
                populationController.reconcileSoon();
                sender.sendMessage(ChatColor.GREEN + "Refreshed fake players, skin policy and population target.");
            }
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length == 2 && !args[1].equalsIgnoreCase("player") && !args[1].equalsIgnoreCase("multi")) {
            addSingle(sender, args[1]);
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /spoofer add player <name> OR /spoofer add multi <quantity>");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "player" -> addSingle(sender, args[2]);
            case "multi" -> addMulti(sender, args[2]);
            default -> sender.sendMessage(ChatColor.RED + "Usage: /spoofer add player <name> OR /spoofer add multi <quantity>");
        }
    }

    private void addSingle(CommandSender sender, String name) {
        if (!fakePlayerManager.isValidName(name)) {
            sender.sendMessage(ChatColor.RED + "Invalid name. Use 1-16 letters, numbers or underscores.");
            return;
        }
        if (isListed("selection.player-blacklist", name)) {
            sender.sendMessage(ChatColor.RED + name + " is in the spoofed-player blacklist.");
            return;
        }
        if (realPlayerRegistry != null && realPlayerRegistry.isExcludedName(name)
                && getConfig().getBoolean("collision-protection.history-excludes-manual", true)) {
            sender.sendMessage(ChatColor.RED + name + " is reserved because that real player has joined this server before or is currently connecting.");
            return;
        }
        if (fakePlayerManager.contains(name)) {
            sender.sendMessage(ChatColor.RED + name + " is already spoofed.");
            return;
        }

        Location location = sender instanceof Player player
                ? player.getLocation().add(2.0, 0.0, 0.0)
                : fakePlayerManager.defaultLocation();
        skinResolver.addVerifiedSingle(sender, name, location);
    }

    private void addMulti(CommandSender sender, String rawQuantity) {
        Integer quantity = parseQuantity(rawQuantity, sender);
        if (quantity == null) return;
        Location location = fakePlayerManager.defaultLocation();
        if (location == null) {
            sender.sendMessage(ChatColor.RED + "No loaded world is available.");
            return;
        }
        skinResolver.addVerifiedBulk(sender, usernamePool(), quantity, location);
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length == 2 && !args[1].equalsIgnoreCase("player") && !args[1].equalsIgnoreCase("multi")) {
            removeSingle(sender, args[1]);
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /spoofer remove player <name> OR /spoofer remove multi <quantity>");
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "player" -> removeSingle(sender, args[2]);
            case "multi" -> removeMulti(sender, args[2]);
            default -> sender.sendMessage(ChatColor.RED + "Usage: /spoofer remove player <name> OR /spoofer remove multi <quantity>");
        }
    }

    private void removeSingle(CommandSender sender, String name) {
        FakePlayer removed = fakePlayerManager.remove(name);
        sender.sendMessage(removed == null
                ? ChatColor.RED + "Fake player not found."
                : ChatColor.GREEN + "Removed " + removed.name() + " from TAB" + (removed.worldVisible() ? " and the world." : "."));
        populationController.reconcileSoon();
    }

    private void removeMulti(CommandSender sender, String rawQuantity) {
        Integer quantity = parseQuantity(rawQuantity, sender);
        if (quantity == null) return;
        List<FakePlayer> removed = fakePlayerManager.removeBulk(quantity);
        sender.sendMessage(ChatColor.GREEN + "Removed " + removed.size() + " manually bulk-added spoofed player(s)."
                + (removed.size() < quantity ? ChatColor.YELLOW + " Only " + removed.size() + " manual multi players were available." : ""));
        populationController.reconcileSoon();
    }

    private void handleMove(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command must be run by a player.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /spoofer move <name>");
            return;
        }
        FakePlayer before = fakePlayerManager.all().stream()
                .filter(fake -> fake.name().equalsIgnoreCase(args[1])).findFirst().orElse(null);
        if (before != null && !before.worldVisible()) {
            sender.sendMessage(ChatColor.YELLOW + before.name() + " is TAB-only and has no world entity to move.");
            return;
        }
        FakePlayer moved = fakePlayerManager.move(args[1], player.getLocation().add(2.0, 0.0, 0.0));
        sender.sendMessage(moved == null ? ChatColor.RED + "Fake player not found." : ChatColor.GREEN + "Moved " + moved.name() + " next to you.");
    }

    private void handleList(CommandSender sender) {
        if (fakePlayerManager.size() == 0) {
            sender.sendMessage(ChatColor.YELLOW + "No fake players configured.");
            return;
        }
        long now = System.currentTimeMillis();
        String list = fakePlayerManager.all().stream()
                .map(fake -> fake.name() + "[" + fake.ping() + "ms," + fake.connectionProfile().name().toLowerCase(Locale.ROOT)
                        + "," + fake.origin().name().toLowerCase(Locale.ROOT)
                        + ",skin=" + (fake.hasSkin() ? (fake.skinSourceName() == null ? "random" : fake.skinSourceName()) : "pending")
                        + ",persona=" + fake.personalityId()
                        + ",style=" + WritingStyleEngine.styleFor(fake).name().toLowerCase(Locale.ROOT)
                        + (fake.targetManaged() && fake.sessionEndsAtMillis() > 0
                        ? ",session=" + Math.max(0L, (fake.sessionEndsAtMillis() - now) / 60_000L) + "m" : "") + "]")
                .collect(Collectors.joining(", "));
        sender.sendMessage(ChatColor.YELLOW + "Fake players (visible=" + fakePlayerManager.visibleSize() + ", configured=" + fakePlayerManager.size() + "): " + list);
    }

    private void handleStats(CommandSender sender) {
        int real = Bukkit.getOnlinePlayers().size();
        int fake = fakePlayerManager.visibleSize();
        int[] bars = fakePlayerManager.pingBarCounts();
        sender.sendMessage(ChatColor.GOLD + "PlayerSpoofer 1.7.1 stats");
        sender.sendMessage(ChatColor.YELLOW + "Population: " + ChatColor.WHITE + real + " real + " + fake + " fake = " + (real + fake)
                + (populationController.enabled() ? ChatColor.YELLOW + " / baseline " + populationController.targetTotal()
                + " / live target " + populationController.liveTargetTotal() : ChatColor.GRAY + " (target off)"));
        sender.sendMessage(ChatColor.YELLOW + "Fakes: " + ChatColor.WHITE + fakePlayerManager.manualSize() + " manual, "
                + fakePlayerManager.targetManagedSize() + " target-managed, " + fakePlayerManager.worldVisibleSize() + " world entities");
        sender.sendMessage(ChatColor.YELLOW + "Skins: " + ChatColor.WHITE + fakePlayerManager.usedSkinCount() + " unique active, "
                + skinResolver.eligibleCachedSkinCount() + " cached/allowed, " + skinResolver.pendingSkinCount() + " pending");
        sender.sendMessage(ChatColor.YELLOW + "Ping bars: " + ChatColor.WHITE + "5=" + bars[4] + " 4=" + bars[3] + " 3=" + bars[2]
                + " 2=" + bars[1] + " 1=" + bars[0]
                + (populationController.enabled() ? ChatColor.GRAY + " | next population action ~" + populationController.secondsUntilNextAction() + "s" : ""));
        if (populationController.enabled()) {
            sender.sendMessage(ChatColor.YELLOW + "Population motion: " + ChatColor.WHITE
                    + "offset " + (populationController.variationOffset() >= 0 ? "+" : "") + populationController.variationOffset()
                    + ChatColor.GRAY + " | next variation ~" + populationController.secondsUntilNextVariation() + "s"
                    + " | next churn ~" + populationController.secondsUntilNextChurn() + "s");
        }
        if (realPlayerRegistry != null) {
            sender.sendMessage(ChatColor.YELLOW + "Collision guard: " + ChatColor.WHITE
                    + realPlayerRegistry.historyNameCount() + " historical real name(s), "
                    + realPlayerRegistry.activeReservationCount() + " active login reservation(s)");
        }
        if (chatDirector != null) sender.sendMessage(ChatColor.YELLOW + "Chat: " + ChatColor.WHITE
                + (chatDirector.enabled() ? "enabled" : "disabled") + " / " + chatDirector.providerMode().name().toLowerCase(Locale.ROOT));
    }

    private void handleTarget(CommandSender sender, String[] args) {
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            sender.sendMessage(populationController.enabled()
                    ? ChatColor.YELLOW + "Baseline target is " + populationController.targetTotal()
                    + "; live target is currently " + populationController.liveTargetTotal()
                    + " (offset " + (populationController.variationOffset() >= 0 ? "+" : "") + populationController.variationOffset() + "). "
                    + "Current total: " + (Bukkit.getOnlinePlayers().size() + fakePlayerManager.visibleSize()) + "."
                    : ChatColor.YELLOW + "Target population mode is disabled.");
            return;
        }
        if (args[1].equalsIgnoreCase("off") || args[1].equals("0")) {
            int before = fakePlayerManager.targetManagedSize();
            populationController.setTarget(0);
            sender.sendMessage(ChatColor.GREEN + "Target population mode disabled; removed " + before + " target-managed fake player(s). Manual fakes were kept.");
            return;
        }
        try {
            int target = Integer.parseInt(args[1]);
            if (target < 1 || target > 100) {
                sender.sendMessage(ChatColor.RED + "Target must be between 1 and 100, or 'off'.");
                return;
            }
            populationController.setTarget(target);
            sender.sendMessage(ChatColor.GREEN + "Baseline target set to " + target + " total visible players. The live target will wander around it and rotate players organically.");
        } catch (NumberFormatException exception) {
            sender.sendMessage(ChatColor.RED + "Usage: /spoofer target <1-100|off|status>");
        }
    }

    private void handleChat(CommandSender sender, String[] args) {
        if (chatDirector == null) {
            sender.sendMessage(ChatColor.RED + "Chat system is not initialized.");
            return;
        }
        if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
            sender.sendMessage(ChatColor.GOLD + "PlayerSpoofer chat status");
            for (String line : chatDirector.statusLines()) sender.sendMessage(ChatColor.YELLOW + line);
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "on" -> {
                chatDirector.setEnabled(true);
                sender.sendMessage(ChatColor.GREEN + "Spoofed-player chat enabled.");
            }
            case "off" -> {
                chatDirector.setEnabled(false);
                sender.sendMessage(ChatColor.GREEN + "Spoofed-player chat disabled.");
            }
            case "reload" -> {
                reloadConfig();
                chatDirector.reload();
                sender.sendMessage(ChatColor.GREEN + "Reloaded PlayerSpoofer chat configuration/providers.");
            }
            case "provider" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /spoofer chat provider <template|openrouter|ollama|hybrid>");
                    return;
                }
                try {
                    ChatProviderMode mode = ChatProviderMode.valueOf(args[2].toUpperCase(Locale.ROOT));
                    chatDirector.setProvider(mode);
                    sender.sendMessage(ChatColor.GREEN + "Chat provider mode set to " + mode.name().toLowerCase(Locale.ROOT) + ".");
                } catch (IllegalArgumentException exception) {
                    sender.sendMessage(ChatColor.RED + "Provider must be template, openrouter, ollama or hybrid.");
                }
            }
            case "say" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /spoofer chat say <fake-player> <message...>");
                    return;
                }
                String message = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                sender.sendMessage(chatDirector.manualSay(args[2], message)
                        ? ChatColor.GREEN + "Fake message sent."
                        : ChatColor.RED + "Visible fake player not found or message was empty.");
            }
            case "test" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /spoofer chat test <fake-player> <message...>");
                    return;
                }
                String message = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                chatDirector.forceTest(sender, args[2], message);
            }
            case "memory" -> {
                if (args.length < 4) {
                    sender.sendMessage(ChatColor.RED + "Usage: /spoofer chat memory <fake-player> <real-player-name>");
                    return;
                }
                sender.sendMessage(ChatColor.YELLOW + chatDirector.memoryDescription(args[2], args[3]));
            }
            case "context" -> {
                int lines = 12;
                if (args.length >= 3) {
                    try { lines = Math.max(1, Math.min(40, Integer.parseInt(args[2]))); }
                    catch (NumberFormatException ignored) {
                        sender.sendMessage(ChatColor.RED + "Usage: /spoofer chat context [1-40]");
                        return;
                    }
                }
                sender.sendMessage(ChatColor.GOLD + "Sanitized AI chat context (latest " + lines + "):");
                for (String line : chatDirector.contextPreview(lines)) sender.sendMessage(ChatColor.GRAY + line);
            }
            case "clearcontext" -> {
                chatDirector.clearContext();
                sender.sendMessage(ChatColor.GREEN + "Cleared in-memory AI chat context/conversation leases.");
            }
            default -> sender.sendMessage(ChatColor.RED + "Usage: /spoofer chat <status|on|off|reload|provider|say|test|memory|context|clearcontext>");
        }
    }

    private void handleSelectionList(CommandSender sender, String[] args, boolean blacklist) {
        String root = blacklist ? "blacklist" : "whitelist";
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /spoofer " + root + " <player|skin> <add|remove|list> [name]");
            return;
        }
        String type = args[1].toLowerCase(Locale.ROOT);
        if (!type.equals("player") && !type.equals("skin")) {
            sender.sendMessage(ChatColor.RED + "Type must be player or skin.");
            return;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        String path = "selection." + type + "-" + root;
        if (action.equals("list")) {
            List<String> values = getConfig().getStringList(path);
            sender.sendMessage(ChatColor.YELLOW + type + " " + root + " (" + values.size() + "): "
                    + (values.isEmpty() ? ChatColor.GRAY + "empty" : ChatColor.WHITE + String.join(", ", values)));
            return;
        }
        if (!action.equals("add") && !action.equals("remove")) {
            sender.sendMessage(ChatColor.RED + "Action must be add, remove or list.");
            return;
        }
        if (args.length < 4 || !fakePlayerManager.isValidName(args[3])) {
            sender.sendMessage(ChatColor.RED + "Provide a valid Minecraft username.");
            return;
        }
        String name = args[3];
        List<String> values = new ArrayList<>(getConfig().getStringList(path));
        boolean changed;
        if (action.equals("add")) {
            changed = values.stream().noneMatch(v -> v.equalsIgnoreCase(name));
            if (changed) values.add(name);
        } else {
            changed = values.removeIf(v -> v.equalsIgnoreCase(name));
        }
        getConfig().set(path, values);
        saveConfig();
        sender.sendMessage(changed ? ChatColor.GREEN + "Updated " + type + " " + root + ": " + action + " " + name
                : ChatColor.YELLOW + "No change; " + name + " was already in the requested state.");

        if (type.equals("skin")) {
            skinResolver.reconcileSkinPolicy();
        } else {
            // Automatic selections (manual-multi + target mode) obey the policy immediately.
            List<String> toRemove = fakePlayerManager.all().stream()
                    .filter(fake -> fake.origin() != BotOrigin.MANUAL_SINGLE)
                    .filter(fake -> !isAutoPlayerAllowed(fake.name()))
                    .map(FakePlayer::name)
                    .toList();
            for (String remove : toRemove) fakePlayerManager.remove(remove);
            if (!toRemove.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "Removed " + toRemove.size() + " automatically-selected fake player(s) that no longer match the policy.");
            }
            if (blacklist && action.equals("add") && fakePlayerManager.contains(name)) {
                fakePlayerManager.remove(name);
                sender.sendMessage(ChatColor.YELLOW + "Removed currently spoofed " + name + " because it is now blacklisted.");
            }
            populationController.reconcileSoon();
        }
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("playerspoofer.admin")) return List.of();
        if (args.length == 1) {
            return filterPrefix(List.of("add", "remove", "move", "list", "stats", "target", "chat", "blacklist", "whitelist", "clear", "refresh"), args[0]);
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) return filterPrefix(List.of("player", "multi"), args[1]);
            if (args[0].equalsIgnoreCase("move")) return filterPrefix(fakeNames(false), args[1]);
            if (args[0].equalsIgnoreCase("target")) return filterPrefix(List.of("status", "off", "25", "50", "75", "100"), args[1]);
            if (args[0].equalsIgnoreCase("chat")) return filterPrefix(List.of("status", "on", "off", "reload", "provider", "say", "test", "memory", "context", "clearcontext"), args[1]);
            if (args[0].equalsIgnoreCase("blacklist") || args[0].equalsIgnoreCase("whitelist")) return filterPrefix(List.of("player", "skin"), args[1]);
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("add") && args[1].equalsIgnoreCase("player")) {
                List<String> unused = usernamePool().stream().filter(name -> !fakePlayerManager.contains(name)).toList();
                return filterPrefix(unused, args[2]);
            }
            if (args[0].equalsIgnoreCase("remove") && args[1].equalsIgnoreCase("player")) return filterPrefix(fakeNames(true), args[2]);
            if ((args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) && args[1].equalsIgnoreCase("multi")) {
                return filterPrefix(List.of("1", "5", "10", "25", "50"), args[2]);
            }
            if (args[0].equalsIgnoreCase("chat")) {
                if (args[1].equalsIgnoreCase("provider")) return filterPrefix(List.of("template", "openrouter", "ollama", "hybrid"), args[2]);
                if (args[1].equalsIgnoreCase("context")) return filterPrefix(List.of("5", "10", "20", "30", "40"), args[2]);
                if (args[1].equalsIgnoreCase("say") || args[1].equalsIgnoreCase("test") || args[1].equalsIgnoreCase("memory")) return filterPrefix(fakeNames(true), args[2]);
            }
            if (args[0].equalsIgnoreCase("blacklist") || args[0].equalsIgnoreCase("whitelist")) {
                return filterPrefix(List.of("add", "remove", "list"), args[2]);
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("chat") && args[1].equalsIgnoreCase("memory")) {
            return filterPrefix(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[3]);
        }
        if (args.length == 4 && (args[0].equalsIgnoreCase("blacklist") || args[0].equalsIgnoreCase("whitelist"))) {
            String root = args[0].toLowerCase(Locale.ROOT);
            String type = args[1].toLowerCase(Locale.ROOT);
            String action = args[2].toLowerCase(Locale.ROOT);
            if (action.equals("remove")) return filterPrefix(getConfig().getStringList("selection." + type + "-" + root), args[3]);
            if (action.equals("add")) {
                List<String> source = type.equals("skin") ? getConfig().getStringList("skin-donor-pool") : getConfig().getStringList("username-pool");
                return filterPrefix(source, args[3]);
            }
        }
        return List.of();
    }

    List<String> usernamePool() {
        return getConfig().getStringList("username-pool").stream()
                .map(String::trim)
                .filter(fakePlayerManager::isValidName)
                .filter(this::isAutoPlayerAllowed)
                .distinct()
                .toList();
    }

    boolean isAutoPlayerAllowed(String name) {
        if (name == null || isListed("selection.player-blacklist", name)) return false;
        if (realPlayerRegistry != null && realPlayerRegistry.isExcludedName(name)) return false;
        List<String> whitelist = getConfig().getStringList("selection.player-whitelist");
        return whitelist.isEmpty() || whitelist.stream().anyMatch(v -> v.equalsIgnoreCase(name));
    }

    boolean isNameCollisionProtected(String name) {
        return realPlayerRegistry != null && realPlayerRegistry.isExcludedName(name);
    }

    boolean isSkinDonorAllowed(String name) {
        if (name == null || isListed("selection.skin-blacklist", name)) return false;
        List<String> whitelist = getConfig().getStringList("selection.skin-whitelist");
        return whitelist.isEmpty() || whitelist.stream().anyMatch(v -> v.equalsIgnoreCase(name));
    }

    String getDescriptionVersion() {
        return VERSION;
    }

    private boolean isListed(String path, String name) {
        return getConfig().getStringList(path).stream().anyMatch(v -> v.equalsIgnoreCase(name));
    }

    private List<String> fakeNames(boolean includeTabOnly) {
        return fakePlayerManager.all().stream()
                .filter(fake -> includeTabOnly || fake.worldVisible())
                .map(FakePlayer::name)
                .toList();
    }

    private static List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        Set<String> deduped = new LinkedHashSet<>();
        for (String option : options) if (option != null && option.toLowerCase(Locale.ROOT).startsWith(lower)) deduped.add(option);
        return new ArrayList<>(deduped);
    }

    private static Integer parseQuantity(String raw, CommandSender sender) {
        try {
            int quantity = Integer.parseInt(raw);
            if (quantity < 1 || quantity > 100) {
                sender.sendMessage(ChatColor.RED + "Quantity must be between 1 and 100.");
                return null;
            }
            return quantity;
        } catch (NumberFormatException exception) {
            sender.sendMessage(ChatColor.RED + "Quantity must be a number between 1 and 100.");
            return null;
        }
    }

    private void registerPlaceholderExpansion() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return;
        try {
            boolean registered = new PlayerSpooferExpansion(this, fakePlayerManager, skinResolver, populationController).register();
            getLogger().info("PlaceholderAPI integration " + (registered ? "registered" : "was already registered or rejected") + ".");
        } catch (Throwable throwable) {
            getLogger().warning("Could not register PlaceholderAPI expansion: " + throwable.getMessage());
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "/spoofer add player <name>" + ChatColor.YELLOW + " - verified TAB + world player");
        sender.sendMessage(ChatColor.GOLD + "/spoofer add multi <quantity>" + ChatColor.YELLOW + " - verified TAB-only players");
        sender.sendMessage(ChatColor.GOLD + "/spoofer remove player <name> | /spoofer remove multi <quantity>");
        sender.sendMessage(ChatColor.GOLD + "/spoofer target <1-100|off|status>" + ChatColor.YELLOW + " - gradually maintain total real+fake population");
        sender.sendMessage(ChatColor.GOLD + "/spoofer stats | /spoofer list | /spoofer move <name> | /spoofer clear | /spoofer refresh");
        sender.sendMessage(ChatColor.GOLD + "/spoofer chat <status|on|off|provider|test|say|memory|context|clearcontext|reload>" + ChatColor.YELLOW + " - fake-player chat/LLM controls");
        sender.sendMessage(ChatColor.GOLD + "/spoofer blacklist|whitelist <player|skin> <add|remove|list> [name]");
    }

    private void installChatDefaults() {
        setDefault("chat.enabled", true);
        setDefault("chat.provider", "HYBRID");
        setDefault("chat.reply-chance", 0.11D);
        setDefault("chat.mention-reply-chance", 0.85D);
        setDefault("chat.conversation-reply-chance", 0.62D);
        setDefault("chat.conversation-lease-seconds", 90);
        setDefault("chat.max-response-characters", 120);
        setDefault("chat.message-format", "{prefix}&f{name}{suffix}&7: &f{message}");
        setDefault("chat.http-connect-timeout-seconds", 4);
        setDefault("chat.typing.min-delay-ms", 1200);
        setDefault("chat.typing.max-delay-ms", 6500);
        setDefault("chat.typing.ms-per-character", 48);
        setDefault("chat.cooldowns.global-seconds", 4);
        setDefault("chat.cooldowns.per-bot-min-seconds", 15);
        setDefault("chat.cooldowns.per-bot-max-seconds", 55);
        setDefault("chat.density.max-messages-per-minute", 6);
        setDefault("chat.density.max-autonomous-per-minute", 2);
        setDefault("chat.llm.fallback-to-templates", true);
        setDefault("chat.memory.enabled", true);
        setDefault("chat.memory.recent-messages", 24);
        setDefault("chat.memory.history-size", 60);
        setDefault("chat.memory.context-max-age-minutes", 10);
        setDefault("chat.autonomous.enabled", true);
        setDefault("chat.autonomous.min-delay-seconds", 45);
        setDefault("chat.autonomous.max-delay-seconds", 150);
        setDefault("chat.autonomous.quiet-after-real-chat-seconds", 20);
        setDefault("chat.autonomous.fake-to-fake-chance", 0.16D);
        setDefault("chat.openrouter.enabled", true);
        setDefault("chat.openrouter.endpoint", "https://openrouter.ai/api/v1/chat/completions");
        setDefault("chat.openrouter.api-key", "${OPENROUTER_API_KEY}");
        setDefault("chat.openrouter.api-key-file", "openrouter.key");
        setDefault("chat.openrouter.models", List.of("openrouter/free"));
        setDefault("chat.openrouter.temperature", 0.9D);
        setDefault("chat.openrouter.max-tokens", 80);
        setDefault("chat.openrouter.timeout-seconds", 12);
        setDefault("chat.openrouter.failure-backoff-seconds", 60);
        setDefault("chat.openrouter.max-requests-per-minute", 4);
        setDefault("chat.openrouter.max-requests-per-hour", 40);
        setDefault("chat.openrouter.max-requests-per-day", 45);
        setDefault("chat.openrouter.http-referer", "");
        setDefault("chat.ollama.enabled", false);
        setDefault("chat.ollama.endpoint", "http://127.0.0.1:11434/api/chat");
        setDefault("chat.ollama.model", "gemma3:4b");
        setDefault("chat.ollama.api-key", "");
        setDefault("chat.ollama.temperature", 0.9D);
        setDefault("chat.ollama.max-tokens", 80);
        setDefault("chat.ollama.max-concurrent", 2);
        setDefault("chat.ollama.timeout-seconds", 10);
        setDefault("chat.ollama.failure-backoff-seconds", 30);
    }

    private void installChatRealismDefaults() {
        // 1.7.0: prison-log-informed chat. Existing configs are migrated so the livelier
        // pacing actually takes effect instead of only existing in a newly generated config.yml.
        getConfig().set("chat.reply-chance", 0.14D);
        setDefault("chat.room-conversation-reply-chance", 0.72D);
        setDefault("chat.room-conversation-lease-seconds", 75);
        getConfig().set("chat.cooldowns.global-seconds", 3);
        getConfig().set("chat.cooldowns.per-bot-min-seconds", 8);
        getConfig().set("chat.cooldowns.per-bot-max-seconds", 30);
        getConfig().set("chat.density.max-messages-per-minute", 12);
        getConfig().set("chat.density.max-autonomous-per-minute", 8);
        getConfig().set("chat.autonomous.min-delay-seconds", 20);
        getConfig().set("chat.autonomous.max-delay-seconds", 60);
        getConfig().set("chat.autonomous.quiet-after-real-chat-seconds", 8);
        getConfig().set("chat.autonomous.fake-to-fake-chance", 0.55D);
        setDefault("chat.autonomous.fake-to-fake-message-count-min", 2);
        setDefault("chat.autonomous.fake-to-fake-message-count-max", 5);
        setDefault("chat.autonomous.follow-up-delay-min-ticks", 30L);
        setDefault("chat.autonomous.follow-up-delay-max-ticks", 100L);
    }

    private void installChatSocialDefaults() {
        // 1.7.1: conversational reciprocity. Bots remember recent questions / return-to-chat cues
        // long enough to say thanks when somebody actually answers or welcomes them back.
        setDefault("chat.social.enabled", true);
        setDefault("chat.social.response-window-seconds", 32);
        setDefault("chat.social.implicit-response-window-seconds", 18);
        setDefault("chat.social.reply-delay-min-ticks", 18L);
        setDefault("chat.social.reply-delay-max-ticks", 48L);
        setDefault("chat.autonomous.recent-template-memory", 8);
    }

    private void installChatContextAndDisplayDefaults() {
        // 1.6.3: richer context + safer untrusted-chat handling and server-style prefixes.
        getConfig().set("chat.memory.recent-messages", Math.max(24, getConfig().getInt("chat.memory.recent-messages", 12)));
        getConfig().set("chat.memory.history-size", Math.max(60, getConfig().getInt("chat.memory.history-size", 30)));
        setDefault("chat.memory.context-max-age-minutes", 10);
        setDefault("chat.display.use-vault-prefix", true);
        setDefault("chat.display.vault-world", "");
        setDefault("chat.display.fallback-prefix", "");
        setDefault("chat.display.fallback-suffix", "");
        setDefault("chat.display.format", "{prefix}&f{name}{suffix}&7: &f{message}");
        // Keep old key for compatibility, but the display.format key is authoritative from 1.6.3 onward.
        setDefault("chat.message-format", "{prefix}&f{name}{suffix}&7: &f{message}");
    }

    private void installChatCaptureDefaults() {
        // 1.6.4: custom chat plugins often cancel AsyncChatEvent after taking over rendering.
        // We observe those cancelled events so the LLM transcript still sees visible chat.
        setDefault("chat.capture.cancelled-events", true);
        setDefault("chat.capture.dedupe-window-ms", 750L);
    }

    private void installCollisionAndPopulationDefaults() {
        setDefault("collision-protection.enabled", true);
        setDefault("collision-protection.import-existing-server-history", true);
        setDefault("collision-protection.exclude-real-player-history", true);
        setDefault("collision-protection.history-excludes-manual", true);
        setDefault("collision-protection.prelogin-reservation-minutes", 1440);
        setDefault("real-player-history.names", List.of());
        setDefault("real-player-history.uuids", List.of());

        // Faster pacing plus a wandering live target makes the TAB population visibly breathe.
        getConfig().set("population.join-delay-seconds-min", 3);
        getConfig().set("population.join-delay-seconds-max", 10);
        getConfig().set("population.leave-delay-seconds-min", 4);
        getConfig().set("population.leave-delay-seconds-max", 12);
        getConfig().set("population.session-minutes-min", 10);
        getConfig().set("population.session-minutes-max", 120);
        setDefault("population.variation.enabled", true);
        setDefault("population.variation.max-deviation-players", 6);
        setDefault("population.variation.max-deviation-percent", 0.12D);
        setDefault("population.variation.max-step", 3);
        setDefault("population.variation.interval-seconds-min", 35);
        setDefault("population.variation.interval-seconds-max", 95);
        setDefault("population.churn.enabled", true);
        setDefault("population.churn.interval-seconds-min", 35);
        setDefault("population.churn.interval-seconds-max", 100);
        setDefault("population.churn.chance-percent", 82);
        setDefault("population.churn.minimum-managed-players", 4);
        setDefault("population.churn.replacement-gap-seconds-min", 5);
        setDefault("population.churn.replacement-gap-seconds-max", 14);
    }

    private void removePersistedAutomaticHistoryConflicts() {
        if (realPlayerRegistry == null || fakePlayerManager == null) return;
        List<String> remove = fakePlayerManager.all().stream()
                .filter(fake -> fake.origin() != BotOrigin.MANUAL_SINGLE)
                .filter(fake -> realPlayerRegistry.isExcludedName(fake.name()))
                .map(FakePlayer::name)
                .toList();
        for (String name : remove) fakePlayerManager.remove(name);
        if (!remove.isEmpty()) {
            getLogger().info("Removed " + remove.size()
                    + " persisted automatic spoof(s) matching genuine server-player history.");
        }
    }

    private void setDefault(String path, Object value) {
        if (!getConfig().contains(path)) getConfig().set(path, value);
    }

    private static int floorToChunk(double coordinate) { return ((int) Math.floor(coordinate)) >> 4; }
}
