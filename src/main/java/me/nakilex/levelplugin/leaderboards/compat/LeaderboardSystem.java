package me.nakilex.levelplugin.leaderboards.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.clip.placeholderapi.PlaceholderAPI;
import me.nakilex.levelplugin.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Persistent, PlaceholderAPI-backed leaderboard engine. It intentionally exposes an ajLeaderboards
 * compatible view so existing menus and holograms do not need to be rewritten during migration.
 */
public final class LeaderboardSystem implements Listener {
    private static final Pattern NUMBER = Pattern.compile("[-+]?[0-9][0-9,]*(?:\\.[0-9]+)?(?:[eE][-+]?[0-9]+)?");
    private static final String[] SHORT_SUFFIXES = {"", "k", "m", "b", "t", "q", "qi", "sx", "sp", "o", "n", "d", "ud"};
    private static final Pattern DURATION_PART = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*([wdhms])", Pattern.CASE_INSENSITIVE);

    private final Main plugin;
    private final File settingsFile;
    private final File dataFile;
    private final File importFolder;
    private final Map<String, LeaderboardDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, Map<UUID, ScoreRecord>> scores = new HashMap<>();
    private final Map<UUID, Map<String, BigDecimal>> aggregateStatisticTotals = new HashMap<>();
    private final AtomicBoolean saveQueued = new AtomicBoolean();
    private FileConfiguration settings;
    private ZoneId zoneId;
    private DayOfWeek weekStart;
    private String noDataName;
    private String noDataValue;
    private long refreshTicks;
    private BukkitTask refreshTask;
    private BukkitTask saveTask;

    public LeaderboardSystem(Main plugin) {
        this.plugin = plugin;
        this.settingsFile = new File(plugin.getDataFolder(), "leaderboard-system.yml");
        this.dataFile = new File(plugin.getDataFolder(), "leaderboards-data.yml");
        this.importFolder = new File(plugin.getDataFolder(), "leaderboard-import");
        if (!settingsFile.exists()) plugin.saveResource("leaderboard-system.yml", false);
        reloadSettings();
        loadData();
        importLegacyCsvFiles();
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        restartTasks();
        for (Player player : Bukkit.getOnlinePlayers()) {
            initializeAggregateStatistics(player);
            updatePlayer(player);
        }
    }

    public void close() {
        if (refreshTask != null) refreshTask.cancel();
        if (saveTask != null) saveTask.cancel();
        saveNow();
    }

    public void reload() {
        reloadSettings();
        restartTasks();
    }

    public int updateAllOnline() {
        int updated = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
            updated++;
        }
        return updated;
    }

    public void updatePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        long now = System.currentTimeMillis();
        String prefix = parseText(player, "%vault_prefix%");
        String suffix = parseText(player, "%vault_suffix%");
        for (LeaderboardDefinition definition : definitions.values()) {
            BigDecimal value = resolveNativeStatistic(player, definition);
            if (value == null) {
                String resolved = parseText(player, definition.placeholder());
                value = definition.valueType() == LeaderboardDefinition.ValueType.TIME_SECONDS
                        ? parseDuration(resolved) : parseNumber(resolved);
            }
            if (value == null) continue;
            value = value.multiply(definition.scale());
            updateScore(definition.id(), player.getUniqueId(), player.getName(), player.getDisplayName(),
                    prefix, suffix, value, now);
        }
        queueSave();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        initializeAggregateStatistics(event.getPlayer());
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayer(event.getPlayer()), 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        updatePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStatisticIncrement(PlayerStatisticIncrementEvent event) {
        String boardId = switch (event.getStatistic()) {
            case CRAFT_ITEM -> "statistic_craft_item";
            case MINE_BLOCK -> "statistic_mine_block";
            default -> null;
        };
        if (boardId == null) return;
        initializeAggregateStatistics(event.getPlayer());
        int increment = Math.max(0, event.getNewValue() - event.getPreviousValue());
        aggregateStatisticTotals.get(event.getPlayer().getUniqueId())
                .merge(boardId, BigDecimal.valueOf(increment), BigDecimal::add);
    }

    public LeaderboardDefinition getDefinition(String id) {
        return definitions.get(normalizeId(id));
    }

    public List<RankedScore> getTop(String boardId, LeaderboardPeriod period, int limit) {
        LeaderboardDefinition definition = getDefinition(boardId);
        if (definition == null || limit <= 0) return List.of();
        Map<UUID, ScoreRecord> boardScores = scores.get(definition.id());
        if (boardScores == null) return List.of();
        Comparator<ScoreRecord> byValue = Comparator.comparing((ScoreRecord score) -> score.value(period));
        if (!definition.reverseSort()) byValue = byValue.reversed();
        Comparator<ScoreRecord> comparator = byValue.thenComparing(score -> score.name.toLowerCase(Locale.ROOT));
        List<ScoreRecord> sorted = boardScores.values().stream()
                .filter(score -> !definition.excludeZero() || score.value(period).compareTo(BigDecimal.ZERO) != 0)
                .sorted(comparator)
                .limit(limit)
                .toList();
        List<RankedScore> result = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) result.add(new RankedScore(i + 1, sorted.get(i)));
        return result;
    }

    public RankedScore getRank(String boardId, LeaderboardPeriod period, UUID playerId) {
        if (playerId == null) return null;
        List<RankedScore> all = getTop(boardId, period, Integer.MAX_VALUE);
        for (RankedScore ranked : all) {
            if (ranked.score.playerId.equals(playerId)) return ranked;
        }
        return null;
    }

    public String formatValue(String boardId, BigDecimal value, boolean abbreviated) {
        if (value == null) return noDataValue;
        LeaderboardDefinition definition = getDefinition(boardId);
        if (definition != null && definition.valueType() == LeaderboardDefinition.ValueType.TIME_SECONDS && !abbreviated) {
            return formatDuration(value.setScale(0, RoundingMode.DOWN).longValue());
        }
        if (abbreviated) return abbreviate(value);
        int scale = definition != null && definition.valueType() == LeaderboardDefinition.ValueType.INTEGER ? 0 : 2;
        BigDecimal display = value.setScale(scale, RoundingMode.DOWN).stripTrailingZeros();
        java.text.DecimalFormat format = new java.text.DecimalFormat("#,##0.################");
        return format.format(display);
    }

    public String noDataName() { return noDataName; }
    public String noDataValue() { return noDataValue; }
    public int boardCount() { return definitions.size(); }
    public int scoreCount() { return scores.values().stream().mapToInt(Map::size).sum(); }
    public void flush() { saveNow(); }

    private String parseText(Player player, String placeholder) {
        try {
            return PlaceholderAPI.setPlaceholders(player, placeholder == null ? "" : placeholder);
        } catch (RuntimeException ex) {
            plugin.getLogger().fine("Could not resolve leaderboard placeholder " + placeholder + ": " + ex.getMessage());
            return "";
        }
    }

    /**
     * Reads simple Bukkit statistics directly and uses a small event-backed cache for the two
     * aggregate material statistics. PlaceholderAPI's aggregate implementation walks every
     * Material and initializes CraftLegacy on the server thread, which can freeze a 1.21 server.
     */
    private BigDecimal resolveNativeStatistic(Player player, LeaderboardDefinition definition) {
        try {
            return switch (definition.id()) {
                case "statistic_animals_bred" -> statistic(player, Statistic.ANIMALS_BRED);
                case "statistic_chest_opened" -> statistic(player, Statistic.CHEST_OPENED);
                case "statistic_craft_item", "statistic_mine_block" -> {
                    initializeAggregateStatistics(player);
                    yield aggregateStatisticTotals.get(player.getUniqueId())
                            .getOrDefault(definition.id(), BigDecimal.ZERO);
                }
                case "statistic_damage_dealt" -> statistic(player, Statistic.DAMAGE_DEALT);
                case "statistic_deaths" -> statistic(player, Statistic.DEATHS);
                case "statistic_fall_one_cm" -> statistic(player, Statistic.FALL_ONE_CM);
                case "statistic_jump" -> statistic(player, Statistic.JUMP);
                case "statistic_mob_kills" -> statistic(player, Statistic.MOB_KILLS);
                case "statistic_player_kills" -> statistic(player, Statistic.PLAYER_KILLS);
                case "statistic_sleep_in_bed" -> statistic(player, Statistic.SLEEP_IN_BED);
                case "statistic_time_played" -> ticksToSeconds(player.getStatistic(Statistic.PLAY_ONE_MINUTE));
                case "statistic_time_since_death" -> ticksToSeconds(player.getStatistic(Statistic.TIME_SINCE_DEATH));
                default -> null;
            };
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().fine("Could not read native statistic " + definition.id() + ": " + ex.getMessage());
            return null;
        }
    }

    private static BigDecimal statistic(Player player, Statistic statistic) {
        return BigDecimal.valueOf(player.getStatistic(statistic));
    }

    private static BigDecimal ticksToSeconds(int ticks) {
        return BigDecimal.valueOf(ticks).divide(BigDecimal.valueOf(20), 2, RoundingMode.DOWN);
    }

    private void initializeAggregateStatistics(Player player) {
        aggregateStatisticTotals.computeIfAbsent(player.getUniqueId(), ignored -> {
            Map<String, BigDecimal> totals = new HashMap<>();
            totals.put("statistic_craft_item", BigDecimal.ZERO);
            totals.put("statistic_mine_block", BigDecimal.ZERO);
            if (Bukkit.getWorlds().isEmpty()) return totals;
            File statsFile = new File(new File(Bukkit.getWorlds().get(0).getWorldFolder(), "stats"),
                    player.getUniqueId() + ".json");
            if (!statsFile.isFile()) return totals;
            try (Reader reader = Files.newBufferedReader(statsFile.toPath(), StandardCharsets.UTF_8)) {
                JsonElement rootElement = JsonParser.parseReader(reader);
                if (!rootElement.isJsonObject()) return totals;
                JsonObject stats = rootElement.getAsJsonObject().getAsJsonObject("stats");
                if (stats == null) return totals;
                totals.put("statistic_craft_item", sumStatisticCategory(stats, "minecraft:crafted"));
                totals.put("statistic_mine_block", sumStatisticCategory(stats, "minecraft:mined"));
            } catch (IOException | RuntimeException ex) {
                plugin.getLogger().warning("Could not seed aggregate statistics for " + player.getName()
                        + ": " + ex.getMessage());
            }
            return totals;
        });
    }

    private static BigDecimal sumStatisticCategory(JsonObject stats, String category) {
        JsonObject values = stats.getAsJsonObject(category);
        if (values == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                total = total.add(entry.getValue().getAsBigDecimal());
            }
        }
        return total;
    }

    private void updateScore(String boardId, UUID uuid, String name, String displayName,
                             String prefix, String suffix, BigDecimal sample, long now) {
        ScoreRecord record = scores.computeIfAbsent(boardId, ignored -> new HashMap<>())
                .computeIfAbsent(uuid, ignored -> new ScoreRecord(uuid));
        record.name = name == null ? uuid.toString() : name;
        record.displayName = displayName == null ? record.name : displayName;
        record.prefix = prefix == null ? "" : prefix;
        record.suffix = suffix == null ? "" : suffix;
        record.total = sample;
        for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
            if (period == LeaderboardPeriod.ALLTIME) continue;
            long bucket = period.bucketStart(now, zoneId, weekStart);
            Long oldBucket = record.bucketStarts.get(period);
            BigDecimal last = record.lastTotals.get(period);
            if (oldBucket == null || oldBucket != bucket || last == null) {
                record.periodValues.put(period, BigDecimal.ZERO);
                record.lastTotals.put(period, sample);
                record.bucketStarts.put(period, bucket);
            } else {
                record.periodValues.merge(period, sample.subtract(last), BigDecimal::add);
                record.lastTotals.put(period, sample);
            }
        }
    }

    private void reloadSettings() {
        settings = YamlConfiguration.loadConfiguration(settingsFile);
        definitions.clear();
        refreshTicks = Math.max(20L, settings.getLong("refresh-ticks", 1200L));
        noDataName = settings.getString("no-data.name", "---");
        noDataValue = settings.getString("no-data.value", "---");
        try {
            zoneId = ZoneId.of(settings.getString("resets.time-zone", ZoneId.systemDefault().getId()));
        } catch (RuntimeException ex) {
            zoneId = ZoneId.systemDefault();
        }
        try {
            weekStart = DayOfWeek.valueOf(settings.getString("resets.week-start", "SUNDAY").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            weekStart = DayOfWeek.SUNDAY;
        }
        ConfigurationSection section = settings.getConfigurationSection("boards");
        if (section == null) return;
        for (String rawId : section.getKeys(false)) {
            String id = normalizeId(rawId);
            String base = "boards." + rawId + ".";
            String placeholder = settings.getString(base + "placeholder", "%" + id + "%");
            LeaderboardDefinition.ValueType type;
            try {
                type = LeaderboardDefinition.ValueType.valueOf(settings.getString(base + "value-type", "NUMBER").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                type = LeaderboardDefinition.ValueType.NUMBER;
            }
            BigDecimal scale = parseNumber(String.valueOf(settings.getDouble(base + "scale", 1.0D)));
            definitions.put(id, new LeaderboardDefinition(id, placeholder, type,
                    settings.getBoolean(base + "reverse-sort", false),
                    settings.getBoolean(base + "exclude-zero", false),
                    scale == null ? BigDecimal.ONE : scale));
        }
    }

    private void restartTasks() {
        if (refreshTask != null) refreshTask.cancel();
        if (saveTask != null) saveTask.cancel();
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllOnline, refreshTicks, refreshTicks);
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::saveSnapshot,
                Math.max(200L, refreshTicks), Math.max(200L, refreshTicks));
    }

    private void loadData() {
        if (!dataFile.exists()) return;
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection boards = data.getConfigurationSection("boards");
        if (boards == null) return;
        for (String boardId : boards.getKeys(false)) {
            ConfigurationSection players = boards.getConfigurationSection(boardId + ".players");
            if (players == null) continue;
            Map<UUID, ScoreRecord> board = scores.computeIfAbsent(normalizeId(boardId), ignored -> new HashMap<>());
            for (String uuidText : players.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidText);
                    String base = uuidText + ".";
                    ScoreRecord record = new ScoreRecord(uuid);
                    record.name = players.getString(base + "name", uuidText);
                    record.displayName = players.getString(base + "display-name", record.name);
                    record.prefix = players.getString(base + "prefix", "");
                    record.suffix = players.getString(base + "suffix", "");
                    record.total = decimal(players.getString(base + "total", "0"));
                    for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                        if (period == LeaderboardPeriod.ALLTIME) continue;
                        String periodBase = base + "periods." + period.name().toLowerCase(Locale.ROOT) + ".";
                        record.periodValues.put(period, decimal(players.getString(periodBase + "value", "0")));
                        record.lastTotals.put(period, decimal(players.getString(periodBase + "last-total", record.total.toPlainString())));
                        record.bucketStarts.put(period, players.getLong(periodBase + "bucket-start", 0L));
                    }
                    board.put(uuid, record);
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Skipping malformed leaderboard UUID " + uuidText);
                }
            }
        }
    }

    private void importLegacyCsvFiles() {
        File[] files = importFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".csv"));
        if (files == null || files.length == 0) return;
        int imported = 0;
        for (File csv : files) {
            String boardId = normalizeId(csv.getName().substring(0, csv.getName().length() - 4));
            if (!definitions.containsKey(boardId)) continue;
            try {
                List<String> lines = Files.readAllLines(csv.toPath(), StandardCharsets.UTF_8);
                if (lines.isEmpty()) continue;
                List<String> header = parseCsvLine(lines.get(0));
                Map<String, Integer> columns = new HashMap<>();
                for (int i = 0; i < header.size(); i++) columns.put(header.get(i).toLowerCase(Locale.ROOT), i);
                for (int line = 1; line < lines.size(); line++) {
                    List<String> row = parseCsvLine(lines.get(line));
                    String id = csvValue(row, columns, "id");
                    if (id.isBlank()) continue;
                    UUID uuid;
                    try { uuid = UUID.fromString(id); } catch (IllegalArgumentException ignored) { continue; }
                    Map<UUID, ScoreRecord> board = scores.computeIfAbsent(boardId, ignored -> new HashMap<>());
                    ScoreRecord record = board.computeIfAbsent(uuid, ScoreRecord::new);
                    record.total = decimal(csvValue(row, columns, "value"));
                    record.name = fallback(csvValue(row, columns, "namecache"), uuid.toString());
                    record.displayName = fallback(csvValue(row, columns, "displaynamecache"), record.name);
                    record.prefix = csvValue(row, columns, "prefixcache");
                    record.suffix = csvValue(row, columns, "suffixcache");
                    for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                        if (period == LeaderboardPeriod.ALLTIME) continue;
                        String key = period.name().toLowerCase(Locale.ROOT);
                        record.periodValues.put(period, decimal(csvValue(row, columns, key + "_delta")));
                        record.lastTotals.put(period, decimal(csvValue(row, columns, key + "_lasttotal")));
                        long legacyTimestamp = decimal(csvValue(row, columns, key + "_timestamp")).longValue();
                        record.bucketStarts.put(period, legacyTimestamp <= 0L ? 0L
                                : period.bucketStart(legacyTimestamp, zoneId, weekStart));
                    }
                    imported++;
                }
                Files.move(csv.toPath(), new File(csv.getParentFile(), csv.getName() + ".imported").toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                plugin.getLogger().warning("Could not import " + csv.getName() + ": " + ex.getMessage());
            }
        }
        if (imported > 0) {
            plugin.getLogger().info("Imported " + imported + " cached ajLeaderboards score rows.");
            saveNow();
        }
    }

    private void queueSave() {
        saveQueued.set(true);
    }

    private void saveSnapshot() {
        if (!saveQueued.compareAndSet(true, false)) return;
        saveNow();
    }

    private synchronized void saveNow() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("version", 1);
        for (Map.Entry<String, Map<UUID, ScoreRecord>> board : scores.entrySet()) {
            for (ScoreRecord record : board.getValue().values()) {
                String base = "boards." + board.getKey() + ".players." + record.playerId + ".";
                data.set(base + "name", record.name);
                data.set(base + "display-name", record.displayName);
                data.set(base + "prefix", record.prefix);
                data.set(base + "suffix", record.suffix);
                data.set(base + "total", record.total.toPlainString());
                for (LeaderboardPeriod period : LeaderboardPeriod.values()) {
                    if (period == LeaderboardPeriod.ALLTIME) continue;
                    String periodBase = base + "periods." + period.name().toLowerCase(Locale.ROOT) + ".";
                    data.set(periodBase + "value", record.periodValues.getOrDefault(period, BigDecimal.ZERO).toPlainString());
                    data.set(periodBase + "last-total", record.lastTotals.getOrDefault(period, record.total).toPlainString());
                    data.set(periodBase + "bucket-start", record.bucketStarts.getOrDefault(period, 0L));
                }
            }
        }
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save leaderboards-data.yml: " + ex.getMessage());
            saveQueued.set(true);
        }
    }

    private static BigDecimal parseNumber(String input) {
        if (input == null) return null;
        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', input)).trim();
        Matcher matcher = NUMBER.matcher(stripped);
        if (!matcher.find()) return null;
        String token = matcher.group().replace(",", "");
        try {
            BigDecimal value = new BigDecimal(token);
            String tail = stripped.substring(matcher.end()).trim().toLowerCase(Locale.ROOT);
            String suffix = tail.replaceAll("[^a-z]", "");
            for (int i = SHORT_SUFFIXES.length - 1; i > 0; i--) {
                if (suffix.startsWith(SHORT_SUFFIXES[i])) {
                    return value.multiply(BigDecimal.TEN.pow(i * 3));
                }
            }
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal decimal(String value) {
        BigDecimal parsed = parseNumber(value);
        return parsed == null ? BigDecimal.ZERO : parsed;
    }

    private static BigDecimal parseDuration(String input) {
        if (input == null) return null;
        String stripped = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', input))
                .toLowerCase(Locale.ROOT).replace(",", "").trim();
        Matcher matcher = DURATION_PART.matcher(stripped);
        BigDecimal seconds = BigDecimal.ZERO;
        boolean found = false;
        while (matcher.find()) {
            found = true;
            BigDecimal value = new BigDecimal(matcher.group(1));
            long multiplier = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
                case "w" -> 604800L;
                case "d" -> 86400L;
                case "h" -> 3600L;
                case "m" -> 60L;
                default -> 1L;
            };
            seconds = seconds.add(value.multiply(BigDecimal.valueOf(multiplier)));
        }
        if (found) return seconds;
        if (stripped.matches("[0-9]+:[0-9]{1,2}(?::[0-9]{1,2})?")) {
            String[] parts = stripped.split(":");
            long total = 0;
            for (String part : parts) total = total * 60L + Long.parseLong(part);
            return BigDecimal.valueOf(total);
        }
        return parseNumber(stripped);
    }

    private static String abbreviate(BigDecimal value) {
        BigDecimal absolute = value.abs();
        int group = 0;
        while (absolute.compareTo(BigDecimal.valueOf(1000)) >= 0 && group < SHORT_SUFFIXES.length - 1) {
            absolute = absolute.divide(BigDecimal.valueOf(1000), 8, RoundingMode.HALF_UP);
            group++;
        }
        if (value.signum() < 0) absolute = absolute.negate();
        return absolute.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + SHORT_SUFFIXES[group];
    }

    private static String formatDuration(long seconds) {
        long weeks = seconds / 604800; seconds %= 604800;
        long days = seconds / 86400; seconds %= 86400;
        long hours = seconds / 3600; seconds %= 3600;
        long minutes = seconds / 60; seconds %= 60;
        StringBuilder out = new StringBuilder();
        if (weeks > 0) out.append(weeks).append("w ");
        if (days > 0) out.append(days).append("d ");
        if (hours > 0) out.append(hours).append("h ");
        if (minutes > 0) out.append(minutes).append("m ");
        out.append(seconds).append('s');
        return out.toString().trim();
    }

    private static String normalizeId(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).trim();
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static String csvValue(List<String> row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        return index == null || index < 0 || index >= row.size() ? "" : row.get(index);
    }

    public record RankedScore(int rank, ScoreRecord score) {}

    public static final class ScoreRecord {
        private final UUID playerId;
        private String name = "---";
        private String displayName = "---";
        private String prefix = "";
        private String suffix = "";
        private BigDecimal total = BigDecimal.ZERO;
        private final EnumMap<LeaderboardPeriod, BigDecimal> periodValues = new EnumMap<>(LeaderboardPeriod.class);
        private final EnumMap<LeaderboardPeriod, BigDecimal> lastTotals = new EnumMap<>(LeaderboardPeriod.class);
        private final EnumMap<LeaderboardPeriod, Long> bucketStarts = new EnumMap<>(LeaderboardPeriod.class);

        private ScoreRecord(UUID playerId) { this.playerId = playerId; }
        public UUID playerId() { return playerId; }
        public String name() { return name; }
        public String displayName() { return displayName; }
        public String prefix() { return prefix; }
        public String suffix() { return suffix; }
        public BigDecimal value(LeaderboardPeriod period) {
            return period == LeaderboardPeriod.ALLTIME ? total : periodValues.getOrDefault(period, BigDecimal.ZERO);
        }
    }
}
