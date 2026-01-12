package me.nakilex.npc.plugin.service;

import me.nakilex.npc.core.model.Npc;
import me.nakilex.npc.core.nms.NmsBridge;
import me.nakilex.npc.core.nms.SkinService;
import me.nakilex.npc.core.persistence.NpcRepository;
import me.nakilex.npc.core.persistence.YamlNpcRepository;
import me.nakilex.npc.core.registry.DefaultNpcRegistry;
import me.nakilex.npc.core.registry.NpcRegistry;
import me.nakilex.npc.core.trait.TraitRegistry;
import me.nakilex.npc.nms.v1_21_3.NmsBridgeV1_21_3;
import me.nakilex.npc.nms.v1_21_3.skin.DefaultSkinService;
import me.nakilex.npc.nms.v1_21_3.skin.HttpSkinResolver;
import me.nakilex.npc.nms.v1_21_3.skin.SkinResolver;
import me.nakilex.npc.plugin.integration.IntegrationManager;
import me.nakilex.npc.plugin.trait.CommandListTrait;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;

public class NpcService {
    private final Plugin plugin;
    private final DefaultNpcRegistry registry = new DefaultNpcRegistry();
    private final NpcRepository repository = new YamlNpcRepository();
    private final TraitRegistry traitRegistry = new TraitRegistry();
    private final NmsBridge nmsBridge;
    private final SkinService skinService;
    private final DefaultNpcLifecycleService lifecycleService;
    private final IntegrationManager integrationManager;
    private File dataFile;
    private int autosaveTask = -1;
    private int traitTickTask = -1;

    public NpcService(Plugin plugin) {
        this.plugin = plugin;
        this.nmsBridge = new NmsBridgeV1_21_3(plugin);
        FileConfiguration config = plugin.getConfig();
        SkinResolver resolver = buildResolver(config);
        this.skinService = new DefaultSkinService(plugin, resolver, Duration.ofMinutes(30));
        this.lifecycleService = new DefaultNpcLifecycleService(plugin, registry, nmsBridge, traitRegistry);
        this.integrationManager = new IntegrationManager(this);
    }

    public void start() {
        dataFile = new File(plugin.getDataFolder(), "npcs.yml");
        repository.load(registry, dataFile);
        traitRegistry.register(new CommandListTrait());
        applyTraitLoad();
        lifecycleService.spawnAll();
        scheduleAutosave();
        scheduleTraitTicks();
        integrationManager.register();
    }

    public void stop() {
        if (autosaveTask != -1) {
            Bukkit.getScheduler().cancelTask(autosaveTask);
        }
        if (traitTickTask != -1) {
            Bukkit.getScheduler().cancelTask(traitTickTask);
        }
        lifecycleService.despawnAll();
        applyTraitSave();
        createBackup();
        repository.save(registry, dataFile);
    }

    private void scheduleAutosave() {
        long interval = plugin.getConfig().getLong("autosaveIntervalTicks", 20L * 60L * 5L);
        autosaveTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::saveNow, interval, interval);
    }

    private void scheduleTraitTicks() {
        long interval = plugin.getConfig().getLong("traitTickIntervalTicks", 20L);
        traitTickTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, lifecycleService::tickTraits, interval, interval);
    }

    private SkinResolver buildResolver(FileConfiguration config) {
        String endpoint = config.getString("skinResolverEndpoint");
        if (endpoint == null || endpoint.isBlank()) {
            return null;
        }
        Duration timeout = Duration.ofSeconds(config.getLong("skinResolverTimeoutSeconds", 8));
        return new HttpSkinResolver(URI.create(endpoint), timeout);
    }

    public NpcRegistry getRegistry() {
        return registry;
    }

    public DefaultNpcRegistry getInternalRegistry() {
        return registry;
    }

    public DefaultNpcLifecycleService getLifecycle() {
        return lifecycleService;
    }

    public TraitRegistry getTraitRegistry() {
        return traitRegistry;
    }

    public NmsBridge getNmsBridge() {
        return nmsBridge;
    }

    public SkinService getSkinService() {
        return skinService;
    }

    public void saveNow() {
        applyTraitSave();
        createBackup();
        repository.save(registry, dataFile);
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Optional<Npc> findByEntityId(int entityId) {
        return registry.list().stream()
                .filter(npc -> npc.getEntityId() != null && npc.getEntityId() == entityId)
                .findFirst();
    }

    public Optional<Npc> findByEntityUuid(java.util.UUID uuid) {
        return registry.list().stream()
                .filter(npc -> uuid.equals(npc.getUuid()))
                .findFirst();
    }

    public void updateNpcPosition(Npc npc, Location location) {
        if (location == null || location.getWorld() == null) {
            return;
        }
        npc.setPosition(me.nakilex.npc.core.model.NpcPosition.fromLocation(location));
    }

    public void exportData(File file) {
        repository.exportData(registry, file);
    }

    public void importData(File file) {
        repository.importData(registry, file);
    }

    private void applyTraitLoad() {
        for (Npc npc : registry.list()) {
            traitRegistry.list().forEach(trait -> trait.onLoad(npc, npc.getTraitData(trait.getId())));
        }
    }

    private void applyTraitSave() {
        for (Npc npc : registry.list()) {
            traitRegistry.list().forEach(trait -> {
                var data = trait.onSave(npc);
                if (data != null) {
                    npc.setTraitData(trait.getId(), data);
                }
            });
        }
    }

    private void createBackup() {
        boolean enabled = plugin.getConfig().getBoolean("backupOnSave", true);
        if (!enabled || dataFile == null || !dataFile.exists()) {
            return;
        }
        File backupDir = new File(plugin.getDataFolder(), "backups");
        if (!backupDir.exists() && !backupDir.mkdirs()) {
            return;
        }
        String name = "npcs-" + System.currentTimeMillis() + ".yml";
        File backup = new File(backupDir, name);
        try {
            java.nio.file.Files.copy(dataFile.toPath(), backup.toPath());
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to create backup: " + ex.getMessage());
        }
        int maxBackups = plugin.getConfig().getInt("maxBackups", 10);
        File[] files = backupDir.listFiles((dir, filename) -> filename.startsWith("npcs-") && filename.endsWith(".yml"));
        if (files != null && files.length > maxBackups) {
            java.util.Arrays.sort(files, java.util.Comparator.comparingLong(File::lastModified).reversed());
            for (int i = maxBackups; i < files.length; i++) {
                files[i].delete();
            }
        }
    }

    public void applySkin(Npc npc) {
        if (npc == null || npc.getSkinRef() == null) {
            return;
        }
        skinService.resolveSkin(npc.getSkinRef())
                .thenAcceptAsync(skinData -> Bukkit.getScheduler().runTask(plugin, () -> nmsBridge.applySkin(npc, skinData)),
                        Bukkit.getScheduler().getAsyncExecutor(plugin))
                .exceptionally(ex -> {
                    plugin.getLogger().warning(\"Failed to resolve skin for NPC \" + npc.getId() + \": \" + ex.getMessage());
                    return null;
                });
    }
}
