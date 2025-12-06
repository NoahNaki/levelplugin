package me.nakilex.levelplugin.quests.def;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import me.nakilex.levelplugin.Main;
import me.nakilex.levelplugin.fakeblock.QuestGateManager;
import me.nakilex.levelplugin.quests.data.BeaconTargets;
import me.nakilex.levelplugin.quests.data.PlayerQuestProgress;
import me.nakilex.levelplugin.quests.data.Quest;
import me.nakilex.levelplugin.quests.data.QuestObjective;
import me.nakilex.levelplugin.quests.data.QuestObjectiveType;
import me.nakilex.levelplugin.quests.data.QuestRewardCompat;
import me.nakilex.levelplugin.quests.data.QuestCompletionScript;
import me.nakilex.levelplugin.quests.data.QuestResetScript;
import me.nakilex.levelplugin.quests.data.QuestScript;
import me.nakilex.levelplugin.quests.managers.QuestManager;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CultistCullingQuest extends Quest implements QuestScript, QuestCompletionScript, QuestResetScript {
    public static final String ID = "cultistculling";
    public static final int NPC_ID = 823;
    public static final int REQUIRED_LEVEL = 20;
    public static final String RITUAL_TARGET = "cultist_ritual";
    private static final int CONTACT_NPC_ID = 1510;
    private static final String CONTACT_TALK_TARGET = "npc" + CONTACT_NPC_ID + "_mystery";
    private static final String WORLD_NAME = "world";
    private static final String GATE_ID = "cultisthq";
    private static final double TRIGGER_RADIUS_SQ = 30 * 30;
    private static final double LEASH_RANGE = 60;
    private static final double SPAWN_RADIUS = 6;
    private static final String RITUAL_SITE_KEY_NAME = "cultist_site";
    private static final String RITUAL_OWNER_KEY_NAME = "cultist_owner";
    private static final String RITUAL_SPAWN_X = "cultist_spawn_x";
    private static final String RITUAL_SPAWN_Y = "cultist_spawn_y";
    private static final String RITUAL_SPAWN_Z = "cultist_spawn_z";
    private static final String SHADOW_SITE_KEY = "shadow_sorcerer";

    private static final List<String> INTRO_DIALOG = List.of(
            "Seras|Word is a cult has been stirring trouble well beyond these woods.",
            "Seras|You're not the same fresh-faced adventurer from before—think you can stamp out their rituals?",
            "Seras|Disrupt every circle you find, then seek out the contact waiting to debrief you."
    );

    private static final List<String> CONTACT_DIALOG = List.of(
            "???|You move quietly for someone who just ruined a handful of rituals.",
            "???|Piwan said you'd be thorough. Keep that up and the cult won't see us coming."
    );

    public static List<String> getContactDialog() {
        return CONTACT_DIALOG;
    }

    public static String getContactTalkTarget() {
        return CONTACT_TALK_TARGET;
    }

    public static int getContactNpcId() {
        return CONTACT_NPC_ID;
    }

    private static final int RITUAL_KILL_TARGET = 10;

    private static final List<RitualSite> RITUAL_SITES = List.of(
            new RitualSite(SHADOW_SITE_KEY, "cultist_acolyte", "Shadow Ritual", new Location(world(), 262.5, 73, -364.5)),
            new RitualSite("tenebris", "cultist_zealot", "Zealot Ritual", new Location(world(), 176.5, 80, -629.5)),
            new RitualSite("gravekeeper", "cultist_fanatic", "Fanatic Ritual", new Location(world(), 329.5, 73, 175.5)),
            new RitualSite("crowknight", "cultist_inquisitor", "Inquisitor Ritual", new Location(world(), -329.5, 87, 36.5)),
            new RitualSite("piglinking", "cultist_high_priest", "High Priest Ritual", new Location(world(), -1161.5, 66, -834.5))
    );

    private static CultistCullingQuest instance;
    private static boolean listenersRegistered;
    private static int proximityTaskId = -1;

    private final Map<UUID, Map<String, SiteProgress>> siteProgress = new HashMap<>();
    private final Map<UUID, ActiveRitual> mobOwners = new HashMap<>();
    private final Map<UUID, Location> mobSpawnLocations = new HashMap<>();
    private final Map<UUID, Integer> processedDeaths = new HashMap<>();
    private final NamespacedKey ritualSiteKey = new NamespacedKey(Main.getInstance(), RITUAL_SITE_KEY_NAME);
    private final NamespacedKey ritualOwnerKey = new NamespacedKey(Main.getInstance(), RITUAL_OWNER_KEY_NAME);
    private final NamespacedKey ritualSpawnXKey = new NamespacedKey(Main.getInstance(), RITUAL_SPAWN_X);
    private final NamespacedKey ritualSpawnYKey = new NamespacedKey(Main.getInstance(), RITUAL_SPAWN_Y);
    private final NamespacedKey ritualSpawnZKey = new NamespacedKey(Main.getInstance(), RITUAL_SPAWN_Z);

    public CultistCullingQuest() {
        super(
                ID,
                "Cultist Culling",
                "Halt the cult's scattered rituals and report back to a covert contact.",
                createObjectives(),
                REQUIRED_LEVEL,
                List.of(SerasQuest.ID),
                null,
                QuestRewardCompat.create(1200, 600, 0, List.of()),
                NPC_ID,
                INTRO_DIALOG,
                true
        );
        instance = this;
        ensureListeners();
    }

    private static List<QuestObjective> createObjectives() {
        return List.of(
                new QuestObjective(QuestObjectiveType.KILL, RITUAL_TARGET, RITUAL_SITES.size(), false, null,
                        "Disrupt cultist rituals"),
                new QuestObjective(QuestObjectiveType.TALK, CONTACT_TALK_TARGET, 1, false,
                        BeaconTargets.npc(CONTACT_NPC_ID),
                        "Report to the mysterious contact")
        );
    }

    public static void registerTalkTargets(QuestManager questManager) {
        questManager.registerTalkTarget(CONTACT_TALK_TARGET, "Mysterious Person", "Mysterious Person");
    }

    @Override
    public void onStart(Player player, Main plugin) {
        refreshGateState(player);
    }

    @Override
    public void onComplete(Player player, Main plugin) {
        clearTracking(player.getUniqueId());
        refreshGateState(player);
    }

    @Override
    public void onReset(Player player, Main plugin) {
        clearTracking(player.getUniqueId());
        QuestGateManager gates = plugin.getQuestGateManager();
        if (gates != null) {
            gates.closeGateInstant(player, GATE_ID);
        }
    }

    private static World world() {
        return Bukkit.getWorld(WORLD_NAME);
    }

    private static void ensureListeners() {
        if (listenersRegistered) {
            return;
        }
        listenersRegistered = true;
        Main plugin = Main.getInstance();
        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onMove(PlayerMoveEvent event) {
                if (instance != null) {
                    instance.handleProximity(event.getPlayer(), event.getTo());
                }
            }

            @EventHandler
            public void onDeath(EntityDeathEvent event) {
                if (instance != null) {
                    instance.handleDeath(event.getEntity());
                }
            }

            @EventHandler
            public void onMythicDeath(MythicMobDeathEvent event) {
                if (instance != null && event.getEntity() instanceof LivingEntity living) {
                    instance.handleDeath(living);
                }
            }

            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                if (instance != null) {
                    instance.refreshGateState(event.getPlayer());
                }
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                if (instance != null) {
                    instance.clearTracking(event.getPlayer().getUniqueId());
                }
            }
        }, plugin);

        if (proximityTaskId == -1) {
            proximityTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (instance == null) {
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    instance.handleProximity(player, player.getLocation());
                }
                instance.enforceLeash();
            }, 40L, 40L).getTaskId();
        }
    }

    private void handleProximity(Player player, Location to) {
        if (player == null || to == null || to.getWorld() == null) {
            return;
        }
        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null) {
            return;
        }
        PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
        if (progress == null) {
            return;
        }
        if (progress.getProgress(0) >= getObjectives().get(0).getAmount()) {
            prepareContact(player);
            refreshGateState(player);
            return;
        }

        for (RitualSite site : RITUAL_SITES) {
            if (questManager.hasFlag(player.getUniqueId(), ID, site.key())) {
                continue;
            }
            if (!site.withinRange(to)) {
                continue;
            }
            if (isActive(player.getUniqueId(), site.key())) {
                pruneIfInvalid(player.getUniqueId(), site.key());
                continue;
            }
            spawnRitual(player, site);
        }
    }

    private void handleDeath(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        if (!markProcessed(entity.getUniqueId())) {
            return;
        }

        ActiveRitual ritual = resolveRitual(entity);
        if (ritual == null) {
            return;
        }
        clearActive(ritual.playerId(), ritual.siteKey(), entity.getUniqueId());
        Player player = Bukkit.getPlayer(ritual.playerId());
        if (player == null || !player.isOnline()) {
            return;
        }
        if (entity.getKiller() == null || !player.getUniqueId().equals(entity.getKiller().getUniqueId())) {
            return;
        }

        QuestManager questManager = Main.getInstance().getQuestManager();
        if (questManager == null) {
            return;
        }
        SiteProgress progress = getSiteProgress(player.getUniqueId(), ritual.siteKey());
        if (progress == null) {
            return;
        }

        progress.incrementKills();
        if (progress.kills() < RITUAL_KILL_TARGET) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS,
                    "Cultist ritual weakened. " + (RITUAL_KILL_TARGET - progress.kills()) + " cultists remain.");
            return;
        }

        if (questManager.hasFlag(player.getUniqueId(), ID, ritual.siteKey())) {
            return;
        }

        questManager.setFlag(player.getUniqueId(), ID, ritual.siteKey());
        questManager.handleKill(player, RITUAL_TARGET, true);
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.SUCCESS, "Cultist Ritual Halted.");

        if (ritualsCleared(questManager, player.getUniqueId())) {
            ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                    "A mysterious figure awaits your report.");
            prepareContact(player);
            refreshGateState(player);
        }
    }

    private void spawnRitual(Player player, RitualSite site) {
        Location loc = site.location();
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        int remaining = Math.max(0, RITUAL_KILL_TARGET - getKills(player.getUniqueId(), site.key()));
        if (remaining <= 0) {
            return;
        }

        int spawnCount = remaining;
        SiteProgress progress = siteProgress
                .computeIfAbsent(player.getUniqueId(), k -> new HashMap<>())
                .computeIfAbsent(site.key(), k -> new SiteProgress());

        for (int i = 0; i < spawnCount; i++) {
            Location spawnLoc = site.randomizedLocation();
            Entity mob;
            try {
                mob = MythicBukkit.inst().getAPIHelper().spawnMythicMob(site.mobId(), spawnLoc);
            } catch (InvalidMobTypeException ex) {
                Main.getInstance().getLogger().warning("Unable to spawn ritual mob '" + site.mobId() + "': " + ex.getMessage());
                return;
            }
            if (!(mob instanceof LivingEntity living)) {
                continue;
            }
            UUID playerId = player.getUniqueId();
            UUID mobId = living.getUniqueId();
            progress.activeMobIds().add(mobId);
            mobOwners.put(mobId, new ActiveRitual(playerId, site.key()));
            PersistentDataContainer data = living.getPersistentDataContainer();
            data.set(ritualSiteKey, PersistentDataType.STRING, site.key());
            data.set(ritualOwnerKey, PersistentDataType.STRING, playerId.toString());
            storeSpawn(living, spawnLoc);
            mobSpawnLocations.put(mobId, spawnLoc);
        }
        ChatMessageUtil.send(player, ChatMessageUtil.MessageType.INFO,
                "Dark energy gathers nearby. The cult begins a ritual...");
    }

    private boolean isActive(UUID playerId, String key) {
        SiteProgress progress = getSiteProgress(playerId, key);
        return progress != null && !progress.activeMobIds().isEmpty();
    }

    private void pruneIfInvalid(UUID playerId, String key) {
        SiteProgress progress = getSiteProgress(playerId, key);
        if (progress == null) {
            return;
        }
        progress.activeMobIds().removeIf(id -> {
            Entity mob = Bukkit.getEntity(id);
            return mob == null || mob.isDead();
        });
    }

    private void clearActive(UUID playerId, String key, UUID mobIdToRemove) {
        Map<String, SiteProgress> map = siteProgress.get(playerId);
        if (map != null) {
            SiteProgress progress = map.get(key);
            if (progress != null) {
                if (mobIdToRemove != null) {
                    progress.activeMobIds().remove(mobIdToRemove);
                }
                if (mobIdToRemove != null) {
                    mobOwners.remove(mobIdToRemove);
                    mobSpawnLocations.remove(mobIdToRemove);
                }
            }
        }
    }

    private void clearTracking(UUID playerId) {
        Map<String, SiteProgress> map = siteProgress.remove(playerId);
        if (map != null) {
            for (SiteProgress progress : map.values()) {
                for (UUID mobId : progress.activeMobIds()) {
                    mobOwners.remove(mobId);
                    mobSpawnLocations.remove(mobId);
                }
            }
        }
    }

    private ActiveRitual extractRitualData(LivingEntity entity) {
        PersistentDataContainer data = entity.getPersistentDataContainer();
        String siteKey = data.get(ritualSiteKey, PersistentDataType.STRING);
        String ownerId = data.get(ritualOwnerKey, PersistentDataType.STRING);
        if (siteKey == null || ownerId == null) {
            return null;
        }
        try {
            UUID playerId = UUID.fromString(ownerId);
            return new ActiveRitual(playerId, siteKey);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean ritualsCleared(QuestManager questManager, UUID playerId) {
        for (RitualSite site : RITUAL_SITES) {
            if (!questManager.hasFlag(playerId, ID, site.key())) {
                return false;
            }
        }
        return true;
    }

    private void prepareContact(Player player) {
        if (player == null) {
            return;
        }
        NPC npc = CitizensAPI.getNPCRegistry().getById(CONTACT_NPC_ID);
        if (npc == null || npc.getEntity() == null) {
            return;
        }
        Entity entity = npc.getEntity();
        if (entity instanceof LivingEntity living) {
            living.setCustomNameVisible(false);
            living.customName(Component.text("Mysterious Person", NamedTextColor.DARK_PURPLE));
        }
        npc.spawn(npc.getStoredLocation());
    }

    private void refreshGateState(Player player) {
        QuestGateManager gates = Main.getInstance().getQuestGateManager();
        if (gates == null || player == null) {
            return;
        }
        QuestManager questManager = Main.getInstance().getQuestManager();
        boolean unlocked = false;
        if (questManager != null) {
            PlayerQuestProgress progress = questManager.getProgress(player.getUniqueId(), ID);
            unlocked = questManager.hasCompleted(player.getUniqueId(), ID)
                    || (progress != null && progress.getProgress(0) >= getObjectives().get(0).getAmount());
        }
        if (unlocked) {
            gates.openGateInstant(player, GATE_ID);
            QuestManager qm = Main.getInstance().getQuestManager();
            if (qm != null && !qm.hasCompleted(player.getUniqueId(), ID)) {
                prepareContact(player);
            }
        } else {
            gates.closeGateInstant(player, GATE_ID);
        }
    }

    public record RitualStatus(String title, int remaining, int target) {}

    private record RitualSite(String key, String mobId, String title, Location location) {
        boolean withinRange(Location loc) {
            if (loc == null || location == null) {
                return false;
            }
            World siteWorld = location.getWorld();
            World playerWorld = loc.getWorld();
            if (siteWorld == null || playerWorld == null) {
                return false;
            }
            if (!siteWorld.equals(playerWorld)) {
                return false;
            }
            return location.distanceSquared(loc) <= TRIGGER_RADIUS_SQ;
        }

        Location randomizedLocation() {
            if (location == null) {
                return null;
            }
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            double radius = Math.sqrt(ThreadLocalRandom.current().nextDouble()) * SPAWN_RADIUS;
            double xOffset = Math.cos(angle) * radius;
            double zOffset = Math.sin(angle) * radius;
            return location.clone().add(xOffset, 0, zOffset);
        }
    }

    private record ActiveRitual(UUID playerId, String siteKey) {
    }

    private ActiveRitual resolveRitual(LivingEntity entity) {
        ActiveRitual ritual = mobOwners.remove(entity.getUniqueId());
        if (ritual == null) {
            ritual = extractRitualData(entity);
        }
        return ritual;
    }

    private SiteProgress getSiteProgress(UUID playerId, String key) {
        Map<String, SiteProgress> map = siteProgress.get(playerId);
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    private int getKills(UUID playerId, String key) {
        SiteProgress progress = getSiteProgress(playerId, key);
        return progress == null ? 0 : progress.kills();
    }

    public static RitualStatus getRitualStatus(Player player) {
        if (instance == null || player == null) {
            return null;
        }
        QuestManager qm = Main.getInstance().getQuestManager();
        if (qm == null) {
            return null;
        }
        PlayerQuestProgress progress = qm.getProgress(player.getUniqueId(), ID);
        if (progress == null) {
            return null;
        }

        Location loc = player.getLocation();
        for (RitualSite site : RITUAL_SITES) {
            if (!site.withinRange(loc)) {
                continue;
            }
            if (qm.hasFlag(player.getUniqueId(), ID, site.key())) {
                continue;
            }
            int remaining = Math.max(0, RITUAL_KILL_TARGET - instance.getKills(player.getUniqueId(), site.key()));
            return new RitualStatus(site.title(), remaining, RITUAL_KILL_TARGET);
        }
        return null;
    }

    private boolean markProcessed(UUID mobId) {
        if (processedDeaths.containsKey(mobId)) {
            return false;
        }

        int taskId = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            processedDeaths.remove(mobId);
        }, 200L).getTaskId();

        processedDeaths.put(mobId, taskId);
        return true;
    }

    private void storeSpawn(LivingEntity living, Location spawnLoc) {
        if (living == null || spawnLoc == null) {
            return;
        }
        PersistentDataContainer data = living.getPersistentDataContainer();
        data.set(ritualSpawnXKey, PersistentDataType.DOUBLE, spawnLoc.getX());
        data.set(ritualSpawnYKey, PersistentDataType.DOUBLE, spawnLoc.getY());
        data.set(ritualSpawnZKey, PersistentDataType.DOUBLE, spawnLoc.getZ());
    }

    private Location resolveSpawn(LivingEntity living) {
        Location cached = mobSpawnLocations.get(living.getUniqueId());
        if (cached != null) {
            return cached;
        }
        PersistentDataContainer data = living.getPersistentDataContainer();
        Double x = data.get(ritualSpawnXKey, PersistentDataType.DOUBLE);
        Double y = data.get(ritualSpawnYKey, PersistentDataType.DOUBLE);
        Double z = data.get(ritualSpawnZKey, PersistentDataType.DOUBLE);
        if (x == null || y == null || z == null) {
            return null;
        }
        Location loc = living.getLocation().clone();
        loc.setX(x);
        loc.setY(y);
        loc.setZ(z);
        mobSpawnLocations.put(living.getUniqueId(), loc);
        return loc;
    }

    private void enforceLeash() {
        mobSpawnLocations.entrySet().removeIf(entry -> {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || entity.isDead()) {
                mobOwners.remove(entry.getKey());
                return true;
            }
            Location origin = entry.getValue();
            if (origin == null) {
                origin = resolveSpawn(living);
                if (origin == null) {
                    origin = living.getLocation();
                    mobSpawnLocations.put(living.getUniqueId(), origin);
                    storeSpawn(living, origin);
                }
            }
            if (origin == null || origin.getWorld() == null || living.getWorld() == null) {
                return false;
            }
            if (!origin.getWorld().equals(living.getWorld())) {
                living.teleport(origin);
                return false;
            }
            if (origin.distanceSquared(living.getLocation()) > LEASH_RANGE * LEASH_RANGE) {
                living.teleport(origin);
            }
            return false;
        });
    }

    private static final class SiteProgress {
        private int kills;
        private final java.util.Set<UUID> activeMobIds = new java.util.HashSet<>();

        int kills() {
            return kills;
        }

        void incrementKills() {
            kills++;
        }

        java.util.Set<UUID> activeMobIds() {
            return activeMobIds;
        }
    }
}
