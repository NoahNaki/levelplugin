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
import me.nakilex.levelplugin.utils.RewardBombUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil;
import me.nakilex.levelplugin.utils.ChatMessageUtil.MessageType;
import me.nakilex.levelplugin.mob.utils.CombatPowerUtil;
import me.nakilex.levelplugin.lootchests.managers.LootChestManager;
import me.nakilex.levelplugin.items.utils.ItemUtil;
import me.nakilex.levelplugin.economy.managers.EconomyManager;
import me.nakilex.levelplugin.economy.managers.GemsManager;
import me.nakilex.levelplugin.potions.managers.PotionManager;
import me.nakilex.levelplugin.potions.data.PotionTemplate;
import me.nakilex.levelplugin.potions.data.PotionInstance;
import me.nakilex.levelplugin.mercenary.MercenaryGift;
import me.nakilex.levelplugin.mercenary.MercenaryAffinityManager;
import me.nakilex.levelplugin.player.classes.data.PlayerClass;
import me.nakilex.levelplugin.player.classes.essence.ClassEssence;
import me.nakilex.levelplugin.utils.NumberUtil;
import me.nakilex.levelplugin.utils.TooltipUtil;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.ChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;

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
    private static final double SPAWN_RADIUS = 10;
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
            createSite(SHADOW_SITE_KEY, "cultist_acolyte", 20, "Shadow Ritual", new Location(world(), 262.5, 73, -364.5)),
            createSite("tenebris", "cultist_zealot", 40, "Zealot Ritual", new Location(world(), 176.5, 80, -629.5)),
            createSite("gravekeeper", "cultist_fanatic", 60, "Fanatic Ritual", new Location(world(), 329.5, 73, 175.5)),
            createSite("crowknight", "cultist_inquisitor", 80, "Inquisitor Ritual", new Location(world(), -329.5, 87, 36.5)),
            createSite("piglinking", "cultist_high_priest", 100, "High Priest Ritual", new Location(world(), -1161.5, 66, -834.5))
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
                List.of(SerasSlimeKingQuest.ID),
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
            public void onDeath(PlayerDeathEvent event) {
                if (instance != null) {
                    instance.despawnActiveMobs(event.getEntity().getUniqueId());
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
            sendRitualMessage(player, MessageType.SUCCESS,
                    ChatColor.GRAY + "Ritual weakened—" + ChatColor.YELLOW + (RITUAL_KILL_TARGET - progress.kills())
                            + ChatColor.GRAY + " cultists remain.");
            return;
        }

        if (questManager.hasFlag(player.getUniqueId(), ID, ritual.siteKey())) {
            return;
        }

        questManager.setFlag(player.getUniqueId(), ID, ritual.siteKey());
        questManager.handleKill(player, RITUAL_TARGET, true);
        sendRitualMessage(player, MessageType.REWARD,
                ChatColor.GRAY + "Ritual halted! The cult loses its grip here.");
        spawnRewardBomb(player, ritual.siteKey());

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
                mob = MythicBukkit.inst().getAPIHelper().spawnMythicMob(site.mobId(), spawnLoc, site.level());
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
                ritualHeader() + ChatColor.GRAY + "Dark energy gathers nearby. Cultists are preparing a ritual!"
        );
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
        despawnActiveMobs(playerId);
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

    private static RitualSite createSite(String key, String mobId, int level, String title, Location location) {
        return new RitualSite(key, mobId, level, title, resolveCombatPower(mobId, level), location);
    }

    private static int resolveCombatPower(String mobId, int level) {
        int combatPower = 0;
        try {
            combatPower = CombatPowerUtil.estimateCombatPower(mobId);
        } catch (Exception ex) {
            Main.getInstance().getLogger().warning("Failed to estimate combat power for mob '" + mobId + "': " + ex.getMessage());
        }
        if (combatPower <= 0) {
            combatPower = Math.max(level * 5, level * 3);
        }
        return combatPower;
    }

    private record RitualSite(String key, String mobId, int level, String title, int combatPower, Location location) {
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

    private static String ritualHeader() {
        return ChatColor.DARK_PURPLE + "§lRITUAL" + ChatColor.GRAY + " | ";
    }

    private void sendRitualMessage(Player player, MessageType type, String body) {
        ChatMessageUtil.send(player, type, ritualHeader() + body);
    }

    private void despawnActiveMobs(UUID playerId) {
        Map<String, SiteProgress> map = siteProgress.get(playerId);
        if (map == null) {
            return;
        }
        for (SiteProgress progress : map.values()) {
            for (UUID mobId : new java.util.ArrayList<>(progress.activeMobIds())) {
                Entity mob = Bukkit.getEntity(mobId);
                if (mob != null) {
                    mob.remove();
                }
                mobOwners.remove(mobId);
                mobSpawnLocations.remove(mobId);
            }
            progress.activeMobIds().clear();
        }
    }

    private void spawnRewardBomb(Player player, String siteKey) {
        if (player == null) {
            return;
        }
        RitualSite site = RITUAL_SITES.stream()
                .filter(s -> s.key().equals(siteKey))
                .findFirst()
                .orElse(null);
        if (site == null) {
            return;
        }

        Location origin = site.location();
        if (origin == null || origin.getWorld() == null) {
            return;
        }

        LootChestManager loot = Main.getInstance().getLootChestManager();
        PotionManager potionManager = Main.getInstance().getPotionManager();
        MercenaryAffinityManager affinity = Main.getInstance().getMercenaryAffinityManager();
        EconomyManager economyManager = Main.getInstance().getEconomyManager();
        GemsManager gemsManager = Main.getInstance().getGemsManager();

        RewardBombUtil.startRewardBomb(Main.getInstance(), origin.clone(), site.level(), lvl ->
                        rollReward(player, site, loot, potionManager, affinity, economyManager, gemsManager),
                80, player);
    }

    private ItemStack rollReward(Player player, RitualSite site, LootChestManager loot,
                                 PotionManager potionManager, MercenaryAffinityManager affinity,
                                 EconomyManager economyManager, GemsManager gemsManager) {
        double roll = ThreadLocalRandom.current().nextDouble();

        if (roll < 0.35 && loot != null) {
            ItemStack gear = loot.getRandomLootForCombatPower(site.combatPower(), site.level(), site.mobId(), null, false);
            if (gear != null) {
                ItemUtil.updateTooltip(gear, player);
            }
            return gear;
        }
        roll -= 0.35;

        if (roll < 0.15 && potionManager != null) {
            return rollPotion(potionManager, site.level());
        }
        roll -= 0.15;

        if (roll < 0.2) {
            return rollEssence();
        }
        roll -= 0.2;

        if (roll < 0.1 && affinity != null) {
            return rollFriendshipGift(affinity);
        }
        roll -= 0.1;

        if (roll < 0.1 && gemsManager != null && player != null) {
            return rollGems(gemsManager, site.level());
        }
        roll -= 0.1;

        if (economyManager != null && player != null) {
            return grantCoins(economyManager, player, site);
        }

        return null;
    }

    private ItemStack rollPotion(PotionManager potionManager, int level) {
        int tier;
        if (level < 35) {
            tier = 1;
        } else if (level < 70) {
            tier = 2;
        } else {
            tier = 3;
        }
        List<PotionTemplate> templates = potionManager.getTemplatesForTier(tier);
        if (templates == null || templates.isEmpty()) {
            return null;
        }
        PotionTemplate chosen = templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
        PotionInstance instance = potionManager.createInstance(chosen);
        return instance == null ? null : instance.toItemStack(Main.getInstance());
    }

    private ItemStack rollEssence() {
        List<PlayerClass> options = List.of(PlayerClass.ARCHER, PlayerClass.WARRIOR, PlayerClass.MAGE, PlayerClass.ROGUE);
        PlayerClass clazz = options.get(ThreadLocalRandom.current().nextInt(options.size()));
        return ClassEssence.generateEssence(clazz);
    }

    private ItemStack rollFriendshipGift(MercenaryAffinityManager affinity) {
        List<MercenaryGift> gifts = new java.util.ArrayList<>(affinity.getGifts());
        if (gifts.isEmpty()) {
            return null;
        }
        MercenaryGift gift = gifts.get(ThreadLocalRandom.current().nextInt(gifts.size()));
        return gift.getIcon();
    }

    private ItemStack rollGems(GemsManager gemsManager, int level) {
        int units = Math.max(10, level * 5);
        Material material = Material.MEDIUM_AMETHYST_BUD;
        int unitValue = 1;
        int qty = Math.max(1, units);
        if (units >= 4096) {
            material = Material.AMETHYST_CLUSTER;
            unitValue = 4096;
            qty = Math.max(1, units / unitValue);
        } else if (units >= 64) {
            material = Material.AMETHYST_SHARD;
            unitValue = 64;
            qty = Math.max(1, units / unitValue);
        }
        return gemsManager.createCurrencyItem(material, qty, unitValue);
    }

    private ItemStack grantCoins(EconomyManager economyManager, Player player, RitualSite site) {
        int coins = Math.max(10, (site.combatPower() * site.level()) / 120);
        economyManager.addCoins(player, coins);
        ItemStack pouch = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta meta = pouch.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Coin Pouch");
            List<String> lore = new java.util.ArrayList<>();
            lore.add(ChatColor.GRAY + "Currency");
            lore.addAll(TooltipUtil.bulletList(ChatColor.YELLOW + "+" + NumberUtil.formatCommas(coins) + " coins added"));
            lore.add(ChatColor.DARK_GRAY + "Already deposited to your balance.");
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            pouch.setItemMeta(meta);
        }
        return pouch;
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
